package me.moros.hyperion.util;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

public class UpdateChecker {

    private final JavaPlugin plugin;
    private final String apiUrl;

    private boolean updateAvailable = false;
    private boolean checked = false;

    private String currentVersion;
    private String latestVersion;

    public UpdateChecker(JavaPlugin plugin, String repo) {
        this.plugin = plugin;
        this.apiUrl = "https://api.github.com/repos/" + repo + "/releases/latest";
    }

    public void checkForUpdate() {
        currentVersion = plugin.getDescription().getVersion();

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

            String token = System.getenv("GITHUB_TOKEN");
            if (token != null && !token.isEmpty()) {
                conn.setRequestProperty("Authorization", "token " + token);
            }

            conn.setRequestMethod("GET");

            String json = new BufferedReader(new InputStreamReader(conn.getInputStream()))
                    .lines().collect(Collectors.joining());

            String tagName = parseJsonField(json, "tag_name");
            if (tagName == null) {
                plugin.getLogger().warning("Update check failed: tag_name not found in GitHub API response");
                checked = true;
                return;
            }

            latestVersion = tagName.startsWith("v") ? tagName.substring(1) : tagName;
            currentVersion = currentVersion.startsWith("v") ? currentVersion.substring(1) : currentVersion;

            updateAvailable = compareVersions(currentVersion, latestVersion) < 0;
            checked = true;

        } catch (IOException e) {
            plugin.getLogger().warning("Update check failed: " + e.getMessage());
            checked = true;
        }
    }

    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("[.-]");
        String[] parts2 = v2.split("[.-]");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            String p1 = i < parts1.length ? parts1[i] : "0";
            String p2 = i < parts2.length ? parts2[i] : "0";

            int cmp;

            try {
                // Compare numeric parts
                int n1 = Integer.parseInt(p1);
                int n2 = Integer.parseInt(p2);
                cmp = Integer.compare(n1, n2);
            } catch (NumberFormatException e) {
                // Handle pre-release comparison
                boolean isPre1 = p1.toUpperCase().startsWith("PRE");
                boolean isPre2 = p2.toUpperCase().startsWith("PRE");

                if (isPre1 && !isPre2) {
                    cmp = -1; // pre-release is older than stable
                } else if (!isPre1 && isPre2) {
                    cmp = 1;  // stable is newer than pre-release
                } else if (isPre1 && isPre2) {
                    cmp = p1.compareTo(p2); // compare pre-releases lexically
                } else {
                    cmp = p1.compareTo(p2); // fallback string compare
                }
            }

            if (cmp != 0) return cmp;
        }

        return 0;
    }


    private String parseJsonField(String json, String field) {
        String search = "\"" + field + "\":\"";
        int index = json.indexOf(search);
        if (index == -1) return null;

        int start = index + search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;

        return json.substring(start, end);
    }

    // Getters
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public boolean hasChecked() {
        return checked;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}
