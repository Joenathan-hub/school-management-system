#!/usr/bin/env python3
"""
Generates the browser-rendered design preview from the application's own
stylesheets.

The JavaFX sheets in src/main/resources/css are the source of truth for the
design; this script does not restate the design, it *translates* it, so what the
preview shows cannot drift away from what the application ships:

  * the two token sheets become [data-theme="light"] / [data-theme="dark"] blocks
    of CSS custom properties, with token-to-token references turned into var()
    references. Switching the attribute is therefore the browser's equivalent of
    the application swapping one token stylesheet on the Scene.
  * structure.css and components.css have their -fx-* declarations mapped onto
    the equivalent standard CSS properties. Anything JavaFX-specific that has no
    meaning in a browser (background insets, cell sizes, table-view internals) is
    dropped, and the script prints what it dropped so the mapping stays honest.

Run from the repository root:

    python3 tools/design-preview/build_preview.py

Writes tools/design-preview/preview.css.
"""

import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
CSS_DIR = os.path.join(ROOT, "src", "main", "resources", "css")
OUT = os.path.join(ROOT, "tools", "design-preview", "preview.css")

# JavaFX property -> standard CSS property. Anything not listed here is reported.
PROPERTY_MAP = {
    "-fx-background-color": "background-color",
    "-fx-background-radius": "border-radius",
    "-fx-border-radius": "border-radius",
    "-fx-border-color": "border-color",
    "-fx-border-width": "border-width",
    "-fx-text-fill": "color",
    "-fx-font-family": "font-family",
    "-fx-font-size": "font-size",
    "-fx-font-weight": "font-weight",
    "-fx-font-style": "font-style",
    "-fx-padding": "padding",
    "-fx-spacing": "gap",
    "-fx-hgap": "column-gap",
    "-fx-vgap": "row-gap",
    "-fx-min-width": "min-width",
    "-fx-pref-width": "width",
    "-fx-max-width": "max-width",
    "-fx-min-height": "min-height",
    "-fx-pref-height": "height",
    "-fx-max-height": "max-height",
    "-fx-cursor": "cursor",
    "-fx-graphic-text-gap": "gap",
    "-fx-icon-color": "color",
    "-fx-icon-size": "font-size",
    "-fx-text-alignment": "text-align",
    "-fx-opacity": "opacity",
    "-fx-effect": "box-shadow",
    "-fx-fill": "color",
}

# JavaFX alignment values -> flexbox, applied by the preview's layout classes.
ALIGNMENT = {
    "CENTER-LEFT": "justify-content:flex-start;align-items:center",
    "CENTER": "justify-content:center;align-items:center",
    "CENTER-RIGHT": "justify-content:flex-end;align-items:center",
    "TOP-LEFT": "justify-content:flex-start;align-items:flex-start",
    "TOP-CENTER": "justify-content:center;align-items:flex-start",
    "TOP-RIGHT": "justify-content:flex-end;align-items:flex-start",
    "BOTTOM-LEFT": "justify-content:flex-start;align-items:flex-end",
    "BOTTOM-CENTER": "justify-content:center;align-items:flex-end",
    "BOTTOM-RIGHT": "justify-content:flex-end;align-items:flex-end",
}

# Properties that exist for JavaFX layout only and have no browser counterpart.
IGNORED = {
    "-fx-background-insets",
    "-fx-background",
    "-fx-cell-size",
    "-fx-table-cell-border-color",
    "-fx-alignment",
    "-fx-hbar-policy",
    "-fx-vbar-policy",
    "-fx-fit-to-width",
    "-fx-border-insets",
    "-fx-translate-x",
    "-fx-translate-y",
}

TOKEN = re.compile(r"(?<![a-zA-Z0-9-])(-color-[a-zA-Z0-9-]+)")


def token_values(path):
    """Reads the -color-* declarations from a token sheet, in file order."""
    text = strip_comments(read(path))
    values = {}
    for match in re.finditer(r"(-color-[a-zA-Z0-9-]+)\s*:\s*([^;}]+)[;}]", text):
        values[match.group(1)] = match.group(2).strip()
    return values


def token_block(selector, values):
    lines = [f"{selector} {{"]
    for name, value in values.items():
        lines.append(f"    --{name[1:]}: {substitute(value)};")
    lines.append("    background-color: var(--color-canvas);")
    lines.append("    color: var(--color-text);")
    lines.append("}")
    return "\n".join(lines)


