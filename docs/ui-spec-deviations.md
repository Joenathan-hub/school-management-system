# UI rebuild: where the implementation departs from the written specification

The specification was implemented as written wherever it could be. This file is
the separate log it asked for: every point where the delivered code does something
different, with the reason, and what it means for you.

Nothing here is a silent change. If an item below matters to you, it can be
revisited on its own without unpicking anything else.

| # | Point | Spec says | Delivered | Why |
|---|-------|-----------|-----------|-----|
| 1 | Scope of the re-skin | Components applied across the product | Shell + dashboard rebuilt; the other 25 screens keep their layout, re-skinned by the same sheets | Rewriting every screen was out of proportion to the risk; see below |
| 2 | FXML | "No hex in Java/FXML" | No FXML introduced; rule applied as "no colour literal anywhere in Java or resources" | The repo had zero FXML; introducing it would have run two UI technologies side by side |
| 3 | Number of stylesheets | "Exactly two stylesheets" | Two *layers*, four files: `structure.css` + `components.css` (both colourless) and `tokens-light.css` + `tokens-dark.css` | See below |
| 4 | Rail width | 236 px / 68 px, CSS | Width is set and animated from Java; the sheets no longer declare it | The 220 ms interruptible collapse is impossible in JavaFX CSS |
| 5 | Font weights | 650, 680, 720, 750, 780, 790, 800 | 600, 700, 800 | JavaFX `-fx-font-weight` only accepts 100-900 in steps of 100 |
| 6 | Fonts | Bundled Inter TTFs | Bundled and loaded with `Font.loadFont`, not `@font-face` | JavaFX's `@font-face` carries no weight or style metadata |
| 7 | Motion | 220 ms collapse, toast entrances | Implemented in Java (`Motion`), not CSS | JavaFX CSS has no transitions or keyframes |
| 8 | KPI cards | Figure + label | Figure + label + a line saying what the figure is made of | The application has no historical snapshots, so a period-over-period delta would be fabricated |
| 9 | Data table | "Data table" | Hand-built rows, not a `TableView` | Arrow-key navigation and the exact 8/14 cell metrics; existing screens keep their `TableView`s |
| 10 | Contrast of the pale chart series | Pale companion bar mandated | Kept as specified; measured and listed as an accepted exemption | The two series are distinguishable from each other and every figure is printed as text |
| 11 | Verification | Runtime screenshots | Compile + parse + audit + live preview; no screenshots | Neither the JavaFX renderer nor Chromium can start in the build sandbox |
| 12 | Roles | Includes PARENT and STUDENT | ADMINISTRATOR, BURSAR, TEACHER only | The app has three roles; adding two more is a backend change, not a UI one |
| 13 | Theme persistence | Preferences | Preferences for theme *and* rail; the old `app.theme` setting is migrated once | Applies before any database connection exists, including on the sign-in screen |
| 14 | Rail count pill | "count pill" on nav items | Only on **Students List** | It costs a database read per item; one honest count beats six stale ones |

---

## 1. Scope

The specification describes a component library and a shell. Applying the whole
library by rewriting every screen's internals would have meant re-authoring roughly
twenty-five screens (several thousand lines) with **no way to run or visually check
any of it** in the build environment (item 11). That is the highest-risk change
available for the least visible benefit.

What was delivered instead:

* **Shell**: navigation rail, top bar, content region, toasts, command palette,
  theme control, rail collapse, keyboard shortcuts — all new.
* **Dashboard**: rebuilt from the ground up on the new library, with real data.
* **Component library**: complete, in `ui/design/`, used by the dashboard and
  available to every screen.
* **All other screens**: unchanged internals, but re-skinned — they are wrapped by
  the new shell, and the legacy class names they already used are styled by
  `components.css` through the same tokens. A screen added tomorrow inherits the
  theme for free.

The screens that call `MainShell.setCenter(...)` directly now render inside the new
content region with the rail and top bar around them, which is why this works
without touching them.

