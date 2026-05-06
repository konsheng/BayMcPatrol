package com.baymc.patrol.redis;

import com.baymc.patrol.config.ConfigManager;
import com.baymc.patrol.domain.PatrolTarget;
import com.baymc.patrol.service.OnlinePlayerRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Redis 在线玩家仓库
 */
public final class RedisOnlinePlayerRepository implements OnlinePlayerRepository {
    private final ConfigManager configManager;
    private final RedisManager redisManager;
    private final RedisKey redisKey;

    public RedisOnlinePlayerRepository(ConfigManager configManager, RedisManager redisManager, RedisKey redisKey) {
        this.configManager = configManager;
        this.redisManager = redisManager;
        this.redisKey = redisKey;
    }

    @Override
    public CompletableFuture<Void> publishLocalOnline(List<PatrolTarget> players) {
        return redisManager.execute(commands -> {
            String serverId = configManager.settings().server().id();
            String serverAlias = configManager.settings().server().alias();
            long onlineTtl = configManager.settings().sync().onlineTtl().toSeconds();
            long heartbeatTtl = configManager.settings().sync().heartbeatTtl().toSeconds();

            commands.sadd(redisKey.servers(), serverId);
            commands.setex(redisKey.heartbeat(serverId), heartbeatTtl, Long.toString(System.currentTimeMillis()));
            commands.set(redisKey.serverAlias(serverId), serverAlias);
            commands.del(redisKey.serverOnline(serverId));
            if (!players.isEmpty()) {
                String[] uuids = players.stream().map(target -> target.uuid().toString()).toArray(String[]::new);
                commands.sadd(redisKey.serverOnline(serverId), uuids);
            }
            commands.expire(redisKey.serverOnline(serverId), onlineTtl);
            for (PatrolTarget player : players) {
                commands.setex(redisKey.playerServer(player.uuid()), onlineTtl, serverId);
                commands.setex(redisKey.playerName(player.uuid()), onlineTtl, player.name());
                commands.setex(redisKey.playerBypass(player.uuid()), onlineTtl, Boolean.toString(player.bypass()));
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<List<PatrolTarget>> findAllOnline() {
        return redisManager.execute(commands -> {
            List<PatrolTarget> players = new ArrayList<>();
            Set<String> servers = commands.smembers(redisKey.servers());
            for (String serverId : servers) {
                if (commands.exists(redisKey.heartbeat(serverId)) <= 0L) {
                    continue;
                }
                String alias = Optional.ofNullable(commands.get(redisKey.serverAlias(serverId))).orElse(serverId);
                for (String uuidText : commands.smembers(redisKey.serverOnline(serverId))) {
                    UUID uuid = UUID.fromString(uuidText);
                    String currentServer = commands.get(redisKey.playerServer(uuid));
                    if (currentServer == null || !currentServer.equals(serverId)) {
                        continue;
                    }
                    String name = Optional.ofNullable(commands.get(redisKey.playerName(uuid))).orElse(uuidText);
                    boolean bypass = Boolean.parseBoolean(commands.get(redisKey.playerBypass(uuid)));
                    players.add(new PatrolTarget(uuid, name, serverId, alias, bypass));
                }
            }
            return players;
        });
    }

    @Override
    public CompletableFuture<Optional<PatrolTarget>> findOnlinePlayer(UUID playerUuid) {
        return redisManager.execute(commands -> {
            String serverId = commands.get(redisKey.playerServer(playerUuid));
            if (serverId == null || commands.exists(redisKey.heartbeat(serverId)) <= 0L) {
                return Optional.empty();
            }
            String name = Optional.ofNullable(commands.get(redisKey.playerName(playerUuid))).orElse(playerUuid.toString());
            String alias = Optional.ofNullable(commands.get(redisKey.serverAlias(serverId))).orElse(serverId);
            boolean bypass = Boolean.parseBoolean(commands.get(redisKey.playerBypass(playerUuid)));
            return Optional.of(new PatrolTarget(playerUuid, name, serverId, alias, bypass));
        });
    }
}
