package com.kevin.tiertagger;

import com.kevin.tiertagger.model.GameMode;
import com.kevin.tiertagger.model.PlayerInfo;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TierCache {
    private static final List<String> MODE_ORDER = List.of("sword", "axe", "mace", "crystal", "uhc", "pot", "nethpot", "smp");
    private static final List<GameMode> DEFAULT_GAMEMODES = MODE_ORDER.stream().map(GameMode::fromId).toList();
    private static final long REFRESH_INTERVAL_MINUTES = 2;
    private static final Map<String, PlayerInfo> EMPTY_PLAYERS = Map.of();
    private static final ScheduledExecutorService REFRESH_EXECUTOR = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "R7XTiertagger-tier-refresh");
        thread.setDaemon(true);
        return thread;
    });
    private static final AtomicBoolean REFRESH_IN_PROGRESS = new AtomicBoolean();
    private static volatile Map<String, PlayerInfo> players = EMPTY_PLAYERS;
    private static volatile List<GameMode> GAMEMODES = List.of();
    private static volatile CompletableFuture<List<PlayerInfo>> loadFuture;
    private static boolean refreshScheduleStarted;

    public static synchronized void init() {
        if (!refreshScheduleStarted) {
            REFRESH_EXECUTOR.scheduleWithFixedDelay(
                    TierCache::refreshPlayers,
                    REFRESH_INTERVAL_MINUTES,
                    REFRESH_INTERVAL_MINUTES,
                    TimeUnit.MINUTES
            );
            refreshScheduleStarted = true;
        }
        refreshPlayers();
    }

    private static CompletableFuture<List<PlayerInfo>> refreshPlayers() {
        if (!REFRESH_IN_PROGRESS.compareAndSet(false, true)) {
            CompletableFuture<List<PlayerInfo>> current = loadFuture;
            return current != null ? current : CompletableFuture.completedFuture(List.copyOf(players.values()));
        }

        CompletableFuture<List<PlayerInfo>> refresh = PlayerInfo.fetchPlayers(TierTagger.getClient())
                .thenApply(fetchedPlayers -> {
                    Map<String, PlayerInfo> refreshedPlayers = new HashMap<>();
                    fetchedPlayers.forEach(player -> refreshedPlayers.put(normalize(player.name()), player));

                    // Swap the complete snapshot only after a successful response so tags never disappear mid-refresh.
                    players = Map.copyOf(refreshedPlayers);
                    updateGamemodes(fetchedPlayers);
                    TierTagger.getLogger().info("Refreshed {} players and {} gamemodes from R7XTiers", players.size(), GAMEMODES.size());
                    return fetchedPlayers;
                });

        loadFuture = refresh.whenComplete((fetchedPlayers, error) -> {
            REFRESH_IN_PROGRESS.set(false);
            if (error != null) TierTagger.getLogger().error("Failed to refresh players from R7XTiers; keeping the last successful cache", error);
        });
        return loadFuture;
    }

    private static void updateGamemodes(List<PlayerInfo> players) {
        Set<String> found = new LinkedHashSet<>();
        players.forEach(player -> found.addAll(player.rankings().keySet()));

        List<GameMode> modes = new ArrayList<>();
        for (String preferred : MODE_ORDER) {
            findId(found, preferred).ifPresent(id -> modes.add(GameMode.fromId(id)));
        }
        found.stream()
                .filter(id -> modes.stream().noneMatch(mode -> mode.id().equalsIgnoreCase(id)))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .map(GameMode::fromId)
                .forEach(modes::add);
        GAMEMODES = List.copyOf(modes);
    }

    private static Optional<String> findId(Set<String> ids, String wanted) {
        return ids.stream().filter(id -> id.equalsIgnoreCase(wanted)).findFirst();
    }

    private static CompletableFuture<List<PlayerInfo>> ensureLoaded() {
        List<PlayerInfo> snapshot = List.copyOf(players.values());
        if (!snapshot.isEmpty()) return CompletableFuture.completedFuture(snapshot);

        CompletableFuture<List<PlayerInfo>> current = loadFuture;
        if (current != null && !current.isCompletedExceptionally()) return current;
        return refreshPlayers();
    }

    public static List<GameMode> getGamemodes() {
        if (GAMEMODES.isEmpty()) {
            if (loadFuture == null || !loadFuture.isDone()) return DEFAULT_GAMEMODES;
            return Collections.singletonList(GameMode.NONE);
        } else {
            return GAMEMODES;
        }
    }

    public static Optional<PlayerInfo> getPlayerInfo(String name) {
        return Optional.ofNullable(players.get(normalize(name)));
    }

    public static Optional<Map<String, PlayerInfo.Ranking>> getPlayerRankings(String name) {
        return getPlayerInfo(name).map(PlayerInfo::rankings);
    }

    public static CompletableFuture<PlayerInfo> searchPlayer(String query) {
        String playerName = query.trim();
        PlayerInfo cached = players.get(normalize(playerName));
        if (cached != null) return CompletableFuture.completedFuture(cached);

        return ensureLoaded().thenCompose(loadedPlayers -> findPlayer(loadedPlayers, playerName)
                .<CompletableFuture<PlayerInfo>>map(CompletableFuture::completedFuture)
                .orElseGet(() -> refreshPlayers().thenApply(refreshedPlayers -> findPlayer(refreshedPlayers, playerName)
                        .orElseThrow(() -> new CompletionException(new NoSuchElementException("Player not found: " + query))))));
    }

    private static Optional<PlayerInfo> findPlayer(List<PlayerInfo> players, String playerName) {
        String normalizedName = normalize(playerName);
        return players.stream()
                .filter(player -> normalize(player.name()).equals(normalizedName))
                .findFirst();
    }

    public static void clearCache() {
        refreshPlayers();
    }

    public static GameMode findNextMode(GameMode current) {
        List<GameMode> modes = getGamemodes();
        if (modes.size() == 1 && modes.getFirst().isNone()) return GameMode.NONE;

        int index = modes.indexOf(current);
        return modes.get((index + 1 + modes.size()) % modes.size());
    }

    public static Optional<GameMode> findMode(String id) {
        return getGamemodes().stream().filter(mode -> mode.id().equalsIgnoreCase(id)).findFirst();
    }

    public static GameMode findModeOrUgly(String id) {
        return findMode(id).orElseGet(() -> GameMode.fromId(id));
    }

    private static String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private TierCache() {
    }
}
