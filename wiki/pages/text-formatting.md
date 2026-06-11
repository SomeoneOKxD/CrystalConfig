---
layout: default
title: Text formatting
description: Minecraft legacy formatting codes supported by CrystalConfig labels and descriptions.
---

# Text formatting

Developer-provided display strings support legacy Minecraft formatting codes. This applies to labels, descriptions, category text, section text, button text, dropdown/list labels, tooltips, and other non-editable UI text.

Editable text inputs intentionally render and store typed formatting codes as plain text.

## Example

```java
@ConfigToggle(
        key = "enabled",
        label = "§aEnabled",
        description = "§7Turns the §bHUD §7on or off."
)
public static final MutableState<Boolean> enabled = new MutableState<>(true);
```

Manual builder:

```java
section.toggle(
        "§aEnabled",
        enabled,
        "§7Turns the §bHUD §7on or off."
);
```

## Colors

| Code | Color |
|---|---|
| `§0` | Black |
| `§1` | Dark blue |
| `§2` | Dark green |
| `§3` | Dark aqua |
| `§4` | Dark red |
| `§5` | Dark purple |
| `§6` | Gold |
| `§7` | Gray |
| `§8` | Dark gray |
| `§9` | Blue |
| `§a` | Green |
| `§b` | Aqua |
| `§c` | Red |
| `§d` | Light purple |
| `§e` | Yellow |
| `§f` | White |

## Styles

| Code | Style |
|---|---|
| `§l` | Bold |
| `§n` | Underline |
| `§m` | Strikethrough |
| `§r` | Reset to theme color and normal style |

## Search behavior

Formatting codes are ignored by config search and filtering, so searching for `enabled` still matches `§aEnabled`.

## Showing the section sign literally

Escape a formatting code with a backslash when you want to display it literally in developer-provided text:

```java
section.info("Formatting", "Use \\§a to show the green color code literally.");
```
