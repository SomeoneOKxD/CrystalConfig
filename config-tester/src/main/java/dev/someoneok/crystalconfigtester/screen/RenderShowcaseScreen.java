package dev.someoneok.crystalconfigtester.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.someoneok.crystalconfig.api.render.ButtonVariant;
import dev.someoneok.crystalconfig.api.render.TextInputEdit;
import dev.someoneok.crystalconfig.api.render.UiControlState;
import dev.someoneok.crystalconfig.api.render.UiRenderUtils;
import dev.someoneok.crystalconfig.input.KeyCodes;
import dev.someoneok.crystalconfig.render.ColorRGBA;
import dev.someoneok.crystalconfig.render.MinecraftUiRenderUtils;
import dev.someoneok.crystalconfig.render.Rect;
import dev.someoneok.crystalconfig.render.SdfRectStyle;
import dev.someoneok.crystalconfig.render.TextureFilter;
import dev.someoneok.crystalconfig.render.TextureRef;
import dev.someoneok.crystalconfig.theme.ThemePresets;
import dev.someoneok.crystalconfigtester.ConfigTesterClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * A standalone interactive screen exercising every public UiRenderUtils draw family.
 * UiRenderUtils paints only: this screen owns hitboxes, animations, text editing, and input.
 */
public final class RenderShowcaseScreen extends Screen {
    private static final ColorRGBA BLUE = ColorRGBA.hex("#4FA3FF");
    private static final ColorRGBA TEAL = ColorRGBA.hex("#4CE0C2");
    private static final ColorRGBA RED = ColorRGBA.hex("#EB6077");
    private static final ColorRGBA YELLOW = ColorRGBA.hex("#FFCC70");
    private static final ColorRGBA DIM = ColorRGBA.hex("#A0A3B5");
    private static final ColorRGBA NAVY = ColorRGBA.hex("#161923");
    private static final TextureRef ATLAS = TextureRef.of("crystalconfigtester", "textures/gui/test_atlas.png");
    private static final String[] TABS = { "Shapes", "Typography", "Textures", "Controls", "Inputs" };

    private final boolean backToConfig;
    private final MinecraftUiRenderUtils ui = MinecraftUiRenderUtils.create()
            .theme(ThemePresets.darkCrimson())
            .textShadow(false)
            .frameCaching(true);

    private int page;
    private boolean cacheEnabled = true;
    private boolean toggled = true;
    private boolean checked;
    private float progress = 0.35f;
    private float hoverAmount;
    private int heldButton = -1;
    private int focusedField = -1; // 0 = name; 1 = amount; -1 = none
    private TextInputEdit name = TextInputEdit.atEnd("CrystalConfig");
    private TextInputEdit amount = TextInputEdit.atEnd("42.5");
    private int lastMouseX = Integer.MIN_VALUE;
    private int lastMouseY = Integer.MIN_VALUE;
    private long lastFrameNs;
    //? if >=26.3 {
    /*private boolean sdlFocused;
    *///?}

    public RenderShowcaseScreen(boolean backToConfig) {
        super(Component.literal("CrystalConfig render API tester"));
        this.backToConfig = backToConfig;
    }

    private Rect panel() {
        float w = Math.min(680.0f, Math.max(200, width - 24));
        float h = Math.min(390.0f, Math.max(170, height - 20));
        return new Rect((width - w) * 0.5f, (height - h) * 0.5f, w, h);
    }

    private Rect content() {
        Rect p = panel();
        return new Rect(p.x() + 18, p.y() + 94, p.w() - 36, Math.max(0, p.h() - 144));
    }

    private Rect tab(int index) {
        Rect p = panel();
        float gap = 6;
        float w = (p.w() - 36 - (TABS.length - 1) * gap) / TABS.length;
        return new Rect(p.x() + 18 + index * (w + gap), p.y() + 51, w, 27);
    }

    private Rect backButton() {
        Rect p = panel();
        return new Rect(p.x() + 18, p.bottom() - 39, 115, 26);
    }

    private Rect cacheButton() {
        Rect p = panel();
        return new Rect(p.right() - 149, p.bottom() - 39, 131, 26);
    }

    private Rect controlButton(int index) {
        Rect c = content();
        float gap = 9;
        float w = Math.min(130, (c.w() - 20 - gap * 2) / 3);
        return new Rect(c.x() + 10 + index * (w + gap), c.y() + 42, w, 31);
    }

    private Rect toggleBounds() {
        Rect c = content();
        return new Rect(c.x() + 14, c.y() + 109, 45, 23);
    }