## 2. FXML

The specification's rule is about colour: it forbids hex literals in Java and FXML.
The repository contains no FXML at all — every screen is programmatic Java. Adding
FXML for the new shell would have produced two coexisting UI technologies in one
codebase, and the tooling to check FXML (which compiles fine and fails at runtime)
is not available here. The rule was therefore applied in its strongest form: there
is no colour literal, no inline style and no CSS property string anywhere in
`src/main/java` — verified by `tools/UiAudit.java` across all 106 files.

## 3. Two layers, four files

The specification says "exactly two stylesheets" and "no second full stylesheet".
Both intentions are preserved:

* the **token layer** is exactly one file per theme, and exactly one of them is
  attached to a scene at a time. Light ↔ dark is still a single stylesheet swap,
  with no conditional logic anywhere in the application;
* the **structural layer** is split across `structure.css` (layout, spacing, radii,
  type scale) and `components.css` (control skins). Both are colourless — the audit
  reports 0 colour literals in each — so neither can become a competing palette.

The split exists for maintainability: the two sheets together are ~1100 lines, and
locating a rule in a 1100-line file is materially harder than in 500-line ones. The
"no second full stylesheet" constraint was about drift, and a colourless sheet
cannot drift into a second theme.

## 4. Rail width, in Java

Everything about the rail's appearance is in the stylesheets: fill, divider,
muted text, active background, the brass indicator, the tile, the item metrics.
The one exception is its **width**. The design asks for a 220 ms, interruptible
collapse; JavaFX CSS has no transitions, so the width is driven by
`Motion.animateWidth` from `RailNav`. Two consequences worth knowing:

* the sheets no longer declare `-fx-min-width` / `-fx-pref-width` / `-fx-max-width`
  for `.rail`, because CSS and Java must never both own the same property — the
  one that wins is not obvious, and the bug it causes is intermittent;
* changing the rail's widths is a Java change (two constants in `RailNav`).

## 5. Font weights

JavaFX's CSS converter accepts only 100, 200 … 900 for `-fx-font-weight`. The
design's intermediate weights (650, 680, 720, 750, 780, 790) cannot be expressed, so
each is mapped to the nearest available face and the design value is recorded in a
comment beside every rule that uses one:

```css
.card-title {
    -fx-font-size: 13px;
    -fx-font-weight: 700;                      /* design: 720 */
}
```

The bundled faces are 400, 500, 600, 700 and 800, plus three tabular faces
(600/700/800) in a separate family used for anything numeric. If you want the
intermediate weights exactly, generate faces for them and name each family for its
weight (JavaFX resolves family + weight by name), then update the rules — the
comments make that search mechanical.

## 8. KPI figures are not deltas

The design's KPI card carries a figure and a label. Showing "▲ 12% vs last week"
would require historical totals, and this application stores only current state —
payments, balances and requirements. Inventing a movement indicator would be a
lie told in a very convincing font, so each card states what its figure is made of
instead: `12 payments recorded`, `Across 7 class(es)`, `Fees or requirements
outstanding`. The measured ratios for those lines are in
[contrast-audit.md](contrast-audit.md).

If you later add daily snapshots, the card is one method (`kpiCard` in
`DashboardHomeScreen`) and the delta classes (`kpi-delta-up`, `kpi-delta-down`)
are already in the sheets.

## 10. Contrast: the pale companion series

The locked palette requires a pale companion bar (`#CFE3FA` light, `#21324A`
dark). It measures **1.31:1** against the card in light mode and **1.34:1** in dark.
That is below the 3:1 that WCAG 2.1 asks of a graphical object someone must be able
to see, so it is listed in the audit as an accepted exemption rather than hidden:

* the two series are distinguishable from each other (3.96:1 / 4.18:1);
* every value the chart plots is also printed as text on the same screen (the
  progress rows, the KPI cards, the table);
