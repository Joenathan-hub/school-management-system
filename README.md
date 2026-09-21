# School Management System

A desktop school administration application for a Ugandan primary/secondary school:
students and admissions, fees and payments, workers and payroll, requirements and
uniform checklists, report cards, attendance, terms, an audit log, backups and a
finance dashboard. Java 21 + JavaFX 21, backed by a Microsoft Access database
through UCanAccess.

## Running it

```bash
mvn javafx:run
```

Packaging produces a single shaded jar (`mvn package`, main class
`com.school.sms.Launcher`).

## The interface

Two layers and two themes:

| File | Contains |
|------|----------|
| `src/main/resources/css/structure.css` | layout, spacing, radii, type — **no colour** |
| `src/main/resources/css/components.css` | control skins and states — **no colour** |
| `src/main/resources/css/tokens-light.css` | every colour in light mode |
| `src/main/resources/css/tokens-dark.css` | every colour in dark mode |

Light ↔ dark is one stylesheet swap on the scene, with no per-component conditional
logic anywhere. AtlantaFX supplies the base skin for controls the design does not
restyle itself (scrollbars, combo popups, context menus, tooltips, table headers,
spinners, date pickers), and the token sheets redirect its palette at this one.
Icons are vector (Ikonli / FontAwesome 5); text is bundled Inter; numeric columns use
a tabular face so figures line up.

The shell — navigation rail, top bar, command palette (<kbd>Ctrl</kbd>/<kbd>Cmd</kbd>
+<kbd>K</kbd>), toasts, three-state theme and collapsible rail — is described in
[docs/design-system.md](docs/design-system.md), together with how to change a colour,
add a component or find your way around the tools.

## Documentation

* [docs/design-system.md](docs/design-system.md) — how the UI is built and how to change it
* [docs/contrast-audit.md](docs/contrast-audit.md) — every colour pairing, measured in both themes
* [docs/ui-spec-deviations.md](docs/ui-spec-deviations.md) — where the delivery differs from the spec, and why

## Development tools

Not part of the application jar; each one exits non-zero on failure.

| Tool | What it checks |
|------|----------------|
| `tools/CssCheck.java` | every stylesheet parses with JavaFX's own parser, every value converts, every `-color-*` is defined, the themes define the same names, no colour outside the token sheets |
| `tools/UiAudit.java` | no colour literal, inline style or `-fx-` string in Java; WCAG contrast for every pairing used, both themes |
| `tools/design-preview/` | an interactive preview of the dashboard and the component library, generated from the stylesheets themselves |

```bash
python3 tools/design-preview/build_preview.py   # regenerate the preview
python3 tools/design-preview/check_preview.py   # every class the preview uses exists
python3 -m http.server 8080 --directory tools/design-preview
```
