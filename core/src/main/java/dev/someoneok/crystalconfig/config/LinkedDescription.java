package dev.someoneok.crystalconfig.config;

import dev.someoneok.crystalconfig.input.MouseButton;
import dev.someoneok.crystalconfig.input.MouseButtonEvent;
import dev.someoneok.crystalconfig.layout.LayoutContext;
import dev.someoneok.crystalconfig.render.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LinkedDescription {
    private static final Pattern LINK_PATTERN = Pattern.compile(
            "\\[([^\\]]+)]\\(config:([^)]+)\\)|\\[\\[([^\\]]+)]]"
    );
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\s+|\\S+");

    private Function<String, String> labelResolver = key -> null;
    private Consumer<String> navigator = key -> { };
    private final List<LinkHitbox> hitboxes = new ArrayList<>();
    private String pressedKey;

    void configure(Function<String, String> labelResolver, Consumer<String> navigator) {
        this.labelResolver = labelResolver == null ? key -> null : labelResolver;
        this.navigator = navigator == null ? key -> { } : navigator;
    }

    int lineCount(LayoutContext context, String text, float fontSize, float maxWidth) {
        return layout(text, maxWidth, value -> MinecraftTextFormatting.measureText(
                context.backend(),
                context.theme(),
                value,
                fontSize,
                context.theme().fonts().regular()
        ).width()).size();
    }

    void render(
            RenderContext context,
            String text,
            float x,
            float y,
            float fontSize,
            String fontFace,
            ColorRGBA textColor,
            ColorRGBA linkColor,
            float lineHeight,
            float maxWidth,
            float z
    ) {
        hitboxes.clear();
        List<Line> lines = layout(text, maxWidth, value -> context.measureText(value, fontSize, fontFace).width());
        float cursorY = y;
        for (Line line : lines) {
            float cursorX = x;
            for (Run run : line.runs()) {
                if (run.text().isEmpty()) continue;
                ColorRGBA color = run.optionKey() == null ? textColor : linkColor;
                context.text(run.text(), cursorX, cursorY, fontSize, fontFace, color, z);
                TextMetrics metrics = context.measureText(run.text(), fontSize, fontFace);
                if (run.optionKey() != null) {
                    Rect hitbox = new Rect(cursorX, cursorY, metrics.width(), Math.max(lineHeight, metrics.height()));
                    hitboxes.add(new LinkHitbox(hitbox, run.optionKey()));
                    context.rect(
                            new Rect(cursorX, cursorY + Math.max(1, lineHeight - 1.5f), metrics.width(), 1),
                            linkColor.withAlpha(Math.min(220, linkColor.a())),
                            0,
                            z + 0.01f
                    );
                }
                cursorX += metrics.width();
            }
            cursorY += lineHeight;
        }
    }

    boolean mousePressed(MouseButtonEvent event) {
        if (event.button != MouseButton.LEFT) return false;
        String key = keyAt(event.x, event.y);
        if (key == null) return false;
        pressedKey = key;
        return true;
    }

    boolean containsLink(float x, float y) {
        return keyAt(x, y) != null;
    }

    boolean mouseReleased(MouseButtonEvent event) {
        if (pressedKey == null) return false;
        String pressed = pressedKey;
        pressedKey = null;
        if (event.button == MouseButton.LEFT && Objects.equals(pressed, keyAt(event.x, event.y))) {
            navigator.accept(pressed);
        }
        return true;
    }

    private String keyAt(float x, float y) {
        for (LinkHitbox hitbox : hitboxes) {
            if (hitbox.bounds().contains(x, y)) return hitbox.optionKey();
        }
        return null;
    }

    private List<Line> layout(String text, float maxWidth, Function<String, Float> measure) {
        List<Line> lines = new ArrayList<>();
        if (text == null || text.isBlank()) return lines;
        if (maxWidth <= 8) {
            lines.add(new Line(parseRuns(text.replace('\n', ' '))));
            return lines;
        }

        String[] paragraphs = text.split("\\n", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.isBlank()) {
                lines.add(new Line(new ArrayList<>()));
                continue;
            }

            List<Run> current = new ArrayList<>();
            float currentWidth = 0;
            for (Run parsed : parseRuns(paragraph)) {
                Matcher tokenMatcher = TOKEN_PATTERN.matcher(parsed.text());
                while (tokenMatcher.find()) {
                    String token = tokenMatcher.group();
                    boolean whitespace = Character.isWhitespace(token.charAt(0));
                    if (whitespace && current.isEmpty()) continue;

                    float tokenWidth = measure.apply(token);
                    if (!whitespace && !current.isEmpty() && currentWidth + tokenWidth > maxWidth) {
                        trimTrailingWhitespace(current);
                        lines.add(new Line(current));
                        current = new ArrayList<>();
                        currentWidth = 0;
                    }

                    appendRun(current, token, parsed.optionKey());
                    currentWidth += tokenWidth;
                }
            }
            trimTrailingWhitespace(current);
            lines.add(new Line(current));
        }
        return lines;
    }

    private List<Run> parseRuns(String text) {
        List<Run> runs = new ArrayList<>();
        Matcher matcher = LINK_PATTERN.matcher(text == null ? "" : text);
        int cursor = 0;
        while (matcher.find()) {
            if (matcher.start() > cursor) {
                appendRun(runs, text.substring(cursor, matcher.start()), null);
            }

            String optionKey;
            String label;
            if (matcher.group(3) != null) {
                optionKey = matcher.group(3).trim();
                String resolved = labelResolver.apply(optionKey);
                label = resolved == null || resolved.isBlank() ? optionKey : resolved;
            } else {
                label = matcher.group(1);
                optionKey = matcher.group(2).trim();
            }

            if (optionKey.isBlank()) appendRun(runs, matcher.group(), null);
            else appendRun(runs, label, optionKey);
            cursor = matcher.end();
        }
        if (cursor < text.length()) appendRun(runs, text.substring(cursor), null);
        return runs;
    }

    private static void appendRun(List<Run> runs, String text, String optionKey) {
        if (text == null || text.isEmpty()) return;
        if (!runs.isEmpty()) {
            Run previous = runs.get(runs.size() - 1);
            if (Objects.equals(previous.optionKey(), optionKey)) {
                runs.set(runs.size() - 1, new Run(previous.text() + text, optionKey));
                return;
            }
        }
        runs.add(new Run(text, optionKey));
    }

    private static void trimTrailingWhitespace(List<Run> runs) {
        while (!runs.isEmpty()) {
            int lastIndex = runs.size() - 1;
            Run last = runs.get(lastIndex);
            String trimmed = last.text().stripTrailing();
            if (trimmed.isEmpty()) {
                runs.remove(lastIndex);
                continue;
            }
            if (!trimmed.equals(last.text())) runs.set(lastIndex, new Run(trimmed, last.optionKey()));
            return;
        }
    }

    private record Run(String text, String optionKey) { }
    private record Line(List<Run> runs) { }
    private record LinkHitbox(Rect bounds, String optionKey) { }
}
