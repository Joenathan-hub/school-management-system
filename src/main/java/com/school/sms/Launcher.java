package com.school.sms;

/**
 * Entry point used ONLY by the packaged jar (maven-shade-plugin / jpackage
 * manifest points here, not at Main). A fat jar whose Main-Class extends
 * javafx.application.Application directly can trigger:
 *   "Error: JavaFX runtime components are missing, and are required to run this application"
 * even though the classes are right there in the jar. Routing through a
 * plain main() that isn't itself an Application subclass avoids that check.
 *
 * `mvn javafx:run` during development still uses Main directly (see pom.xml
 * javafx-maven-plugin config) — Launcher only matters for the packaged jar.
 */
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}
