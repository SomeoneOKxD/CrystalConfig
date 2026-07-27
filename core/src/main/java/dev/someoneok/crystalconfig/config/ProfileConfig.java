package dev.someoneok.crystalconfig.config;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import dev.someoneok.crystalconfig.persistence.GsonConfigStore;
import dev.someoneok.crystalconfig.state.ConfigValue;
import dev.someoneok.crystalconfig.state.MutableState;
import dev.someoneok.crystalconfig.state.State;

import java.lang.reflect.Type;
import java.text.Normalizer;
import java.util.*;

public final class ProfileConfig {
    public static final int MAX_NAME_CODE_POINTS = 64;
    private static final String DEFAULT_PROFILE_ID = "profile_default";
    private static final String ID_PREFIX = "profile_";
    private static final Set<Integer> STRIPPED_FORMAT_CODE_POINTS = Set.of(
            0x061C, 0x200E, 0x200F, 0x202A, 0x202B, 0x202C, 0x202D, 0x202E,
            0x2066, 0x2067, 0x2068, 0x2069
    );
    private static final Map<State<?>, ProfileConfig> LINKED_OWNERS = Collections.synchronizedMap(new IdentityHashMap<>());

    private final MutableState<ProfileDocument> documentState;
    private final LinkedHashMap<String, LinkedSetting<?>> linkedSettings = new LinkedHashMap<>();
    private boolean applyingProfile;
    private boolean updatingDocument;
    private Gson gson = new Gson();
    private GsonConfigStore registeredStore;
    private String registeredKey;

    private ProfileConfig(String initialName) {
        String safeName = sanitizeName(initialName);
        if (safeName.isBlank()) safeName = "Default";
        ProfileData initial = new ProfileData(DEFAULT_PROFILE_ID, safeName, new LinkedHashMap<>(), new LinkedHashMap<>());
        this.documentState = new MutableState<>(new ProfileDocument(DEFAULT_PROFILE_ID, List.of(initial)));
        this.documentState.subscribe(this::onDocumentReplaced);
    }

    public static ProfileConfig create() {
        return create("Default");
    }

    public static ProfileConfig create(String initialName) {
        return new ProfileConfig(initialName);
    }

    public synchronized <T> ProfileConfig link(String key, State<T> state, Class<T> type) {
        return link(key, state, (Type) type);
    }

    public synchronized <T> ProfileConfig link(String key, State<T> state, TypeToken<T> type) {
        Objects.requireNonNull(type, "type");
        return link(key, state, type.getType());
    }

    public synchronized <T> ProfileConfig link(ConfigValue<T> value, Class<T> type) {
        Objects.requireNonNull(value, "value");
        return link(value.path(), value, type);
    }

    public synchronized <T> ProfileConfig link(ConfigValue<T> value, TypeToken<T> type) {
        Objects.requireNonNull(value, "value");
        return link(value.path(), value, type);
    }

    public synchronized <T> ProfileConfig link(String key, State<T> state, Type type) {
        String safeKey = validateSettingKey(key);
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(type, "type");
        if (linkedSettings.containsKey(safeKey)) {
            throw new IllegalArgumentException("Profile setting key is already linked: " + safeKey);
        }
        synchronized (LINKED_OWNERS) {
            ProfileConfig existing = LINKED_OWNERS.get(state);
            if (existing != null && existing != this) {
                throw new IllegalArgumentException("A State can only be linked to one ProfileConfig");
            }
            LINKED_OWNERS.put(state, this);
        }

        LinkedSetting<T> linked = new LinkedSetting<>(safeKey, state, type, copy(type, state.get()));
        linkedSettings.put(safeKey, linked);
        state.subscribe(value -> onLinkedValueChanged(linked, value));

        ProfileDocument current = documentState.get();
        List<ProfileData> updated = new ArrayList<>(current.profiles().size());
        for (ProfileData profile : current.profiles()) {
            LinkedHashMap<String, Object> values = new LinkedHashMap<>(profile.values());
            values.putIfAbsent(safeKey, copy(type, Objects.equals(profile.id(), current.selectedId()) ? state.get() : linked.defaultValue()));
            updated.add(profile.withValues(values));
        }
        replaceDocument(new ProfileDocument(current.selectedId(), updated), false);
        return this;
    }

