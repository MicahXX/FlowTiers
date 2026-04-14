package com.flowpvp.client.feature;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LeaderboardManager {

    public static class Entry {
        public final String name;
        public final int elo;
        public final int position;

        public Entry(String name, int elo, int position) {
            this.name = name;
            this.elo = elo;
            this.position = position;
        }
    }

    private static List<Entry> cached = new ArrayList<>();
    private static String cachedMode = "";
    private static int currentPage = 1;
    private static boolean hasMorePages = true;
    private static boolean isLoading = false;

    public static List<Entry> getCached() {
        return cached;
    }

    public static String getCachedMode() {
        return cachedMode;
    }

    public static boolean isLoading() {
        return isLoading;
    }

    public static boolean hasMorePages() {
        return hasMorePages;
    }

    /** Load page 1 of a mode, resetting any previous results. */
    public static void load(String mode) {
        String upperMode = mode.toUpperCase();
        currentPage = 1;
        hasMorePages = true;
        cached = new ArrayList<>();
        cachedMode = upperMode;
        fetchPage(upperMode, 1, false);
    }

    /** Append the next page to the existing results. */
    public static void loadMore() {
        if (isLoading || !hasMorePages || cachedMode.isEmpty()) return;
        fetchPage(cachedMode, currentPage + 1, true);
    }

    private static void fetchPage(String upperMode, int page, boolean append) {
        if (isLoading) return;
        isLoading = true;
        MinecraftClient mc = MinecraftClient.getInstance();

        CompletableFuture.supplyAsync(() -> {
            try {
                String urlStr = "https://flowpvp.gg/api/leaderboard/" + upperMode + "?page=" + page;
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "FlowTiers-Mod/1.0");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(10000);

                int status = conn.getResponseCode();
                if (status != 200) {
                    System.err.println("[FlowTiers] Leaderboard HTTP " + status + " for mode " + upperMode + " page " + page);
                    return new ArrayList<Entry>();
                }

                JsonElement root = JsonParser.parseReader(new InputStreamReader(conn.getInputStream()));
                List<Entry> result = new ArrayList<>();

                JsonArray arr = null;
                if (root.isJsonArray()) {
                    arr = root.getAsJsonArray();
                } else if (root.isJsonObject()) {
                    JsonObject obj = root.getAsJsonObject();
                    for (String key : new String[]{"data", "players", "entries", "results", "leaderboard"}) {
                        if (obj.has(key) && obj.get(key).isJsonArray()) {
                            arr = obj.getAsJsonArray(key);
                            break;
                        }
                    }
                    if (arr == null) {
                        System.err.println("[FlowTiers] Unexpected leaderboard response shape: " + obj.keySet());
                    }
                }

                if (arr != null) {
                    for (JsonElement el : arr) {
                        JsonObject e = el.getAsJsonObject();

                        String name = getStr(e, "lastKnownName", "name", "username", "playerName");
                        if (name == null) continue;

                        int elo;
                        if ("GLOBAL".equals(upperMode)) {
                            elo = getInt(e, "globalElo", "elo", "rating", "totalRating");
                        } else {
                            elo = getInt(e, "totalRating", "elo", "rating", "globalElo");
                        }

                        int pos = getInt(e, "position", "rank", "globalPosition");
                        result.add(new Entry(name, elo, pos));
                    }
                }

                return result;

            } catch (Exception ex) {
                System.err.println("[FlowTiers] Leaderboard fetch failed: " + ex.getMessage());
                return new ArrayList<Entry>();
            }
        }).thenAcceptAsync(result -> mc.execute(() -> {
            isLoading = false;
            if (result.isEmpty()) {
                hasMorePages = false;
            } else {
                if (append) {
                    List<Entry> combined = new ArrayList<>(cached);
                    combined.addAll(result);
                    cached = combined;
                } else {
                    cached = result;
                }
                currentPage = page;
                // If fewer than 10 entries returned, assume last page
                hasMorePages = result.size() >= 10;
            }
        }));
    }

    // Returns the first field present in obj, or null
    private static String getStr(JsonObject obj, String... keys) {
        for (String k : keys) {
            if (obj.has(k) && !obj.get(k).isJsonNull()) {
                return obj.get(k).getAsString();
            }
        }
        return null;
    }

    // Returns the first int field present in obj, or 0
    private static int getInt(JsonObject obj, String... keys) {
        for (String k : keys) {
            if (obj.has(k) && !obj.get(k).isJsonNull()) {
                try { return obj.get(k).getAsInt(); } catch (Exception ignored) {}
            }
        }
        return 0;
    }
}
