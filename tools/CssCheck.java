import javafx.css.CssParser;
import javafx.css.Declaration;
import javafx.css.Rule;
import javafx.css.Stylesheet;
import javafx.collections.ObservableList;
import javafx.scene.text.Font;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Development-time validator for the two-layer stylesheet design system.
 *
 * It runs the REAL JavaFX CSS parser over every stylesheet in
 * src/main/resources/css and reports, per file:
 *
 *   1. parse errors (the parser's own error list, with line/column);
 *   2. declarations whose value the real JavaFX value converter rejects
 *      (e.g. a font weight of 650, which JavaFX cannot express);
 *   3. every looked-up colour variable referenced but never defined in BOTH
 *      token files, which would silently fall back to nothing at runtime;
 *   4. colour literals found anywhere outside the two token files - the
 *      "no hardcoded colour" acceptance criterion, enforced mechanically.
 *
 * Run it from the repository root (JavaFX must be on the module path):
 *
 *   java --module-path target/javafx --add-modules javafx.graphics \
 *        -cp tools tools.CssCheck src/main/resources/css
 *
 * Exit code is non-zero if anything failed, so it can sit in CI.
 */
public final class CssCheck {

    /** Hex literals, rgb()/rgba()/hsb() function colours - never allowed outside tokens. */
    private static final Pattern COLOR_LITERAL = Pattern.compile(
            "#[0-9a-fA-F]{3,8}\\b|\\brgba?\\s*\\(|\\bhsb\\s*\\(");
    /** CSS colour keywords. "transparent" is not a colour and is allowed anywhere. */
    private static final Pattern NAMED_COLOR = Pattern.compile(
            "(?<![a-zA-Z-])(white|black|grey|gray|silver|red|green|blue|yellow|orange|purple|"
            + "pink|brown|navy|teal|olive|maroon|lime|aqua|fuchsia|lightgrey|lightgray|darkgrey|darkgray)"
            + "(?![a-zA-Z-])");
    private static final Pattern LOOKUP = Pattern.compile("-color-[a-zA-Z0-9-]+");

    private final File dir;
    private int failures = 0;

    private final Map<String, Set<String>> defined = new LinkedHashMap<>();
    private final Map<String, Set<String>> referenced = new TreeMap<>();

    private CssCheck(File dir) {
        this.dir = dir;
    }

    public static void main(String[] args) {
        File cssDir = new File(args.length > 0 ? args[0] : "src/main/resources/css");
        int code = new CssCheck(cssDir).run();
        System.exit(code);
    }

    private int run() {
        File[] files = dir.listFiles((d, n) -> n.endsWith(".css"));
        if (files == null || files.length == 0) {
            System.out.println("No stylesheets found in " + dir.getAbsolutePath());
            return 1;
        }
        java.util.Arrays.sort(files);

        for (File f : files) {
            System.out.println("== " + f.getName() + " ==============================================");
            parse(f);
            scanLiterals(f);
            scanVariables(f);
            System.out.println();
        }
        checkUnresolvedVariables();
        checkTokenParity();

        System.out.println(failures == 0
                ? "CSS CHECK PASSED - 0 parse errors, 0 invalid values, 0 unresolved variables, 0 literals outside tokens"
                : "CSS CHECK FAILED - " + failures + " problem(s)");
        return failures == 0 ? 0 : 1;
    }

    private List<String> parseErrorsOf(CssParser parser) {
        List<String> errors = new ArrayList<>();
        ObservableList<?> list = parser.errorsProperty();
        for (Object o : list) {
            errors.add(String.valueOf(o));
        }
        return errors;
    }

    private void parse(File f) {
        List<String> errors = new ArrayList<>();
        try {
            CssParser parser = new CssParser();
            Stylesheet sheet = parser.parse(f.toURI().toURL());
            errors.addAll(parseErrorsOf(parser));
            int declarations = 0;
            int invalid = 0;
            for (Rule rule : sheet.getRules()) {
                for (Declaration d : rule.getDeclarations()) {
                    declarations++;
                    String raw = String.valueOf(d.getParsedValue());
                    if (raw.contains("-color-")) {
                        continue; // looked-up colours can only be resolved against a live node
                    }
                    try {
                        d.getParsedValue().convert(Font.getDefault());
                    } catch (Throwable t) {
                        invalid++;
                        System.out.println("    INVALID VALUE  " + rule.getSelectors() + " { "
                                + d + " }  -> " + t.getClass().getSimpleName());
                    }
                }
            }
            for (String e : errors) {
                failures++;
                System.out.println("    PARSE ERROR" + e);
            }
            invalid += 0;
            System.out.println("    parsed: " + sheet.getRules().size() + " rules, " + declarations
                    + " declarations, " + errors.size() + " parse errors, " + invalid + " invalid values");
            if (invalid > 0) {
                failures += invalid;
            }
        } catch (Exception ex) {
            failures++;
            System.out.println("    FAILED TO PARSE: " + ex);
        }
    }

    /** Colour literals are allowed only in the two token files. */
    private void scanLiterals(File f) {
        boolean tokenFile = f.getName().startsWith("tokens-");
        List<String> lines = readLines(f);
        int found = 0;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String code = line.contains("/*") ? line.substring(0, line.indexOf("/*")) : line;
            Matcher m = COLOR_LITERAL.matcher(code);
            while (m.find()) {
                found++;
                if (!tokenFile) {
                    failures++;
                    System.out.println("    COLOUR LITERAL  line " + (i + 1) + ": " + line.trim());
                }
            }
            Matcher n = NAMED_COLOR.matcher(code.replaceAll("-color-[a-zA-Z0-9-]+", ""));
            while (n.find()) {
                found++;
                if (!tokenFile) {
                    failures++;
                    System.out.println("    COLOUR KEYWORD  line " + (i + 1) + ": " + line.trim());
                }
            }
        }
        System.out.println("    colour literals in file: " + found + (tokenFile ? " (token file - allowed)" : ""));
    }

    private void scanVariables(File f) {
        boolean tokenFile = f.getName().startsWith("tokens-");
        Map<String, Integer> defs = new HashMap<>();
        for (String line : readLines(f)) {
            String code = line.contains("/*") ? line.substring(0, line.indexOf("/*")) : line;
            Matcher m = LOOKUP.matcher(code);
            Set<String> seen = new HashSet<>();
            while (m.find()) {
                String name = m.group();
                seen.add(name);
                if (code.trim().startsWith(name + ":")) {
                    defs.merge(name, 1, Integer::sum);
                    defined.computeIfAbsent(f.getName(), k -> new HashSet<>()).add(name);
                }
                referenced.computeIfAbsent(f.getName(), k -> new HashSet<>()).add(name);
            }
        }
        System.out.println("    defines " + defs.size() + " looked-up colours");
    }

    private void checkUnresolvedVariables() {
        Set<String> all = new HashSet<>();
        defined.values().forEach(all::addAll);
        int unresolved = 0;
        for (Map.Entry<String, Set<String>> e : referenced.entrySet()) {
            for (String name : e.getValue()) {
                if (!all.contains(name)) {
                    unresolved++;
                    failures++;
                    System.out.println("    UNRESOLVED  " + e.getKey() + " references " + name
                            + " which is defined in no stylesheet");
                }
            }
        }
        Set<String> light = defined.getOrDefault("tokens-light.css", Set.of());
        Set<String> dark = defined.getOrDefault("tokens-dark.css", Set.of());
        Set<String> onlyLight = new HashSet<>(light);
        onlyLight.removeAll(dark);
        Set<String> onlyDark = new HashSet<>(dark);
        onlyDark.removeAll(light);
        System.out.println("== token parity ====================================================");
        System.out.println("    light-only variables: " + onlyLight);
        System.out.println("    dark-only variables:  " + onlyDark);
        if (!onlyLight.isEmpty() || !onlyDark.isEmpty()) {
            failures++;
            System.out.println("    PARITY FAILED - the two token files must define the same names");
        }
    }

    private void checkTokenParity() {
        // covered by checkUnresolvedVariables; kept for readability of the report
    }

    private static List<String> readLines(File f) {
        try {
            return java.nio.file.Files.readAllLines(f.toPath());
        } catch (Exception e) {
            return List.of();
        }
    }
}
