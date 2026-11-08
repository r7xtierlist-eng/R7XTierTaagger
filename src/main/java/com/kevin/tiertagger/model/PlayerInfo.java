package com.kevin.tiertagger.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kevin.tiertagger.TierCache;
import com.kevin.tiertagger.TierTagger;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record PlayerInfo(String name, Map<String, Ranking> rankings, String region, int points,
                         int overall, String title, String titleKey) {
    private static final Pattern TIER_PATTERN = Pattern.compile("(?i)^(R)?([HL])T([1-5])$");

    public record Ranking(int tier, int pos, Integer peakTier, Integer peakPos, long attained,
                          boolean retired) {

        /**
         * Lower is better.
         */
        public int comparableTier() {
            return tier * 2 + pos;
        }

        /**
         * Lower is better.
         */
        public int comparablePeak() {
            if (peakTier == null || peakPos == null) {
                return Integer.MAX_VALUE;
            } else {
                return peakTier * 2 + peakPos;
            }
        }

        public NamedRanking asNamed(GameMode mode) {
            return new NamedRanking(mode, this);
        }
    }

    public record NamedRanking(GameMode mode, Ranking ranking) {
    }

    private static final Map<String, Integer> REGION_COLORS = Map.of(
            "NA", 0xff6a6e,
            "EU", 0x6aff6e,
            "SA", 0xff9900,
            "AU", 0xf6b26b,
            "ME", 0xffd966,
            "AS", 0xc27ba0,
            "AF", 0x674ea7,
            "TR", 0xe30a17
    );

    public static CompletableFuture<List<PlayerInfo>> fetchPlayers(HttpClient client) {
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://r7xtierlist.tr/api/players"))
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache, no-store, max-age=0")
                .header("Pragma", "no-cache")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new IllegalStateException("R7XTiers API returned HTTP " + response.statusCode());
                    }

                    JsonElement root = TierTagger.GSON.fromJson(response.body(), JsonElement.class);
                    JsonArray players;
                    if (root != null && root.isJsonArray()) {
                        players = root.getAsJsonArray();
                    } else if (root != null && root.isJsonObject() && root.getAsJsonObject().has("value")) {
                        players = root.getAsJsonObject().getAsJsonArray("value");
                    } else {
                        throw new IllegalStateException("Unexpected R7XTiers API response format");
                    }

                    List<PlayerInfo> result = new ArrayList<>(players.size());
                    for (JsonElement player : players) {
                        if (player.isJsonObject()) result.add(fromJson(player.getAsJsonObject()));
                    }
                    return List.copyOf(result);
                })
                .whenComplete((players, error) -> {
                    if (error != null) TierTagger.getLogger().warn("Error loading players from R7XTiers", error);
                });
    }

    private static PlayerInfo fromJson(JsonObject json) {
        String name = getString(json, "name", "");
        String region = getString(json, "region", "?");
        int points = getInt(json, "overallPoints", 0);
        int overall = getInt(json, "rank", 0);
        String title = json.has("title") && json.get("title").isJsonObject()
                ? getString(json.getAsJsonObject("title"), "label", "")
                : "";
        String titleKey = json.has("title") && json.get("title").isJsonObject()
                ? getString(json.getAsJsonObject("title"), "key", "")
                : "";

        Map<String, Ranking> rankings = new LinkedHashMap<>();
        if (json.has("tiers") && json.get("tiers").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("tiers").entrySet()) {
                if (!entry.getValue().isJsonPrimitive()) continue;
                Matcher matcher = TIER_PATTERN.matcher(entry.getValue().getAsString().trim());
                if (!matcher.matches()) continue;

                boolean retired = matcher.group(1) != null;
                int pos = matcher.group(2).equalsIgnoreCase("H") ? 0 : 1;
                int tier = Integer.parseInt(matcher.group(3));
                rankings.put(entry.getKey(), new Ranking(
                        tier,
                        pos,
                        retired ? tier : null,
                        retired ? pos : null,
                        0,
                        retired
                ));
            }
        }

        return new PlayerInfo(name, Map.copyOf(rankings), region, points, overall, title, titleKey);
    }

    private static String getString(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        JsonElement element = object.get(key);
        try {
            return element != null && element.isJsonPrimitive() ? element.getAsInt() : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public int getRegionColor() {
        return REGION_COLORS.getOrDefault(this.region.toUpperCase(Locale.ROOT), 0xffffff);
    }

    public static Optional<NamedRanking> getHighestRanking(Map<String, Ranking> rankings) {
        return rankings.entrySet().stream()
                .filter(e -> e.getKey() != null)
                .min(Comparator.comparingInt(e -> e.getValue().comparableTier()))
                .map(e -> e.getValue().asNamed(TierCache.findModeOrUgly(e.getKey())));
    }

    @Getter
    @AllArgsConstructor
    public enum PointInfo {
        COMBAT_GRANDMASTER("Combat Grandmaster", 0xE6C622, 0xFDE047),
        COMBAT_MASTER("Combat Master", 0xFBB03B, 0xFFD13A),
        COMBAT_ACE("Combat Ace", 0xCD285C, 0xD65474),
        COMBAT_SPECIALIST("Combat Specialist", 0xAD78D8, 0xC7A3E8),
        COMBAT_CADET("Combat Cadet", 0x9291D9, 0xADACE2),
        COMBAT_NOVICE("Combat Novice", 0x9291D9, 0xFFFFFF),
        ROOKIE("Rookie", 0x6C7178, 0x8B979C),
        UNRANKED("Unranked", 0xFFFFFF, 0xFFFFFF);

        private final String title;
        private final int color;
        private final int accentColor;
    }

    public PointInfo getPointInfo() {
        if (this.points >= 400) {
            return PointInfo.COMBAT_GRANDMASTER;
        } else if (this.points >= 250) {
            return PointInfo.COMBAT_MASTER;
        } else if (this.points >= 100) {
            return PointInfo.COMBAT_ACE;
        } else if (this.points >= 50) {
            return PointInfo.COMBAT_SPECIALIST;
        } else if (this.points >= 20) {
            return PointInfo.COMBAT_CADET;
        } else if (this.points >= 10) {
            return PointInfo.COMBAT_NOVICE;
        } else if (this.points >= 1) {
            return PointInfo.ROOKIE;
        } else {
            return PointInfo.UNRANKED;
        }
    }

    public MutableText getTitleText() {
        String translationKey = switch (this.titleKey) {
            case "combat_grandmaster", "combat_master", "combat_ace", "combat_specialist",
                    "combat_cadet", "combat_novice", "rookie", "unranked" -> "tiertagger.title." + this.titleKey;
            default -> null;
        };
        if (translationKey != null) {
            return Text.translatable(translationKey);
        }

        if (!this.title.isBlank()) return Text.literal(this.title);
        return Text.translatable("tiertagger.title." + this.getPointInfo().name().toLowerCase(Locale.ROOT));
    }

    public List<NamedRanking> getSortedTiers() {
        List<NamedRanking> tiers = new ArrayList<>(this.rankings.entrySet().stream()
                .map(e -> e.getValue().asNamed(TierCache.findModeOrUgly(e.getKey())))
                .toList());

        tiers.sort(Comparator.comparing((NamedRanking a) -> a.ranking.retired, Boolean::compare)
                .thenComparingInt(a -> a.ranking.tier)
                .thenComparingInt(a -> a.ranking.pos));

        return tiers;
    }
}