    private Rect checkboxBounds() {
        Rect c = content();
        return new Rect(c.x() + 204, c.y() + 109, 24, 24);
    }

    private Rect nameBounds() {
        Rect c = content();
        return new Rect(c.x() + 14, c.y() + 51, Math.min(285, c.w() - 28), 31);
    }

    private Rect amountBounds() {
        Rect c = content();
        return new Rect(c.x() + 14, c.y() + 119, Math.min(180, c.w() - 28), 31);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);

        long now = System.nanoTime();
        float deltaSeconds = lastFrameNs == 0 ? 0.016f : Math.min(0.05f, Math.max(0, (now - lastFrameNs) / 1_000_000_000f));
        lastFrameNs = now;
        if (mouseX != lastMouseX || mouseY != lastMouseY) {
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            ui.invalidate();
        }
        float target = page == 3 && controlButton(1).contains(mouseX, mouseY) ? 1 : 0;
        if (Math.abs(hoverAmount - target) > 0.002f) {
            hoverAmount += (target - hoverAmount) * Math.min(1.0f, deltaSeconds * 14);
            ui.invalidate();
        } else {
            hoverAmount = target;
        }

        ui.render(graphics, width, height, deltaSeconds, draw -> {
            Rect p = panel();
            Rect c = content();
            // UI frame, primitives, panels, text, lines, clipping and buttons.
            draw.roundedRect(p, SdfRectStyle.create()
                    .fill(NAVY).radius(13)
                    .border(1.4f, ColorRGBA.hex("#424657"))
                    .shadow(12, 0, 5, ColorRGBA.hex("#00000099")), 0);
            draw.text("CrystalConfig | Render API playground", p.x() + 20, p.y() + 17, 18, BLUE);
            draw.text("SDF + MSDF / host-owned state / Minecraft 26.x", p.x() + 21, p.y() + 36, 11, DIM);
            draw.separator(new Rect(p.x() + 18, p.y() + 85, p.w() - 36, 1));

            for (int i = 0; i < TABS.length; i++) {
                Rect tab = tab(i);
                draw.button(tab, TABS[i], UiControlState.of(tab.contains(mouseX, mouseY), false, i == page, true),
                        i == page ? ButtonVariant.ACCENT : ButtonVariant.DEFAULT);
            }

            draw.clip(c, clipped -> {
                switch (page) {
                    case 0 -> shapes(clipped, c);
                    case 1 -> typography(clipped, c);
                    case 2 -> textures(clipped, c);
                    case 3 -> controls(clipped, c, mouseX, mouseY);
                    default -> inputs(clipped, c, mouseX, mouseY);
                }
            });

            Rect back = backButton();
            Rect cache = cacheButton();
            draw.button(back, backToConfig ? "Back to config" : "Close", UiControlState.of(back.contains(mouseX, mouseY), false, false, true));
            draw.button(cache, cacheEnabled ? "Cache: enabled" : "Cache: disabled",
                    UiControlState.of(cache.contains(mouseX, mouseY), false, false, true), ButtonVariant.DEFAULT);
            draw.text("[1-5] pages", p.x() + 149, p.bottom() - 29, 11, DIM);
        });
    }

    private void shapes(UiRenderUtils d, Rect c) {
        float x = c.x() + 14, y = c.y();
        d.text("Solid / rounded / outlined / styled SDF / panel", x, y + 7, 14, ColorRGBA.WHITE);
        d.rect(new Rect(x, y + 33, 76, 35), BLUE);
        d.roundedRect(new Rect(x + 90, y + 33, 76, 35), TEAL, 9);
        d.outlinedRoundedRect(new Rect(x + 180, y + 33, 76, 35), NAVY, YELLOW, 2, 12);
        d.roundedRect(new Rect(x + 270, y + 33, 78, 35), SdfRectStyle.create()
                .fill(RED).border(2, YELLOW).radius(9).shadow(7, 2, 3, ColorRGBA.hex("#000000B0")).opacity(0.9f), 1);
        d.panel(new Rect(x + 365, y + 33, 76, 35));

        d.text("Circles, points, and batched lines", x, y + 83, 13, DIM);
        d.circle(x + 17, y + 122, 13, TEAL);
        d.point(x + 47, y + 120, 9, BLUE);
        d.line(x + 75, y + 110, x + 145, y + 134, 4, true, YELLOW);
        d.horizontalLine(x + 164, x + 258, y + 118, 3, RED);
        d.verticalLine(x + 282, y + 102, y + 141, 3, BLUE);
        d.outlineRect(new Rect(x + 311, y + 103, 48, 36), TEAL, 2);

        d.text("Horizontal gradient", x, y + 153, 12, DIM);
        d.horizontalGradient(new Rect(x, y + 171, 170, 23), BLUE, RED);
        d.text("Vertical gradient", x + 190, y + 153, 12, DIM);
        d.verticalGradient(new Rect(x + 190, y + 171, 170, 23), TEAL, YELLOW);
        d.progressBar(new Rect(x, y + 216, Math.min(360, c.w() - 28), 11), progress);
    }

    private void typography(UiRenderUtils d, Rect c) {
        float x = c.x() + 14, y = c.y();
        d.text("MSDF text / alignment / font metrics", x, y + 7, 14, ColorRGBA.WHITE);
        d.text("§bColored §lbold§r §7formatted text", x, y + 35, 17, ColorRGBA.WHITE);
        d.plainText("PlainText: §b is not interpreted", x, y + 63, 14,
                d.theme().fonts().regular(), TEAL, 0);
        d.text("Semi-bold face", x, y + 89, 15, d.theme().fonts().semibold(), YELLOW, 0);
        Rect center = new Rect(x, y + 119, Math.min(390, c.w() - 30), 32);
        d.outlineRect(center, BLUE, 1);
        d.centeredText("centeredText", center, 14, ColorRGBA.WHITE);
        d.rightAlignedText("rightAlignedText", center.right(), y + 161, 13,
                d.theme().fonts().medium(), RED, 0);
        var metrics = d.measureText("measureText", 14);
        d.text("measureText: " + Math.round(metrics.width()) + " x " + Math.round(metrics.height()), x, y + 185, 12, DIM);
        d.text("Clip scopes: long text is confined to this box", x, y + 210, 12, DIM);
        Rect clip = new Rect(x, y + 228, Math.min(260, c.w() - 30), 23);
        d.outlineRect(clip, TEAL, 1);
        d.clip(clip.inset(3), sc -> sc.text("This intentionally long text extends beyond clipping bounds", x + 6, y + 233, 12, TEAL));
    }

    private void textures(UiRenderUtils d, Rect c) {
        float x = c.x() + 14, y = c.y();
        d.text("Batched image / blit / atlas UV / filters", x, y + 7, 14, ColorRGBA.WHITE);
        d.text("Full texture", x, y + 29, 12, DIM);
        d.image(ATLAS, new Rect(x, y + 49, 80, 80));
        d.text("Tinted", x + 110, y + 29, 12, DIM);
        d.image(ATLAS, new Rect(x + 110, y + 49, 80, 80), RED);
        d.text("Blit (32px)", x + 218, y + 29, 12, DIM);
        d.blit("crystalconfigtester:textures/gui/test_atlas.png", x + 218, y + 49, 0, 0, 32, 32, 64, 64);
        d.text("Atlas region", x + 292, y + 29, 12, DIM);
        d.imageRegion(ATLAS, new Rect(x + 292, y + 49, 80, 80), 32, 0, 32, 32, 64, 64);

        d.text("Nearest / Linear filtering and normalized UVs", x, y + 149, 12, DIM);
        d.imageUv(ATLAS, new Rect(x, y + 169, 70, 70), 0, 0.5f, 0.5f, 1,
                ColorRGBA.WHITE, TextureFilter.NEAREST, 0);
        d.imageUv(ATLAS, new Rect(x + 95, y + 169, 70, 70), 0, 0.5f, 0.5f, 1,
                ColorRGBA.WHITE, TextureFilter.LINEAR, 0);
        d.texture(ATLAS, new Rect(x + 198, y + 169, 65, 65));
        d.imageRegion(ATLAS, new Rect(x + 291, y + 169, 65, 65), 32, 32, 32, 32, 64, 64,
                YELLOW, TextureFilter.LINEAR, 1);
    }

    private void controls(UiRenderUtils d, Rect c, int mx, int my) {
        float x = c.x() + 14, y = c.y();
        d.text("Stateless controls and animated button amounts", x, y + 7, 14, ColorRGBA.WHITE);
        for (int i = 0; i < 3; i++) {
            Rect b = controlButton(i);
            boolean hovered = b.contains(mx, my);
            if (i == 1) {
                d.button(b, "Increment", hoverAmount, heldButton == i ? 1.0f : 0.0f,
                        false, true, ButtonVariant.ACCENT);
            } else {
                d.button(b, i == 0 ? "Default" : "Reset", UiControlState.of(hovered, heldButton == i, false, true),
                        i == 0 ? ButtonVariant.DEFAULT : ButtonVariant.DANGER);
            }
        }
        d.toggle(toggleBounds(), toggled, UiControlState.of(toggleBounds().contains(mx, my), false, false, true));
        d.text("Toggle: " + toggled, x + 60, y + 115, 12, DIM);
        d.checkbox(checkboxBounds(), checked, UiControlState.of(checkboxBounds().contains(mx, my), false, false, true));
        d.text("Checkbox: " + checked, x + 235, y + 115, 12, DIM);
        d.text("Progress bar (Increment / Reset)", x, y + 166, 12, DIM);
        d.progressBar(new Rect(x, y + 188, Math.min(345, c.w() - 30), 15), progress);
        d.text("Animated hover is host-owned. Button press and toggle states are interactive.", x, y + 224, 11, DIM);
    }

    private void inputs(UiRenderUtils d, Rect c, int mx, int my) {
        float x = c.x() + 14, y = c.y();
        d.text("TextInputEdit + caret + selection + number validation", x, y + 7, 14, ColorRGBA.WHITE);
        d.text("Text input: type, select all (Ctrl+A), arrows, backspace", x, y + 30, 12, DIM);
        Rect nameRect = nameBounds();
        d.textInput(nameRect, name.value(), "Enter a name", name.cursor(), name.selectionAnchor(),
                UiControlState.of(nameRect.contains(mx, my), false, focusedField == 0, true), true);
        d.text("Number (0..100, decimals allowed; red border when invalid)", x, y + 98, 12, DIM);
        Rect numberRect = amountBounds();
        boolean valid = TextInputEdit.isValidNumber(amount.value(), true, 0, 100);
        d.numberInput(numberRect, amount.value(), "0 - 100", amount.cursor(), amount.selectionAnchor(),
                UiControlState.of(numberRect.contains(mx, my), false, focusedField == 1, true), valid);
        d.text("Focused field: " + (focusedField < 0 ? "none" : focusedField == 0 ? "text" : "number") +
                "   |   parsed number: " + (valid ? amount.value() : "invalid"), x, y + 168, 12, valid ? TEAL : RED);
        d.text("Press Tab to switch fields, Enter or Esc to unfocus.", x, y + 194, 12, DIM);
        Rect example = new Rect(x, y + 217, Math.min(330, c.w() - 26), 23);
        d.button(example, "Disabled button style", UiControlState.DISABLED, ButtonVariant.DEFAULT);
    }

    private void changePage(int next) {
        if (next == page) return;
        page = Math.max(0, Math.min(TABS.length - 1, next));
        heldButton = -1;
        focus(-1);
        ui.invalidate();
    }

    private void focus(int next) {
        if (focusedField == next) return;
        focusedField = next;
        // Minecraft 26.3 uses SDL's text input manager; direct charTyped() callbacks
        // are not guaranteed without enabling text input when a field has focus.
        //? if >=26.3 {
        /*boolean desired = focusedField != -1;
        if (desired != sdlFocused) {
            Minecraft.getInstance().textInputManager().onTextInputFocusChange(this, desired);
            sdlFocused = desired;
        }
        *///?}
        ui.invalidate();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!leftButton(event)) return super.mouseClicked(event, doubleClick);
        float x = (float) event.x(), y = (float) event.y();
        for (int i = 0; i < TABS.length; i++) {
            if (tab(i).contains(x, y)) { changePage(i); return true; }
        }
        if (backButton().contains(x, y)) { onClose(); return true; }
        if (cacheButton().contains(x, y)) {
            cacheEnabled = !cacheEnabled;
            ui.frameCaching(cacheEnabled);
            return true;
        }
        if (page == 3) {
            if (toggleBounds().contains(x, y)) {
                toggled = !toggled; ui.invalidate(); return true;
            }
            if (checkboxBounds().contains(x, y)) {
                checked = !checked; ui.invalidate(); return true;
            }
            for (int i = 0; i < 3; i++) {
                if (controlButton(i).contains(x, y)) {
                    heldButton = i; ui.invalidate(); return true;
                }
            }
        }
        if (page == 4) {
            if (nameBounds().contains(x, y)) { focus(0); name = name.end(false); return true; }
            if (amountBounds().contains(x, y)) { focus(1); amount = amount.end(false); return true; }
        }
        focus(-1);
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (leftButton(event) && heldButton >= 0) {
            int selected = heldButton;
            heldButton = -1;
            if (page == 3 && controlButton(selected).contains((float) event.x(), (float) event.y())) {
                if (selected == 1) progress = Math.min(1.0f, progress + 0.1f);
                if (selected == 2) { progress = 0; toggled = false; checked = false; }
                if (selected == 0) { toggled = !toggled; }
            }
            ui.invalidate();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = normalizedKey(event);
        if (focusedField >= 0) {
            if (key == KeyCodes.ESCAPE || key == KeyCodes.ENTER || key == KeyCodes.KP_ENTER) {
                focus(-1); return true;
            }
            if (key == KeyCodes.TAB) {
                focus(1 - focusedField); return true;
            }
            TextInputEdit edit = focusedField == 0 ? name : amount;
            boolean shift = shiftDown(event);
            if (ctrlDown(event) && key == KeyCodes.KEY_A) edit = edit.selectAll();
            else if (key == KeyCodes.BACKSPACE) edit = edit.backspace();
            else if (key == KeyCodes.DELETE) edit = edit.delete();
            else if (key == KeyCodes.LEFT) edit = edit.moveLeft(shift);
            else if (key == KeyCodes.RIGHT) edit = edit.moveRight(shift);
            else if (key == KeyCodes.HOME) edit = edit.home(shift);
            else if (key == KeyCodes.END) edit = edit.end(shift);
            else return super.keyPressed(event);
            applyEdit(edit);
            return true;
        }
        if (key >= KeyCodes.KEY_1 && key <= KeyCodes.KEY_5) {
            changePage(key - KeyCodes.KEY_1);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (focusedField < 0) return super.charTyped(event);
        if (!Character.isValidCodePoint(event.codepoint())) return true;
        String value = new String(Character.toChars(event.codepoint()));
        TextInputEdit edit = focusedField == 0 ? name.insert(value, 60)
                : amount.insertNumber(value, true, false, 16);
        applyEdit(edit);
        return true;
    }

    private void applyEdit(TextInputEdit edit) {
        if (focusedField == 0) name = edit;
        else if (focusedField == 1) amount = edit;
        ui.invalidate();
    }

    private static boolean leftButton(MouseButtonEvent event) {
        //? if >=26.3 {
        /*return event.buttonInfo().button() == InputConstants.MOUSE_BUTTON_LEFT;
        *///?} else {
        return event.buttonInfo().button() == 0;
        //?}
    }

    private static int normalizedKey(KeyEvent event) {
        //? if >=26.3 {
        /*return switch (event.key()) {
            case InputConstants.KEY_ESCAPE -> KeyCodes.ESCAPE;
            case InputConstants.KEY_RETURN -> KeyCodes.ENTER;
            case InputConstants.KEY_TAB -> KeyCodes.TAB;
            case InputConstants.KEY_BACKSPACE -> KeyCodes.BACKSPACE;
            case InputConstants.KEY_DELETE -> KeyCodes.DELETE;
            case InputConstants.KEY_LEFT -> KeyCodes.LEFT;
            case InputConstants.KEY_RIGHT -> KeyCodes.RIGHT;
            case InputConstants.KEY_HOME -> KeyCodes.HOME;
            case InputConstants.KEY_END -> KeyCodes.END;
            case InputConstants.KEY_1 -> KeyCodes.KEY_1;
            case InputConstants.KEY_2 -> KeyCodes.KEY_2;
            case InputConstants.KEY_3 -> KeyCodes.KEY_3;
            case InputConstants.KEY_4 -> KeyCodes.KEY_4;
            case InputConstants.KEY_5 -> KeyCodes.KEY_5;
            case InputConstants.KEY_NUMPADENTER -> KeyCodes.KP_ENTER;
            default -> (event.keycode() == 'a' || event.keycode() == 'A') ? KeyCodes.KEY_A : KeyCodes.UNKNOWN;
        };
        *///?} else {
        return event.key();
        //?}
    }

    private static boolean ctrlDown(KeyEvent event) {
        //? if >=26.3 {
        /*return (event.modifiers() & InputConstants.MOD_CONTROL) != 0;
        *///?} else {
        return (event.modifiers() & 0x0002) != 0;
        //?}
    }

    private static boolean shiftDown(KeyEvent event) {
        //? if >=26.3 {
        /*return (event.modifiers() & InputConstants.MOD_SHIFT) != 0;
        *///?} else {
        return (event.modifiers() & 0x0001) != 0;
        //?}
    }

    @Override
    public void onClose() {
        focus(-1);
        ConfigTesterClient.leaveRenderer(backToConfig);
    }

    @Override
    public void removed() {
        focus(-1);
        super.removed();
    }
}
