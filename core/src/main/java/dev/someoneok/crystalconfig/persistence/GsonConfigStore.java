package dev.someoneok.crystalconfig.persistence;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import dev.someoneok.crystalconfig.state.State;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GsonConfigStore implements AutoCloseable {
    private final Path path;
    private final Gson gson;
    private final long debounceMillis;
    private final ScheduledExecutorService executor;
    private final Map<String, Entry<?>> entries = new LinkedHashMap<>();
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private final String versionKey;
    private final int currentVersion;
    private final NavigableMap<Integer, ConfigMigration> migrations;
    private final List<Runnable> beforeSaveCallbacks;
    private final List<Runnable> afterLoadCallbacks;
    private ScheduledFuture<?> scheduledSave;
    private JsonObject migratedRootForNextSave;
    private volatile Throwable lastError;

    private GsonConfigStore(
            Path path,
            Gson gson,
            long debounceMillis,
            String versionKey,
            int currentVersion,
            NavigableMap<Integer, ConfigMigration> migrations,
            List<Runnable> beforeSaveCallbacks,
            List<Runnable> afterLoadCallbacks
    ) {
        this.path = Objects.requireNonNull(path, "path");
        this.gson = Objects.requireNonNull(gson, "gson");
        this.debounceMillis = Math.max(0, debounceMillis);
        this.versionKey = validateVersionKey(versionKey);
        this.currentVersion = validateVersion(currentVersion);
        this.migrations = new TreeMap<>(Objects.requireNonNull(migrations, "migrations"));
        this.beforeSaveCallbacks = List.copyOf(beforeSaveCallbacks);
        this.afterLoadCallbacks = List.copyOf(afterLoadCallbacks);
        this.executor = Executors.newSingleThreadScheduledExecutor(new ConfigThreadFactory());
    }

    public static Builder builder(Path path) { return new Builder(path); }

    public synchronized <T> GsonConfigStore register(String key, State<T> state, Class<T> type) { return register(key, state, (Type) type); }
    public synchronized <T> GsonConfigStore register(String key, State<T> state, TypeToken<T> type) { return register(key, state, type.getType()); }

    public synchronized <T> GsonConfigStore register(String key, State<T> state, Type type) {
        validateKey(key);
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(type, "type");
        entries.put(key, new Entry<>(key, state, type, gson.toJsonTree(state.get(), type)));
        state.subscribe(ignored -> requestSave());
        return this;
    }

    /** Restores every registered state to the value it had at registration time. */
    public synchronized void resetRegisteredDefaults() {
        for (Entry<?> entry : entries.values()) entry.reset(gson);
        requestSave();
    }

    /** Non-blocking load on the config IO thread. Check lastError() if you want to surface failures. */
    public void load() { executor.execute(this::loadNow); }

    /** Blocking load for early init before building the UI. */
    public void loadBlocking() throws IOException {
        readIntoStates();
        runCallbacks(afterLoadCallbacks);
        saveIfDirty();
    }

    public void requestSave() {
        dirty.set(true);
        synchronized (this) {
            if (scheduledSave != null) scheduledSave.cancel(false);
            scheduledSave = executor.schedule(this::saveIfDirty, debounceMillis, TimeUnit.MILLISECONDS);
        }
    }

    public void saveNow() {
        dirty.set(true);
        executor.execute(this::saveIfDirty);
    }

    public Throwable lastError() { return lastError; }

    private void loadNow() {
        try {
            readIntoStates();
            runCallbacks(afterLoadCallbacks);
            saveIfDirty();
            lastError = null;
        }
        catch (Throwable t) { lastError = t; }
    }

    private synchronized void readIntoStates() throws IOException {
        if (!Files.exists(path)) return;
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed == null || !parsed.isJsonObject()) return;
            root = parsed.getAsJsonObject();
        }

        int fileVersion = readVersion(root);
        if (fileVersion > currentVersion) {
            throw new IOException("Config file version " + fileVersion + " is newer than supported version " + currentVersion);
        }
        if (fileVersion < currentVersion) {
            backupConfigBeforeMigration(fileVersion, currentVersion);
            migrate(root, fileVersion, currentVersion);
            migratedRootForNextSave = root.deepCopy();
            dirty.set(true);
        }

        for (Entry<?> entry : entries.values()) {
            JsonElement element = find(root, entry.key);
            if (element != null && !element.isJsonNull()) entry.read(gson, element);
        }
        if (fileVersion >= currentVersion) dirty.set(false);
    }

    private int readVersion(JsonObject root) {
        JsonElement element = root.get(versionKey);
        if (element == null || element.isJsonNull()) return 0;
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) return 0;
        try {
            return Math.max(0, element.getAsInt());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void backupConfigBeforeMigration(int fromVersion, int toVersion) throws IOException {
        if (!Files.exists(path) || !Files.isRegularFile(path)) return;

        Path absolutePath = path.toAbsolutePath();
        Path parent = absolutePath.getParent();
        Path backupDir = parent != null ? parent.resolve("backup") : Path.of("backup");
        Files.createDirectories(backupDir);

        String fileName = absolutePath.getFileName().toString();
        String baseName = fileName + ".v" + fromVersion + "-to-v" + toVersion + "." + System.currentTimeMillis();
        Path backupPath = backupDir.resolve(baseName + ".bak");

        for (int attempt = 1; Files.exists(backupPath); attempt++) {
            backupPath = backupDir.resolve(baseName + "." + attempt + ".bak");
        }

        Files.copy(path, backupPath, StandardCopyOption.COPY_ATTRIBUTES);
    }

    private void migrate(JsonObject root, int fromVersion, int toVersion) throws IOException {
        for (int version = fromVersion; version < toVersion; version++) {
            ConfigMigration migration = migrations.get(version);
            if (migration != null) {
                try {
                    migration.migrate(root);
                } catch (RuntimeException e) {
                    throw new IOException("Failed to migrate config from version " + version + " to " + (version + 1), e);
                }
            }
        }
        root.addProperty(versionKey, toVersion);
    }

    private void saveIfDirty() {
        if (!dirty.getAndSet(false)) return;
        try { writeSnapshot(); lastError = null; }
        catch (Throwable t) { lastError = t; dirty.set(true); }
    }

    private synchronized void writeSnapshot() throws IOException {
        runCallbacks(beforeSaveCallbacks);

        JsonObject root = consumeMigratedRootForNextSave();
        if (root == null) {
            root = new JsonObject();
            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    JsonElement existing = JsonParser.parseReader(reader);
                    if (existing != null && existing.isJsonObject()) root = existing.getAsJsonObject();
                } catch (RuntimeException ignored) {
                    root = new JsonObject();
                }
            }
        }
        for (Entry<?> entry : entries.values()) entry.write(gson, root);
        root.addProperty(versionKey, currentVersion);
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(tmp)) { gson.toJson(root, writer); }
        try { Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (IOException atomicMoveUnsupported) { Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING); }
    }

    private JsonObject consumeMigratedRootForNextSave() {
        JsonObject root = migratedRootForNextSave;
        migratedRootForNextSave = null;
        return root;
    }

    @Override
    public void close() {
        saveNow();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(Math.max(250, debounceMillis + 250), TimeUnit.MILLISECONDS)) executor.shutdownNow();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    private static void runCallbacks(List<Runnable> callbacks) {
        for (Runnable callback : callbacks) {
            if (callback != null) callback.run();
        }
    }

    private static void validateKey(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Config key cannot be blank");
    }

    private static String validateVersionKey(String key) {
        validateKey(key);
        return key;
    }

    private static int validateVersion(int version) {
        if (version < 1) throw new IllegalArgumentException("Config version must be at least 1");
        return version;
    }

    private static boolean treeKey(String key) {
        return key != null && key.indexOf('.') >= 0 && !key.startsWith("__");
    }

    private static JsonElement find(JsonObject root, String key) {
        if (!treeKey(key)) return root.get(key);
        JsonElement current = root;
        for (String part : key.split("\\.")) {
            if (part.isBlank() || current == null || !current.isJsonObject()) return null;
            current = current.getAsJsonObject().get(part);
        }
        return current;
    }

    private static void add(JsonObject root, String key, JsonElement value) {
        if (!treeKey(key)) {
            root.add(key, value);
            return;
        }
        String[] parts = key.split("\\.");
        JsonObject current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            if (part.isBlank()) continue;
            JsonElement next = current.get(part);
            if (next == null || !next.isJsonObject()) {
                JsonObject created = new JsonObject();
                current.add(part, created);
                current = created;
            } else {
                current = next.getAsJsonObject();
            }
        }
        String leaf = parts[parts.length - 1];
        if (!leaf.isBlank()) current.add(leaf, value);
    }

    @FunctionalInterface
    public interface ConfigMigration {
        /** Mutates the raw JSON root in-place while migrating from version N to N + 1. */
        void migrate(JsonObject root);
    }

    private record Entry<T>(String key, State<T> state, Type type, JsonElement defaultValue) {
        void read(Gson gson, JsonElement element) { state.set(gson.fromJson(element, type)); }
        void write(Gson gson, JsonObject root) { add(root, key, gson.toJsonTree(state.get(), type)); }
        void reset(Gson gson) { state.set(gson.fromJson(defaultValue.deepCopy(), type)); }
    }

    private static final class ConfigThreadFactory implements ThreadFactory {
        @Override public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "crystal-config-save");
            thread.setDaemon(true);
            return thread;
        }
    }

    public static final class Builder {
        private final Path path;
        private final GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting();
        private final List<Runnable> beforeSaveCallbacks = new ArrayList<>();
        private final List<Runnable> afterLoadCallbacks = new ArrayList<>();
        private final NavigableMap<Integer, ConfigMigration> migrations = new TreeMap<>();
        private Gson customGson;
        private long debounceMillis = Duration.ofMillis(350).toMillis();
        private String versionKey = "__configVersion";
        private int currentVersion = 1;

        private Builder(Path path) { this.path = path; }
        public Builder gson(Gson gson) { this.customGson = Objects.requireNonNull(gson, "gson"); return this; }
        public Builder debounceMillis(long debounceMillis) { this.debounceMillis = Math.max(0, debounceMillis); return this; }

        /** Sets the JSON key used to store the schema version. Defaults to {@code __configVersion}. */
        public Builder versionKey(String versionKey) { this.versionKey = validateVersionKey(versionKey); return this; }

        /** Sets the current config schema version written to disk. Defaults to {@code 1}. */
        public Builder configVersion(int version) { this.currentVersion = validateVersion(version); return this; }

        /**
         * Registers a migration from {@code fromVersion} to {@code fromVersion + 1}.
         * Missing migrations are allowed for versions that do not need JSON rewrites.
         */
        public Builder migration(int fromVersion, ConfigMigration migration) {
            if (fromVersion < 0) throw new IllegalArgumentException("Migration source version cannot be negative");
            if (migration != null) migrations.put(fromVersion, migration);
            return this;
        }

        public Builder beforeSave(Runnable callback) { if (callback != null) beforeSaveCallbacks.add(callback); return this; }
        public Builder afterLoad(Runnable callback) { if (callback != null) afterLoadCallbacks.add(callback); return this; }
        public <T> Builder typeAdapter(Class<T> type, TypeAdapter<T> adapter) { gsonBuilder.registerTypeAdapter(type, adapter); return this; }
        public Builder typeAdapter(Type type, Object adapter) { gsonBuilder.registerTypeAdapter(type, adapter); return this; }
        public Builder disableHtmlEscaping() { gsonBuilder.disableHtmlEscaping(); return this; }
        public GsonConfigStore build() {
            return new GsonConfigStore(
                    path,
                    customGson != null ? customGson : gsonBuilder.create(),
                    debounceMillis,
                    versionKey,
                    currentVersion,
                    migrations,
                    beforeSaveCallbacks,
                    afterLoadCallbacks
            );
        }
    }
}
