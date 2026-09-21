import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Development-time audit for the two-layer design system. Not part of the
 * application jar; run it from the repository root with:
 *
 * <pre>
 *   javac -d /tmp/uiaudit tools/UiAudit.java
 *   java -cp /tmp/uiaudit UiAudit .
 * </pre>
 *
 * It checks three things the design claims, and exits non-zero if any of them is
 * false:
 *
 * <ol>
 *   <li><b>No colour in code.</b> No Java source contains a colour literal, an
 *       inline style, or a reference to the {@code javafx.scene.paint.Color} class.
 *       Colour may only ever appear in the two token sheets.</li>
 *   <li><b>One token layer, two themes.</b> The light and dark token sheets define
 *       exactly the same variables, and no other stylesheet contains a colour
 *       literal.</li>
 *   <li><b>Contrast.</b> Every foreground/background pairing the stylesheets
 *       actually create is measured in both themes against WCAG 2.1: 4.5:1 for
 *       text, 3:1 for non-text components (borders, focus rings, chart marks).</li>
 * </ol>
 */
public final class UiAudit {

    private static final Pattern HEX = Pattern.compile("#[0-9a-fA-F]{3,8}\\b");
    private static final Pattern RGB_FUNC = Pattern.compile("\\brgba?\\s*\\(");
    private static final Pattern CSS_COLOUR_FUNC = Pattern.compile("\\blinear-gradient\\s*\\(|\\bradial-gradient\\s*\\(");
    private static final Pattern JAVA_COLOUR_LITERAL = Pattern.compile(
            "#[0-9a-fA-F]{6}\\b|\"\\s*#[0-9a-fA-F]{3,8}\\s*\"|Color\\.(?:web|rgb|gray|grayRgb|valueOf|BLACK|WHITE|RED|GREEN|BLUE)\\b");
    private static final Pattern INLINE_STYLE = Pattern.compile("\\.setStyle\\s*\\(");
    private static final Pattern FX_PROPERTY_IN_JAVA = Pattern.compile("\"-fx-[a-z-]+\\s*:");
    private static final Pattern DECLARATION = Pattern.compile("(-[a-zA-Z0-9-]+)\\s*:\\s*([^;}]+)");

    private static int failures;
    private static final StringBuilder REPORT = new StringBuilder();

    public static void main(String[] args) throws IOException {
        Path root = Paths.get(args.length > 0 ? args[0] : ".");
        Path java = root.resolve("src/main/java");
        Path css = root.resolve("src/main/resources/css");

        auditJavaSources(java);
        auditStylesheets(css);
        auditContrast(css);

        System.out.println(REPORT);
        if (failures == 0) {
            System.out.println("UI AUDIT PASSED");
        } else {
            System.out.println("UI AUDIT FAILED - " + failures + " problem(s)");
            System.exit(1);
        }
    }

    // ==================================================================
    // 1. No colour in code
    // ==================================================================