    public synchronized List<Profile> profiles() {
        List<Profile> result = new ArrayList<>();
        for (ProfileData profile : documentState.get().profiles()) result.add(new Profile(profile.id(), profile.name()));
        return List.copyOf(result);
    }

    public synchronized String selectedId() {
        return documentState.get().selectedId();
    }

    public synchronized String selectedName() {
        ProfileData selected = selectedProfile(documentState.get());
        return selected == null ? "" : selected.name();
    }

    public synchronized String nameOf(String id) {
        ProfileData profile = find(documentState.get(), id);
        return profile == null ? "Unknown profile" : profile.name();
    }

    public synchronized boolean select(String id) {
        ProfileDocument current = captureSelectedValues(documentState.get());
        ProfileData target = find(current, id);
        if (target == null || Objects.equals(current.selectedId(), target.id())) return target != null;
        ProfileDocument changed = new ProfileDocument(target.id(), current.profiles());
        replaceDocument(changed, true);
        return true;
    }

    public synchronized Profile add(String requestedName) {
        String safeName = uniqueName(sanitizeRequiredName(requestedName), null);
        ProfileDocument current = captureSelectedValues(documentState.get());
        ProfileData selected = selectedProfile(current);
        String id = generateId(current);
        LinkedHashMap<String, Object> clonedValues = new LinkedHashMap<>();
        if (selected != null) {
            for (LinkedSetting<?> setting : linkedSettings.values()) {
                clonedValues.put(setting.key(), copyUntyped(setting, selected.values().getOrDefault(setting.key(), setting.defaultValue())));
            }
        } else {
            for (LinkedSetting<?> setting : linkedSettings.values()) {
                clonedValues.put(setting.key(), copyUntyped(setting, setting.defaultValue()));
            }
        }
        ProfileData created = new ProfileData(id, safeName, clonedValues, new LinkedHashMap<>());
        List<ProfileData> profiles = new ArrayList<>(current.profiles());
        profiles.add(created);
        replaceDocument(new ProfileDocument(id, profiles), true);
        return new Profile(id, safeName);
    }

    public synchronized boolean rename(String id, String requestedName) {
        ProfileDocument current = documentState.get();
        ProfileData target = find(current, id);
        if (target == null) return false;
        String safeName = uniqueName(sanitizeRequiredName(requestedName), id);
        List<ProfileData> updated = new ArrayList<>(current.profiles().size());
        for (ProfileData profile : current.profiles()) {
            updated.add(Objects.equals(profile.id(), id) ? profile.withName(safeName) : profile);
        }
        replaceDocument(new ProfileDocument(current.selectedId(), updated), false);
        return true;
    }

    public synchronized boolean delete(String id) {
        ProfileDocument current = captureSelectedValues(documentState.get());
        if (current.profiles().size() <= 1 || find(current, id) == null) return false;
        List<ProfileData> updated = new ArrayList<>();
        for (ProfileData profile : current.profiles()) if (!Objects.equals(profile.id(), id)) updated.add(profile);
        String selected = current.selectedId();
        boolean deletedSelected = Objects.equals(selected, id);
        if (deletedSelected) selected = updated.get(0).id();
        replaceDocument(new ProfileDocument(selected, updated), deletedSelected);
        return true;
    }

    public synchronized boolean canDelete() {
        return documentState.get().profiles().size() > 1;
    }

    public synchronized void register(GsonConfigStore store, String key) {
        Objects.requireNonNull(store, "store");
        String safeKey = Objects.requireNonNull(key, "key").trim();
        if (safeKey.isEmpty()) throw new IllegalArgumentException("Profile config key cannot be blank");
        if (registeredStore != null) {
            if (registeredStore == store && registeredKey.equals(safeKey)) return;
            throw new IllegalStateException("ProfileConfig is already registered as " + registeredKey);
        }
        registeredStore = store;
        registeredKey = safeKey;
        gson = store.gson();
        ProfileDocument normalized = normalize(documentState.get());
        replaceDocument(normalized, true);
        store.register(safeKey, documentState, ProfileDocument.class, new ProfileCodec());
    }

