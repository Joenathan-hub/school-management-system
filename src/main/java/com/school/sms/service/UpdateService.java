package com.school.sms.service;

import com.school.sms.util.AppConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

/**
 * Checks GitHub Releases for a newer version of the app, and can download +
 * launch the new installer directly. The installer itself handles the
 * in-place upgrade — as long as jpackage was built with the SAME
 * --win-upgrade-uuid across every version (see build notes).
 */
public class UpdateService {

    /** Bump this to match pom.xml's <version> every time you cut a new release. */
    public static final String CURRENT_VERSION = "1.1.0";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static class UpdateInfo {
        public String latestVersion;
        public String releaseNotes;
        public String releaseUrl;
        public String downloadUrl; // direct link to the .exe asset, or null if none found
    }

    private String owner() { return AppConfig.get("github.owner", ""); }
    private String repo()  { return AppConfig.get("github.repo", ""); }

    /** Returns update info if a newer release exists, or null if already up to date / no releases / offline. */
    public UpdateInfo checkForUpdate() {
        if (owner().isBlank() || repo().isBlank()) {
            System.err.println("github.owner / github.repo not set in config.properties — skipping update check.");
            return null;
        }
        try {
            String url = String.format("https://api.github.com/repos/%s/%s/releases/latest", owner(), repo());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/vnd.github+json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("GitHub release check returned status " + response.statusCode());
                return null;
            }

            JSONObject json = new JSONObject(response.body());
            String tagName = json.optString("tag_name", "").replaceFirst("^v", "");
            if (tagName.isBlank()) return null;

            if (compareVersions(tagName, CURRENT_VERSION) <= 0) {
                return null; // already up to date
            }

            UpdateInfo info = new UpdateInfo();
            info.latestVersion = tagName;
            info.releaseNotes = json.optString("body", "");
            info.releaseUrl = json.optString("html_url", "");

            JSONArray assets = json.optJSONArray("assets");
            if (assets != null) {
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject asset = assets.getJSONObject(i);
                    String name = asset.optString("name", "");
                    if (name.toLowerCase().endsWith(".exe")) {
                        info.downloadUrl = asset.optString("browser_download_url", null);
                        break;
                    }
                }
            }

            return info;
        } catch (IOException | InterruptedException e) {
            System.err.println("Update check failed (likely offline): " + e.getMessage());
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            System.err.println("Update check failed: " + e.getMessage());
            return null;
        }
    }

    /** Downloads the installer to a temp file and returns its path, or null on failure. */
    public Path downloadInstaller(String downloadUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .timeout(Duration.ofMinutes(5))
                    .GET()
                    .build();

            Path tempFile = Path.of(System.getProperty("java.io.tmpdir"), "SchoolManagementSystem-Update.exe");
            HttpResponse<Path> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofFile(tempFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));

            return response.statusCode() == 200 ? response.body() : null;
        } catch (Exception e) {
            System.err.println("Failed to download update: " + e.getMessage());
            return null;
        }
    }

    /** Launches the downloaded installer. Caller should close the app right after — the installer needs the old files unlocked. */
    public boolean launchInstaller(Path installerPath) {
        try {
            new ProcessBuilder(installerPath.toString()).start();
            return true;
        } catch (IOException e) {
            System.err.println("Failed to launch installer: " + e.getMessage());
            return false;
        }
    }

    private int compareVersions(String v1, String v2) {
        String[] a = v1.split("\\.");
        String[] b = v2.split("\\.");
        int len = Math.max(a.length, b.length);
        for (int i = 0; i < len; i++) {
            int x = i < a.length ? parseIntSafe(a[i]) : 0;
            int y = i < b.length ? parseIntSafe(b[i]) : 0;
            if (x != y) return Integer.compare(x, y);
        }
        return 0;
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s.replaceAll("[^0-9]", "")); } catch (Exception e) { return 0; }
    }
}