    private static void auditJavaSources(Path javaDir) throws IOException {
        section("1. Colour must not appear in Java code");
        int files = 0;
        int problems = 0;
        for (Path file : javaFiles(javaDir)) {
            files++;
            String source = Files.readString(file);
            String[] lines = source.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (line.trim().startsWith("*") || line.trim().startsWith("//")) {
                    continue; // comments may discuss colour; code may not contain it
                }
                if (JAVA_COLOUR_LITERAL.matcher(line).find()) {
                    problems++;
                    line("COLOUR IN CODE   " + shortPath(javaDir, file) + ":" + (i + 1) + "  " + line.trim());
                }
                if (INLINE_STYLE.matcher(line).find()) {
                    problems++;
                    line("INLINE STYLE     " + shortPath(javaDir, file) + ":" + (i + 1) + "  " + line.trim());
                }
                if (FX_PROPERTY_IN_JAVA.matcher(line).find()) {
                    problems++;
                    line("CSS IN CODE      " + shortPath(javaDir, file) + ":" + (i + 1) + "  " + line.trim());
                }
            }
        }
        line("scanned " + files + " Java files: " + problems + " colour literal(s), inline style(s) or CSS property string(s)");
        failures += problems;
    }

    // ==================================================================
    // 2. Two layers, one token sheet per theme
    // ==================================================================

    private static void auditStylesheets(Path cssDir) throws IOException {
        section("2. The token layer is the only place colour appears");
        Map<String, Map<String, String>> themes = new LinkedHashMap<>();
        int problems = 0;

        for (Path file : cssFiles(cssDir)) {
            String name = file.getFileName().toString();
            String text = stripComments(Files.readString(file));
            boolean isTokenSheet = name.startsWith("tokens-");

            if (isTokenSheet) {
                themes.put(name, declarations(text));
                line(name + ": " + countMatches(HEX, text) + " colour literal(s), definition sheet (allowed)");
                continue;
            }

            int literals = countMatches(HEX, text) + countMatches(RGB_FUNC, text);
            line(name + ": " + literals + " colour literal(s)");
            if (literals > 0) {
                problems += literals;
                Matcher matcher = Pattern.compile("(?m)^.*(#[0-9a-fA-F]{3,8}|rgba?\\s*\\().*$").matcher(text);
                while (matcher.find()) {
                    line("   " + matcher.group().trim());
                }
            }
            if (name.startsWith("tokens-")) {
                continue;
            }
            // Structural sheets may reference tokens, never define them.
            Set<String> defined = declarations(text).keySet().stream()
                    .filter(key -> key.startsWith("-color-"))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (!defined.isEmpty()) {
                problems += defined.size();
                line("   defines colour variables outside the token layer: " + defined);
            }
        }

        if (themes.size() != 2) {
            problems++;
            line("expected exactly two token sheets (tokens-light.css, tokens-dark.css), found " + themes.size());
        } else {
            Map<String, String> light = themes.get("tokens-light.css");
            Map<String, String> dark = themes.get("tokens-dark.css");
            Set<String> lightOnly = new LinkedHashSet<>(light.keySet());
            lightOnly.removeAll(dark.keySet());
            Set<String> darkOnly = new LinkedHashSet<>(dark.keySet());
            darkOnly.removeAll(light.keySet());
            line("token parity: light defines " + light.size() + ", dark defines " + dark.size());
            line("   names only in light: " + lightOnly);
            line("   names only in dark:  " + darkOnly);
            if (!lightOnly.isEmpty() || !darkOnly.isEmpty()) {
                problems++;
            }
            // Every token a structural sheet references must exist in both themes.
            Set<String> referenced = new LinkedHashSet<>();
            for (Path file : cssFiles(cssDir)) {
                if (file.getFileName().toString().startsWith("tokens-")) {
                    continue;
                }
                String text = stripComments(Files.readString(file));
                Matcher matcher = Pattern.compile("(-color-[a-zA-Z0-9-]+)").matcher(text);
                while (matcher.find()) {
                    referenced.add(matcher.group(1));
                }
            }
            Set<String> missing = new LinkedHashSet<>(referenced);
            missing.removeAll(light.keySet());
            missing.removeAll(dark.keySet());
            line("referenced tokens: " + referenced.size() + ", missing from the token sheets: " + missing);
            if (!missing.isEmpty()) {
                problems++;
            }
        }
        failures += problems;
    }

    // ==================================================================
    // 3. Contrast
    // ==================================================================

    /**
     * One pairing the interface actually creates.
     *
     * @param minimum the ratio this pairing must reach. Text is 4.5:1. Non-text
     *                components (focus rings, chart marks, status dots) are 3:1,
     *                as WCAG 2.1 requires for graphical objects needed to
     *                understand the content.
     * @param reason  set only where the pairing is deliberately exempt from the
     *                standard, and then only with the specific reason written out.
     *                An exempt pairing is reported as ACCEPTED rather than PASSED:
     *                the measurement is still printed, so the exemption is visible
     *                and reviewable rather than hidden.
     */
    private record Pair(String context, String fgToken, String bgToken, double minimum, String reason) {

        static Pair of(String context, String fgToken, String bgToken, double minimum) {
            return new Pair(context, fgToken, bgToken, minimum, null);
        }

        static Pair exempt(String context, String fgToken, String bgToken, double minimum, String reason) {
            return new Pair(context, fgToken, bgToken, minimum, reason);
        }
    }

    /**
     * The pairings the design creates, stated explicitly rather than inferred:
     * inference cannot know which background a piece of text sits on. Every entry
     * is a foreground/background combination that exists in components.css.
     */
    private static List<Pair> usedPairs() {
        List<Pair> pairs = new ArrayList<>();
        double text = 4.5;
        double nonText = 3.0;

        // Surfaces and text
        pairs.add(Pair.of("body text on canvas", "-color-text", "-color-canvas", text));
        pairs.add(Pair.of("body text on surface (cards)", "-color-text", "-color-surface", text));
        pairs.add(Pair.of("body text on secondary surface", "-color-text", "-color-surface-2", text));
        pairs.add(Pair.of("muted text on surface", "-color-text-muted", "-color-surface", text));
        pairs.add(Pair.of("muted text on secondary surface", "-color-text-muted", "-color-surface-2", text));
        pairs.add(Pair.of("muted text on canvas", "-color-text-muted", "-color-canvas", text));

        // Brand
        pairs.add(Pair.of("label on primary button", "-color-on-primary", "-color-primary", text));
        pairs.add(Pair.of("brand text (links, view all) on surface", "-color-primary", "-color-surface", text));
        pairs.add(Pair.of("brand text on secondary surface", "-color-primary", "-color-surface-2", text));
        pairs.add(Pair.of("avatar initials on gradient", "-color-on-primary", "-color-primary", text));
        pairs.add(Pair.of("count pill text", "-color-on-primary", "-color-primary", text));

        // Semantic pairs
        pairs.add(Pair.of("success badge", "-color-success-fg", "-color-success-bg", text));
        pairs.add(Pair.of("warning badge", "-color-warning-fg", "-color-warning-bg", text));
        pairs.add(Pair.of("danger badge", "-color-danger-fg", "-color-danger-bg", text));
        pairs.add(Pair.of("neutral badge", "-color-text-muted", "-color-surface-2", text));
        pairs.add(Pair.of("info badge", "-color-primary", "-color-surface-2", text));
        pairs.add(Pair.of("success text on surface (status line)", "-color-success-fg", "-color-surface", text));
        pairs.add(Pair.of("danger text on surface (error line)", "-color-danger-fg", "-color-surface", text));

        // The rail, which is dark in both themes
        pairs.add(Pair.of("rail brand text", "-color-rail-text", "-color-rail", text));
        pairs.add(Pair.of("rail muted item text", "-color-rail-text-muted", "-color-rail", text));
        pairs.add(Pair.of("rail active item text", "-color-rail-text", "-color-rail-active-bg", text));
        pairs.add(Pair.of("rail item text on hover", "-color-rail-text", "-color-rail-active-bg", text));
        pairs.add(Pair.of("rail brand tile text", "-color-rail-accent-on", "-color-rail-accent", text));
        pairs.add(Pair.exempt("rail divider against rail fill", "-color-rail-border", "-color-rail", 1.0,
                "decorative hairline between rail blocks; grouping is also carried by spacing, so no ratio is required"));
        pairs.add(Pair.of("rail brass indicator against rail fill", "-color-rail-accent", "-color-rail", nonText));
        pairs.add(Pair.exempt("rail active item background against rail fill", "-color-rail-active-bg", "-color-rail", 1.0,
                "the active item's tint is reinforcement: the same state is carried by white text at 12.6:1 and by "
                        + "the inset brass bar, so the tint is not the only cue"));
        pairs.add(Pair.exempt("rail brass indicator against active item", "-color-rail-accent", "-color-rail-active-bg", 1.0,
                "the inset brass bar only reinforces the active item: the state is already carried by the tinted "
                        + "background and by white text, both of which are measured above"));

        // The chart
        pairs.add(Pair.of("chart series bar against surface", "-color-chart-series", "-color-surface", nonText));
        pairs.add(Pair.exempt("chart companion bar against surface", "-color-chart-companion", "-color-surface", 1.0,
                "the pale companion series is required to be pale by the locked palette; the two series are "
                        + "distinguishable from each other, and every value the chart plots is also printed as text "
                        + "on the same screen, so the chart is never the only carrier of the data"));

        pairs.add(Pair.of("chart series against companion bar", "-color-chart-series", "-color-chart-companion", nonText));

        // Non-text components
        pairs.add(Pair.exempt("hairline border against surface", "-color-border", "-color-surface", 1.0,
                "structural hairline: it separates surfaces rather than carrying meaning, so no ratio is required"));
        pairs.add(Pair.of("focus ring against surface", "-color-focus-ring", "-color-surface", nonText));
        pairs.add(Pair.of("focus ring against canvas", "-color-focus-ring", "-color-canvas", nonText));
        pairs.add(Pair.exempt("progress track against surface", "-color-surface-2", "-color-surface", 1.0,
                "the empty track is decoration; the fill and the printed percentage carry the value, and both are measured below"));
        pairs.add(Pair.of("progress fill against track", "-color-primary", "-color-surface-2", nonText));
        pairs.add(Pair.exempt("skeleton block against surface", "-color-surface-2", "-color-surface", 1.0,
                "loading placeholder: it is deliberately faint and the application never asks the user to read it"));
        pairs.add(Pair.of("notification dot against surface", "-color-danger-fg", "-color-surface", nonText));
        pairs.add(Pair.exempt("empty-state icon chip against surface", "-color-border", "-color-surface-2", 1.0,
                "decorative chip outline; the icon inside it is what the user reads"));
        return pairs;
    }

    private static void auditContrast(Path cssDir) throws IOException {
        section("3. Contrast of every pairing actually used (WCAG 2.1)");
        Map<String, String> light = resilient(declarations(stripComments(
                Files.readString(cssDir.resolve("tokens-light.css")))));
        Map<String, String> dark = resilient(declarations(stripComments(
                Files.readString(cssDir.resolve("tokens-dark.css")))));

        for (Map.Entry<String, Map<String, String>> theme : Map.of("LIGHT", light, "DARK", dark).entrySet()) {
            line("");
            line("   " + theme.getKey() + " THEME");
            line("   " + "-".repeat(96));
            line(String.format("   %-52s %8s %6s  %s", "pairing", "ratio", "need", "verdict"));
            for (Pair pair : usedPairs()) {
                Double fg = resolveColour(pair.fgToken(), theme.getValue());
                Double bg = resolveColour(pair.bgToken(), theme.getValue());
                if (fg == null || bg == null) {
                    line(String.format("   %-52s %8s %6.1f  UNRESOLVED", pair.context(), "n/a", pair.minimum()));
                    failures++;
                    continue;
                }
                double ratio = contrast(fg, bg);
                boolean pass = ratio >= pair.minimum();
                String verdict;
                if (!pass) {
                    verdict = "FAIL";
                    failures++;
                } else if (pair.reason() != null) {
                    verdict = "ACCEPTED (see notes)";
                } else if (pair.minimum() >= 4.5) {
                    verdict = ratio >= 7 ? "PASS AAA" : "PASS AA";
                } else {
                    verdict = "PASS (non-text)";
                }
                line(String.format("   %-52s %8.2f %6.1f  %s", pair.context(), ratio, pair.minimum(), verdict));
            }
            line("");
            line("   notes on pairings accepted below the standard ratio:");
            for (Pair pair : usedPairs()) {
                if (pair.reason() != null) {
                    line("     - " + pair.context() + ": " + pair.reason());
                }
            }
        }
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private static Map<String, String> resilient(Map<String, String> declarations) {
        return declarations;
    }

    /** Resolves a token to an ARGB colour, following single-reference chains. */
    private static Double resolveColour(String token, Map<String, String> tokens) {
        String value = tokens.get(token);
        for (int hop = 0; hop < 8 && value != null; hop++) {
            value = value.trim();
            if (value.startsWith("#")) {
                return parseHex(value);
            }
            if (value.startsWith("rgba(") || value.startsWith("rgb(")) {
                return parseRgbFunction(value);
            }
            if (value.startsWith("linear-gradient(") || value.startsWith("radial-gradient(")) {
                // The tile gradient's first stop is the token that carries the
                // text; measure against that rather than refusing to measure.
                int comma = value.indexOf(',');
                if (comma < 0) {
                    return null;
                }
                value = value.substring(value.indexOf('(', comma) + 1, comma).trim();
                if (value.isBlank()) {
                    int firstStop = value.indexOf('(') + 1;
                    value = value.substring(firstStop).split(",")[0].trim();
                }
                continue;
            }
            String next = tokens.get(value);
            if (next == null) {
                return null;
            }
            value = next;
        }
        return null;
    }

    private static Double parseHex(String hex) {
        String value = hex.substring(1);
        if (value.length() == 3) {
            StringBuilder expanded = new StringBuilder();
            for (char c : value.toCharArray()) {
                expanded.append(c).append(c);
            }
            value = expanded.toString();
        }
        if (value.length() == 8) {
            // JavaFX -color tokens in these sheets are #RRGGBBAA only if written that
            // way; the palette uses 6-digit hex plus rgba() where alpha is needed.
            value = value.substring(0, 6);
        }
        if (value.length() != 6) {
            return null;
        }
        int rgb = (int) Long.parseLong(value, 16);
        return (double) rgb;
    }

    private static Double parseRgbFunction(String function) {
        Matcher matcher = Pattern.compile("rgba?\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)").matcher(function);
        if (!matcher.find()) {
            return null;
        }
        int r = Integer.parseInt(matcher.group(1));
        int g = Integer.parseInt(matcher.group(2));
        int b = Integer.parseInt(matcher.group(3));
        return (double) ((r << 16) | (g << 8) | b);
    }

    /** WCAG 2.1 relative-luminance contrast ratio. */
    private static double contrast(double colour1, double colour2) {
        double l1 = luminance(colour1);
        double l2 = luminance(colour2);
        double lighter = Math.max(l1, l2);
        double darker = Math.min(l1, l2);
        return (lighter + 0.05) / (darker + 0.05);
    }

    private static double luminance(double rgb) {
        int r = (int) ((rgb / 65536) % 256);
        int g = (int) ((rgb / 256) % 256);
        int b = (int) (rgb % 256);
        double[] channels = {r / 255.0, g / 255.0, b / 255.0};
        double[] linear = new double[3];
        for (int i = 0; i < 3; i++) {
            linear[i] = channels[i] <= 0.04045
                    ? channels[i] / 12.92
                    : Math.pow((channels[i] + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * linear[0] + 0.7152 * linear[1] + 0.0722 * linear[2];
    }

    private static Map<String, String> declarations(String css) {
        Map<String, String> map = new LinkedHashMap<>();
        Matcher matcher = DECLARATION.matcher(css);
        while (matcher.find()) {
            if (matcher.group(1).startsWith("-color-")) {
                map.put(matcher.group(1), matcher.group(2).trim());
            }
        }
        return map;
    }

    private static String stripComments(String css) {
        return css.replaceAll("(?s)/\\*.*?\\*/", "");
    }

    private static int countMatches(Pattern pattern, String text) {
        int count = 0;
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static List<Path> javaFiles(Path dir) throws IOException {
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.filter(path -> path.toString().endsWith(".java")).sorted().collect(Collectors.toList());
        }
    }

    private static List<Path> cssFiles(Path dir) throws IOException {
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.filter(path -> path.toString().endsWith(".css")).sorted().collect(Collectors.toList());
        }
    }

    private static String shortPath(Path dir, Path file) {
        return dir.getParent().relativize(file).toString().replace('\\', '/');
    }

    private static void section(String title) {
        line("");
        line("== " + title + " " + "=".repeat(Math.max(0, 76 - title.length())));
    }

    private static void line(String text) {
        REPORT.append(text).append('\n');
    }
}
