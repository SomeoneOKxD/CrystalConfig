package dev.someoneok.crystalconfigtester;

import dev.someoneok.crystalconfig.autoconfig.AutoConfig;
import dev.someoneok.crystalconfig.autoconfig.MinecraftAutoConfig;
import dev.someoneok.crystalconfig.config.ConfigScreenBuilder;
import dev.someoneok.crystalconfig.persistence.GsonConfigStore;
import dev.someoneok.crystalconfig.render.ConfigScreen;
import dev.someoneok.crystalconfig.ui.UiRoot;
import dev.someoneok.crystalconfig.theme.ThemePresets;
import dev.someoneok.crystalconfigtester.config.*;
import dev.someoneok.crystalconfigtester.screen.RenderShowcaseScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Path;

/** Standalone client-only test mod, intentionally not part of the CrystalConfig library jar. */
public final class ConfigTesterClient implements ClientModInitializer {
    private static AutoConfig.Model model;
    private static GsonConfigStore store;
    private static boolean initialized;

    @Override
    public void onInitializeClient() {
        MinecraftAutoConfig.register();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("crystaltest")
                        .executes(context -> {
                            if (!initializeOrReport(context.getSource()::sendError)) return 0;
                            Minecraft.getInstance().execute(ConfigTesterClient::openConfigScreen);
                            return 1;
                        })
                        .then(ClientCommands.literal("config").executes(context -> {
                            if (!initializeOrReport(context.getSource()::sendError)) return 0;
                            Minecraft.getInstance().execute(ConfigTesterClient::openConfigScreen);
                            return 1;
                        }))
                        .then(ClientCommands.literal("render").executes(context -> {
                            Minecraft.getInstance().execute(() -> openRendererScreen(false));
                            return 1;
                        }))
                        .then(ClientCommands.literal("manual").executes(context -> {
                            if (!initializeOrReport(context.getSource()::sendError)) return 0;
                            Minecraft.getInstance().execute(ConfigTesterClient::openManualScreen);
                            return 1;
                        }))
                )
        );
    }

    @FunctionalInterface
    private interface ErrorReporter { void report(Component error); }

    private static boolean initializeOrReport(ErrorReporter reporter) {
        if (initialized) return true;
        try {
            Path path = FabricLoader.getInstance().getConfigDir().resolve("crystalconfig-tester.json");
            model = AutoConfig.of(
                    BasicOptions.class,
                    SelectionOptions.class,
                    CustomOptions.class,
                    ProfileOptions.class,
                    ConditionalOptions.class,
                    ConditionalCategoryOptions.class,
                    SoundOptions.class
            ).configureSettings(settings -> settings
                    .defaultTheme(ThemePresets.darkCrimson())
                    .defaultScale(1.0d)
                    .defaultTextShadow(false));
            store = GsonConfigStore.builder(path).build();
            model.register(store);
            store.loadBlocking();
            initialized = true;
            return true;
        } catch (IOException | RuntimeException exception) {
            reporter.report(Component.literal("CrystalConfig tester: " + exception.getMessage()));
            return false;
        }
    }

    private static void openConfigScreen() {
        if (!initialized) return;
        UiRoot root = model.root("CrystalConfig - every option");
        root.onClose(store::saveNow);
        setScreen(new ConfigScreen(root, "CrystalConfig tester"));
    }

    private static void openManualScreen() {
        if (!initialized) return;
        UiRoot root = ConfigScreenBuilder.create("Manual builder showcase", model.settings())
                .section("Manual", "Normal widgets", section -> section
                        .info("Manual builder API", "Controls use the same persistent states as the annotation screen.")
                        .toggle("Toggle", BasicOptions.enabled, "Edit from here or AutoConfig")
                        .checkbox("Checkbox", BasicOptions.checked, "Two-way state sharing")
                        .slider("Slider", BasicOptions.slider, 0, 100, 5, "Range 0..100")
                        .sliderLabel("Slider label", BasicOptions.labeledSlider, 0.5, 3, 0.1, "Value label")
                        .number("Number", BasicOptions.number, -50, 50, 0.5, "Numeric input")
                        .text("Text", BasicOptions.text, "Shared persisted string")
                        .color("Color", BasicOptions.alphaColor, true, "RGBA selection")
                        .keybind("Keybind", BasicOptions.keybind, "Keyboard or mouse"))
                .section("Manual", "Lists and actions", section -> section
                        .dropdown("Dropdown", SelectionOptions.dropdown,
                                java.util.List.of(TestChoices.Mode.values()), Object::toString, "Enum choice")
                        .groupedDropdown("Grouped", SelectionOptions.grouped, SelectionOptions.GROUPS, "Grouped enum choice")
                        .separator()
                        .labeledSeparator("More")
                        .button("Increment counter", "+1", () -> CustomOptions.clicks.set(CustomOptions.clicks.get() + 1), "Test callback")
                        .info("Button counter", "Currently " + CustomOptions.clicks.get() + " clicks"))
                .footerButton("Renderer showcase", ConfigTesterClient::openRenderer)
                .onClose(store::saveNow)
                .buildRoot();
        setScreen(new ConfigScreen(root, "Manual builder tester"));
    }

    /** Called by @ConfigFooterButton and the manual screen footer. */
    public static void openRenderer() {
        Minecraft.getInstance().execute(() -> openRendererScreen(true));
    }

    private static void openRendererScreen(boolean backToConfig) {
        setScreen(new RenderShowcaseScreen(backToConfig));
    }

    public static void leaveRenderer(boolean backToConfig) {
        if (backToConfig && initialized) openConfigScreen();
        else setScreen(null);
    }

    /** Minecraft 26.2 moved setScreen() to the GUI object. */
    private static void setScreen(Screen next) {
        Minecraft mc = Minecraft.getInstance();
        //? if >=26.2 {
        /*mc.gui.setScreen(next);
        *///?} else {
        mc.setScreen(next);
        //?}
    }
}