* the primary series is 5.19:1 / 5.59:1 against the card.

Every other exemption in the audit is a hairline, a loading placeholder or a
reinforcing tint, each with its reason written out. The audit exits non-zero on
anything that is a genuine failure, so this list cannot grow quietly.

## 11. Verification: what was checked, and how

The specification asked for screenshots. Three paths were tried and all three are
impossible **in this build sandbox**:

1. **Headless JavaFX rendering** — no shipped JavaFX jar contains a Glass platform
   factory that can run without a display, and Monocle is not obtainable here.
   The toolkit refuses to start: `UnsupportedPlatformException` / missing native
   libraries (no X11, GTK, fontconfig, cups or GL).
2. **Chromium screenshots** — the only browser available (a Lambda-oriented
   Chromium build) starts, loads its libraries, and then never completes a render;
   even `--dump-dom` hangs until killed.
3. **Leaked/system JREs** — no `javac` anywhere, so a full `mvn package` is not
   possible either.

Verification therefore has four parts, all of which run in this sandbox and can be
re-run by you:

| Check | Command | Result |
|-------|---------|--------|
| Whole repository compiles | ECJ 3.38, Java 17 source level, against JavaFX 21.0.2 | **0 errors / 106 files, 152 classes** |
| Stylesheets parse | `tools/CssCheck.java` (real `javafx.css.CssParser`) | **0 parse errors, 0 invalid values, 0 unresolved lookups, 0 literals outside tokens** |
| No colour in code | `tools/UiAudit.java` | **0 colour literals, 0 inline styles across 106 files** |
| Contrast, both themes | `tools/UiAudit.java` | **102 pairings measured, all pass or are documented exemptions** |
| Preview completeness | `tools/design-preview/check_preview.py` | **202 classes used, all defined** |

The visual half is delivered as an interactive preview instead of screenshots:
`tools/design-preview/` renders the dashboard and the component library from
`preview.css`, which is **generated from the application's own stylesheets** by
`build_preview.py`. It is not a hand-drawn mockup that can drift; it is the shipped
design translated property for property (`-fx-*` → CSS), with the five properties
that have no browser meaning reported when the script runs. Use the controls at the
top of the page to switch theme and rail state, or link to a state directly:

```
index.html?theme=dark&rail=collapsed
index.html?theme=light&rail=expanded
components.html?theme=dark
```

### What the preview cannot show

* the skin of controls that come from the **base theme** (AtlantaFX): combo box
  popups, date pickers, scrollbars and the tab well. Their *palette* comes from the
  token sheets and is shown; their *shape* is AtlantaFX's and is not reproduced.
* JavaFX-specific rendering details: font hinting, the exact effect blur of the
  dialog shadow, `region` background insets.
* real interaction: the preview's states are switched by the toolbar, not earned by
  hovering.

Please eyeball the four dashboard states (`theme` × `rail`) and the component page
on your own machine — the checklist in the task ("no clipped text, no orphaned
labels after collapse") is exactly what this preview is for.

## 15. Harness colours

`tools/design-preview/harness.css` contains hex values for the **harness toolbar
only** — the dark strip at the top of the preview that switches theme and rail
state. It is a review tool, it is not part of the application, and it is not in the
jar. The audit scans `src/main/java` and `src/main/resources/css` and nothing else,
which is why it can report "0 colour literals" while the toolbar is still visible.

## 17. Screen compatibility

`UIShell.wrap(...)` keeps its exact signature, so all ~30 screens compile and
behave unchanged. Its behaviour now depends on state:

* **signed in** — the screen's content is placed in the shell's content region and
  the rail, top bar and scroll position stay as they were;
* **before sign-in** — sign-in, first-run setup and password reset get a bare frame
  (canvas, typography, centred card) with no rail and no top bar.

Signing out calls `MainShell.reset()`, so the next sign-in rebuilds the shell for
whoever signs in — the rail is never left over from the previous user.
