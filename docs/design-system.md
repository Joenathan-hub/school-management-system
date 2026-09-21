# The design system

How the interface is put together, and what to touch when you want to change
something.

## Two layers

```
src/main/resources/css/
├── structure.css     layout, spacing, radii, type scale      no colour at all
├── components.css    control skins, states, legacy screens   no colour at all
├── tokens-light.css  every colour, light                     one file
└── tokens-dark.css   every colour, dark                      one file
```

The rule: **colour exists only in the token sheets.** Structure and components say
`-fx-background-color: -color-primary`, never `#0969DA`. `tools/UiAudit.java`
enforces this — it fails if a colour literal appears anywhere else, in CSS or in
Java.

Switching theme is attaching one token sheet instead of the other. There is no
conditional logic anywhere in the application: no `if (dark)`, no second stylesheet,
no per-component branching. `ThemeManager.applyTheme(scene)` is the only place that
knows which sheet is current.

## The base theme

AtlantaFX (Primer) is installed as the **user agent** stylesheet, so every control
this design does not restyle itself comes from there with proper behaviour: scroll
bars, combo box popups, context menus, tooltips, table headers, spinners, date
pickers, focus rings. The token sheets then redirect AtlantaFX's own colour
variables at this palette (the "base-theme bridge" at the bottom of
`tokens-light.css`), so those inherited controls are painted in the application's
colours instead of Primer's greys. That is why the bridge exists and why removing it
would make the app look like two different products.

Do not hand-style scrollbars, combo popups or context menus "on top of" the base
theme — the bridge already covers them, and two sources of truth is how they end up
mismatched between themes.

## Adding or changing a colour

1. Add the variable to **both** token sheets, with the same name.
2. Give it a value in each theme.
3. Use it in `structure.css` / `components.css` by name.
4. Run `tools/UiAudit.java`. It reports a name that exists in only one theme, and
   if the new colour is used as text on a surface, add the pairing to
   `usedPairs()` in that tool so it is measured from then on.

## Components

`com.school.sms.ui.design` holds the library. Screens assemble controls from it
rather than styling nodes themselves:

| Class | What it provides |
|-------|------------------|
| `Components` | cards, page/card headers, buttons, badges, banners, toasts, avatars, fields, segmented control, toggle, progress rows, skeletons, empty states, filters, money/percent formatting |
| `DataTable` | the data table card: weighted columns, arrow-key rows, Enter to open, pagination, skeleton and empty states |
| `PairedBarChart` | the twelve-column grouped bar chart with legend, reference line and scale caption |
| `Icons` | every glyph, from Ikonli's FontAwesome 5 pack — no raster icons anywhere |
| `Motion` | the four durations the design specifies; JavaFX CSS cannot animate |

`Components.Tone` is how status is expressed. A tone always travels with its word:
`Components.badge("Incomplete", Tone.WARNING)` renders "Incomplete" *and* the tone,
never a bare coloured dot. The same is true of banners and toasts.

## Type and numbers

`Inter` is bundled in `src/main/resources/fonts` in five weights and loaded at
startup; each CSS rule names a fallback chain after it, so a missing font degrades
to the system UI font rather than to nothing.

Anything numeric uses the **`Inter Tabular`** family, in which every digit has the
same advance width, and is right-aligned. That is what makes a column of money line
up on the decimal point; the class is applied by `Components.moneyValue(...)`,
`DataTable.Cell.money(...)` and `Components.progressRow(...)`.

## The rail

Fixed dark in both themes, by design. Its colours are tokens like everything else —
which is why it stays dark in light mode without a single conditional rule.

The navigation is role-filtered: each of the three roles gets its own tree, and an
item a role cannot open is never rendered (not rendered-and-disabled). Collapsing
hides labels, section headings and the wordmark outright — it never truncates them
to an ellipses — centres the icons and gives every item a tooltip. The state and
the theme both live in the JavaFX preference store, so they survive a restart.

## Tools

```bash
# 1. Do the stylesheets still parse, and is colour still only in the tokens?
javac -d /tmp/csscheck -cp "$JAVAFX/*" tools/CssCheck.java
java --module-path "$JAVAFX" --add-modules javafx.graphics -cp /tmp/csscheck \
     CssCheck src/main/resources/css

# 2. Is there colour in Java, and does every pairing pass contrast?
javac -d /tmp/uiaudit tools/UiAudit.java
java -cp /tmp/uiaudit UiAudit .

# 3. Regenerate the visual preview from the stylesheets, then open it
python3 tools/design-preview/build_preview.py
python3 tools/design-preview/check_preview.py
python3 -m http.server 8080 --directory tools/design-preview
```

The preview is generated from the shipped sheets, never hand-drawn, and
`check_preview.py` fails if its pages use a class the stylesheets do not define.