def substitute(value):
    """-color-x -> var(--color-x); JavaFX quirk values cleaned up."""
    value = TOKEN.sub(lambda m: f"var(--{m.group(1)[1:]})", value)
    value = value.replace("!important", "").strip()
    return value


def effects(value):
    """dropshadow(gaussian, <colour>, radius, spread, offsetX, offsetY) -> box-shadow."""
    match = re.match(r"dropshadow\s*\(\s*gaussian\s*,\s*([^,]+),\s*([^,]+),\s*[^,]+,\s*([^,]+),\s*([^)]+)\)", value)
    if not match:
        return None
    colour, radius, offset_x, offset_y = (part.strip() for part in match.groups())
    return f"box-shadow: {substitute(offset_x)} {substitute(offset_y)} {substitute(radius)} {substitute(colour)}"


def translate_selector(selector):
    """JavaFX pseudo-classes and the odd JavaFX-only selector are mapped or dropped."""
    if any(word in selector for word in (":odd", ":even", "virtual-flow", ".filler")):
        return None
    selector = selector.replace(":focused", ":focus")
    selector = selector.replace(":pressed", ":active")
    selector = selector.replace(":selected", ".is-selected")
    selector = selector.replace(":disabled", "[disabled]")
    return selector


def translate_rule(selector, body, dropped):
    declarations = []
    needs_border = False
    for prop, value in re.findall(r"(-[a-zA-Z-]+)\s*:\s*([^;}]+)", body):
        value = value.strip()
        if prop == "-fx-alignment":
            alignment = ALIGNMENT.get(value.upper())
            if alignment:
                declarations.append(alignment)
            continue
        if prop in IGNORED or prop.startswith("-fx-focus") or prop.startswith("-fx-faint"):
            continue
        if prop == "-fx-effect":
            effect = effects(value)
            if effect:
                declarations.append(effect)
            continue
        if prop.startswith("-fx-background-color") or prop == "-fx-background-color":
            # JavaFX supports stacked fills; the browser shows one background, so
            # the first (topmost) fill wins.
            first = split_top_level(value)[0] if split_top_level(value) else value
            declarations.append(f"background-color: {substitute(first)}")
            continue
        css = PROPERTY_MAP.get(prop)
        if css is None:
            dropped.add(prop)
            continue
        if css in ("border-color", "border-width"):
            needs_border = True
        declarations.append(f"{css}: {substitute(value)}")
    if needs_border:
        declarations.append("border-style: solid")
    return declarations


def split_top_level(value):
    """Splits on commas that are not inside parentheses."""
    parts, depth, current = [], 0, ""
    for char in value:
        if char == "(":
            depth += 1
        if char == ")":
            depth -= 1
        if char == "," and depth == 0:
            parts.append(current.strip())
            current = ""
            continue
        current += char
    if current.strip():
        parts.append(current.strip())
    return parts


def translate_stylesheet(path, header, dropped):
    text = strip_comments(read(path))
    out = [header]
    index = 0
    for match in re.finditer(r"([^{}]+)\{([^{}]*)\}", text):
        raw_selector = match.group(1).strip()
        body = match.group(2)
        selectors = []
        for selector in raw_selector.split(","):
            translated = translate_selector(selector.strip())
            if translated:
                selectors.append(translated)
        if not selectors:
            continue
        declarations = translate_rule(raw_selector, body, dropped)
        if not declarations:
            continue
        index += 1
        out.append(",\n".join(selectors) + " {")
        for declaration in declarations:
            out.append("    " + declaration + ";")
        out.append("}")
    return "\n".join(out), index


def strip_comments(text):
    return re.sub(r"/\*.*?\*/", "", text, flags=re.S)


def read(path):
    with open(path, encoding="utf-8") as handle:
        return handle.read()