    public static boolean isLinked(State<?> state) {
        synchronized (LINKED_OWNERS) {
            return LINKED_OWNERS.containsKey(state);
        }
    }

    public static String sanitizeName(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFKC);
        StringBuilder cleaned = new StringBuilder();
        boolean previousWhitespace = false;
        for (int offset = 0; offset < normalized.length();) {
            int codePoint = normalized.codePointAt(offset);
            offset += Character.charCount(codePoint);
            int type = Character.getType(codePoint);
            if (Character.isWhitespace(codePoint)) {
                if (!previousWhitespace && cleaned.length() > 0) cleaned.append(' ');
                previousWhitespace = true;
            } else {
                if (Character.isISOControl(codePoint) || STRIPPED_FORMAT_CODE_POINTS.contains(codePoint)
                        || type == Character.PRIVATE_USE || type == Character.SURROGATE || type == Character.UNASSIGNED) {
                    continue;
                }
                cleaned.appendCodePoint(codePoint);
                previousWhitespace = false;
            }
            if (cleaned.codePointCount(0, cleaned.length()) >= MAX_NAME_CODE_POINTS) break;
        }
        return cleaned.toString().trim();
    }

    private synchronized void onDocumentReplaced(ProfileDocument document) {
        if (updatingDocument) return;
        ProfileDocument normalized = normalize(document);
        replaceDocument(normalized, true);
    }

    private synchronized <T> void onLinkedValueChanged(LinkedSetting<T> setting, T value) {
        if (applyingProfile) return;
        ProfileDocument current = documentState.get();
        List<ProfileData> updated = new ArrayList<>(current.profiles().size());
        for (ProfileData profile : current.profiles()) {
            if (!Objects.equals(profile.id(), current.selectedId())) {
                updated.add(profile);
                continue;
            }
            LinkedHashMap<String, Object> values = new LinkedHashMap<>(profile.values());
            values.put(setting.key(), copy(setting.type(), value));
            updated.add(profile.withValues(values));
        }
        replaceDocument(new ProfileDocument(current.selectedId(), updated), false);
    }

    private ProfileDocument captureSelectedValues(ProfileDocument document) {
        ProfileData selected = selectedProfile(document);
        if (selected == null || linkedSettings.isEmpty()) return document;
        LinkedHashMap<String, Object> values = new LinkedHashMap<>(selected.values());
        for (LinkedSetting<?> setting : linkedSettings.values()) values.put(setting.key(), copyCurrent(setting));
        List<ProfileData> updated = new ArrayList<>(document.profiles().size());
        for (ProfileData profile : document.profiles()) updated.add(Objects.equals(profile.id(), selected.id()) ? profile.withValues(values) : profile);
        return new ProfileDocument(document.selectedId(), updated);
    }

    private void replaceDocument(ProfileDocument document, boolean apply) {
        ProfileDocument safe = normalize(document);
        updatingDocument = true;
        try {
            documentState.set(safe);
        } finally {
            updatingDocument = false;
        }
        if (apply) applySelected(safe);
    }

    private void applySelected(ProfileDocument document) {
        ProfileData selected = selectedProfile(document);
        if (selected == null) return;
        applyingProfile = true;
        try {
            for (LinkedSetting<?> setting : linkedSettings.values()) applyValue(setting, selected.values().get(setting.key()));
        } finally {
            applyingProfile = false;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void applyValue(LinkedSetting<T> setting, Object raw) {
        T value = raw == null ? setting.defaultValue() : (T) raw;
        setting.state().set(copy(setting.type(), value));
    }

    private ProfileDocument normalize(ProfileDocument raw) {
        ProfileDocument source = raw == null ? new ProfileDocument("", List.of()) : raw;
        List<ProfileData> normalized = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (ProfileData candidate : source.profiles() == null ? List.<ProfileData>of() : source.profiles()) {
            if (candidate == null) continue;
            String id = validId(candidate.id()) && ids.add(candidate.id()) ? candidate.id() : generateId(ids);
            ids.add(id);
            String name = sanitizeName(candidate.name());
            if (name.isBlank()) name = "Profile " + (normalized.size() + 1);
            name = uniqueNameWithin(name, id, normalized);
            LinkedHashMap<String, Object> values = new LinkedHashMap<>();
            Map<String, Object> sourceValues = candidate.values() == null ? Map.of() : candidate.values();
            for (LinkedSetting<?> setting : linkedSettings.values()) {
                Object value = sourceValues.containsKey(setting.key()) ? sourceValues.get(setting.key()) : setting.defaultValue();
                values.put(setting.key(), copyUntyped(setting, value));
            }
            LinkedHashMap<String, JsonElement> unknown = new LinkedHashMap<>();
            if (candidate.unknownValues() != null) {
                candidate.unknownValues().forEach((key, value) -> {
                    if (!linkedSettings.containsKey(key) && value != null) unknown.put(key, value.deepCopy());
                });
            }
            normalized.add(new ProfileData(id, name, values, unknown));
        }
        if (normalized.isEmpty()) {
            LinkedHashMap<String, Object> values = new LinkedHashMap<>();
            for (LinkedSetting<?> setting : linkedSettings.values()) values.put(setting.key(), copyUntyped(setting, setting.defaultValue()));
            normalized.add(new ProfileData(DEFAULT_PROFILE_ID, "Default", values, new LinkedHashMap<>()));
        }
        String selected = find(new ProfileDocument(source.selectedId(), normalized), source.selectedId()) == null
                ? normalized.get(0).id() : source.selectedId();
        return new ProfileDocument(selected, List.copyOf(normalized));
    }

    private String uniqueName(String requested, String exceptId) {
        String base = requested;
        String candidate = base;
        int suffix = 2;
        while (nameExists(candidate, exceptId)) candidate = trimForSuffix(base, suffix) + " (" + suffix++ + ")";
        return candidate;
    }

    private boolean nameExists(String name, String exceptId) {
        for (ProfileData profile : documentState.get().profiles()) {
            if (!Objects.equals(profile.id(), exceptId) && profile.name().equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    private static String uniqueNameWithin(String requested, String id, List<ProfileData> existing) {
        String base = requested;
        String candidate = base;
        int suffix = 2;
        while (containsName(existing, candidate, id)) candidate = trimForSuffix(base, suffix) + " (" + suffix++ + ")";
        return candidate;
    }

    private static boolean containsName(List<ProfileData> profiles, String name, String exceptId) {
        for (ProfileData profile : profiles) {
            if (!Objects.equals(profile.id(), exceptId) && profile.name().equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    private static String trimForSuffix(String value, int suffix) {
        String addition = " (" + suffix + ")";
        int max = Math.max(1, MAX_NAME_CODE_POINTS - addition.codePointCount(0, addition.length()));
        if (value.codePointCount(0, value.length()) <= max) return value;
        int end = value.offsetByCodePoints(0, max);
        return value.substring(0, end).trim();
    }

    private static String sanitizeRequiredName(String input) {
        String safe = sanitizeName(input);
        if (safe.isBlank()) throw new IllegalArgumentException("Profile name cannot be blank");
        return safe;
    }

    private static String validateSettingKey(String key) {
        String safe = key == null ? "" : key.trim();
        if (!safe.matches("[A-Za-z0-9_.-]{1,128}")) {
            throw new IllegalArgumentException("Profile setting keys may only contain letters, numbers, '.', '_' and '-'");
        }
        return safe;
    }

    private static boolean validId(String id) {
        return id != null && id.matches("profile_[a-z0-9]{7,64}");
    }

    private static String generateId(ProfileDocument document) {
        Set<String> existing = new HashSet<>();
        for (ProfileData profile : document.profiles()) existing.add(profile.id());
        return generateId(existing);
    }

    private static String generateId(Set<String> existing) {
        String id;
        do {
            id = ID_PREFIX + UUID.randomUUID().toString().replace("-", "");
        } while (existing.contains(id));
        return id;
    }

    private ProfileData selectedProfile(ProfileDocument document) {
        return find(document, document.selectedId());
    }

    private static ProfileData find(ProfileDocument document, String id) {
        if (document == null || id == null) return null;
        for (ProfileData profile : document.profiles()) if (Objects.equals(profile.id(), id)) return profile;
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T copy(Type type, T value) {
        if (value == null) return null;
        try {
            return (T) gson.fromJson(gson.toJsonTree(value, type), type);
        } catch (RuntimeException ignored) {
            return value;
        }
    }

    private Object copyCurrent(LinkedSetting<?> setting) {
        return copyUntyped(setting, setting.state().get());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object copyUntyped(LinkedSetting setting, Object value) {
        return copy(setting.type(), value);
    }

    public record Profile(String id, String name) {}

    private record LinkedSetting<T>(String key, State<T> state, Type type, T defaultValue) {}

    private record ProfileData(String id, String name, Map<String, Object> values, Map<String, JsonElement> unknownValues) {
        private ProfileData withName(String newName) { return new ProfileData(id, newName, values, unknownValues); }
        private ProfileData withValues(Map<String, Object> newValues) { return new ProfileData(id, name, newValues, unknownValues); }
    }

    private record ProfileDocument(String selectedId, List<ProfileData> profiles) {}

    private final class ProfileCodec implements GsonConfigStore.JsonCodec<ProfileDocument> {
        @Override
        public JsonElement write(Gson gson, ProfileDocument raw, Type valueType) {
            ProfileDocument document = captureSelectedValues(normalize(raw));
            JsonObject root = new JsonObject();
            root.addProperty("selected", document.selectedId());
            JsonArray profiles = new JsonArray();
            for (ProfileData profile : document.profiles()) {
                JsonObject entry = new JsonObject();
                entry.addProperty("id", profile.id());
                entry.addProperty("name", profile.name());
                JsonObject settings = new JsonObject();
                if (profile.unknownValues() != null) {
                    profile.unknownValues().forEach((key, value) ->
                            settings.add(key, value == null ? JsonNull.INSTANCE : value.deepCopy()));
                }
                for (LinkedSetting<?> linked : linkedSettings.values()) {
                    Object value = profile.values().getOrDefault(linked.key(), linked.defaultValue());
                    settings.add(linked.key(), gson.toJsonTree(value, linked.type()));
                }
                entry.add("settings", settings);
                profiles.add(entry);
            }
            root.add("profiles", profiles);
            return root;
        }

        @Override
        public ProfileDocument read(Gson gson, JsonElement json, Type valueType) {
            if (json == null || !json.isJsonObject()) return normalize(null);
            JsonObject root = json.getAsJsonObject();
            String selected = string(root, "selected");
            List<ProfileData> profiles = new ArrayList<>();
            JsonElement listElement = root.get("profiles");
            if (listElement != null && listElement.isJsonArray()) {
                for (JsonElement item : listElement.getAsJsonArray()) {
                    if (!item.isJsonObject()) continue;
                    JsonObject entry = item.getAsJsonObject();
                    String id = string(entry, "id");
                    String name = string(entry, "name");
                    JsonObject settings = entry.has("settings") && entry.get("settings").isJsonObject()
                            ? entry.getAsJsonObject("settings") : new JsonObject();
                    LinkedHashMap<String, Object> values = new LinkedHashMap<>();
                    LinkedHashMap<String, JsonElement> unknown = new LinkedHashMap<>();
                    for (Map.Entry<String, JsonElement> value : settings.entrySet()) {
                        LinkedSetting<?> linked = linkedSettings.get(value.getKey());
                        if (linked == null) {
                            unknown.put(value.getKey(), value.getValue().deepCopy());
                            continue;
                        }
                        try {
                            values.put(linked.key(), gson.fromJson(value.getValue(), linked.type()));
                        } catch (RuntimeException ignored) {
                            values.put(linked.key(), copyUntyped(linked, linked.defaultValue()));
                        }
                    }
                    profiles.add(new ProfileData(id, name, values, unknown));
                }
            }
            return normalize(new ProfileDocument(selected, profiles));
        }

        private String string(JsonObject object, String key) {
            JsonElement element = object.get(key);
            if (element == null || !element.isJsonPrimitive()) return "";
            try {
                return element.getAsString();
            } catch (RuntimeException ignored) {
                return "";
            }
        }
    }
}