def main():
    light = token_values(os.path.join(CSS_DIR, "tokens-light.css"))
    dark = token_values(os.path.join(CSS_DIR, "tokens-dark.css"))

    if set(light) != set(dark):
        print("token sheets disagree; light - dark =", set(light) - set(dark),
              "dark - light =", set(dark) - set(light))
        sys.exit(1)

    dropped = set()
    parts = ["""/* ============================================================================
 * GENERATED FILE - do not edit.
 * Produced by tools/design-preview/build_preview.py from the application's own
 * JavaFX stylesheets (src/main/resources/css). Editing this file by hand would
 * let the preview drift away from the shipped design, which is exactly what it
 * exists to prevent.
 * ========================================================================= */
"""]

    parts.append("/* --- token layer, both themes: the browser's equivalent of swapping the\n"
                 "       one token stylesheet the application attaches --- */")
    parts.append(token_block('[data-theme="light"]', light))
    parts.append(token_block('[data-theme="dark"]', dark))

    structure, structure_rules = translate_stylesheet(
        os.path.join(CSS_DIR, "structure.css"),
        "/* --- structural layer (geometry, type, spacing; no colour) --- */", dropped)
    components, component_rules = translate_stylesheet(
        os.path.join(CSS_DIR, "components.css"),
        "/* --- structural layer: control skins (colour by token only) --- */", dropped)

    parts.append(structure)
    parts.append(components)
    parts.append(LAYOUT_PREAMBLE)

    with open(OUT, "w", encoding="utf-8") as handle:
        handle.write("\n\n".join(parts))

    print(f"wrote {os.path.relpath(OUT, ROOT)}")
    print(f"  tokens: {len(light)} in each theme")
    print(f"  structure.css: {structure_rules} rules translated")
    print(f"  components.css: {component_rules} rules translated")
    if dropped:
        print("  JavaFX-only properties dropped (no browser meaning):")
        for prop in sorted(dropped):
            print("    " + prop)


LAYOUT_PREAMBLE = """/* ============================================================================
 * Harness layout scaffolding.
 * ----------------------------------------------------------------------------
 * JavaFX lays panes out itself; the browser does not, so the preview needs two
 * things the stylesheets never had to say: which elements are rows and which are
 * columns, and a box model that does not fight the design's pixel values.
 * ========================================================================= */

*, *::before, *::after { box-sizing: border-box; }
body { margin: 0; background: var(--color-canvas); color: var(--color-text);
       font-family: "Inter", "Segoe UI", Roboto, Arial, sans-serif; font-size: 12.5px; }
.vbox { display: flex; flex-direction: column; }
.hbox { display: flex; flex-direction: row; }
.stack { display: grid; place-items: center; }
.stack > * { grid-area: 1 / 1; }
.grow { flex: 1 1 auto; min-width: 0; min-height: 0; }
.spacer { flex: 1 1 auto; }
.label { white-space: nowrap; }
.wrap { white-space: normal; }
.icon { font-family: "Font Awesome 5 Free Solid", "Inter", sans-serif; font-weight: 900;
        line-height: 1; display: inline-flex; align-items: center; justify-content: center; }
button { font: inherit; border: 0; background: none; color: inherit; padding: 0; }
"""
# Font Awesome ships with the Ikonli pack at build time; the preview loads the
# same glyph font from the pack's resources so icons are identical.
FA_FONT_FACE = """
@font-face { font-family: "Inter"; font-weight: 400; src: url("../../src/main/resources/fonts/Inter-Regular.ttf"); }
@font-face { font-family: "Inter"; font-weight: 500; src: url("../../src/main/resources/fonts/Inter-Medium.ttf"); }
@font-face { font-family: "Inter"; font-weight: 600; src: url("../../src/main/resources/fonts/Inter-SemiBold.ttf"); }
@font-face { font-family: "Inter"; font-weight: 700; src: url("../../src/main/resources/fonts/Inter-Bold.ttf"); }
@font-face { font-family: "Inter"; font-weight: 800; src: url("../../src/main/resources/fonts/Inter-ExtraBold.ttf"); }
@font-face { font-family: "Inter Tabular"; font-weight: 600; src: url("../../src/main/resources/fonts/InterTabular-SemiBold.ttf"); }
@font-face { font-family: "Inter Tabular"; font-weight: 700; src: url("../../src/main/resources/fonts/InterTabular-Bold.ttf"); }
@font-face { font-family: "Inter Tabular"; font-weight: 800; src: url("../../src/main/resources/fonts/InterTabular-ExtraBold.ttf"); }
@font-face { font-family: "Font Awesome 5 Free Solid"; font-weight: 900;
             src: url("../../tools/design-preview/fa-solid-900.ttf"); }
"""

if __name__ == "__main__":
    LAYOUT_PREAMBLE = FA_FONT_FACE + LAYOUT_PREAMBLE
    main()
