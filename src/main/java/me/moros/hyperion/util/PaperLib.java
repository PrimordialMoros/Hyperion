package me.moros.hyperion.util;


import me.moros.hyperion.Hyperion;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.concurrent.CompletableFuture;

public class PaperLib {
    private static Environment ENVIRONMENT;

    static {
        if (Hyperion.isFolia()) {
            ENVIRONMENT = new Folia();
        } else if (Hyperion.isPaper()) {
            ENVIRONMENT = new Paper();
            } else if (Hyperion.isSpigot()) {
            ENVIRONMENT = new Spigot();
        }

    }

    public static CompletableFuture<Chunk> getChunkAtAsync(Location location) {
        return ENVIRONMENT.getChunkAtAsync(location);
    }

    public static CompletableFuture<Chunk> getChunkAtAsync(Block block) {
        return ENVIRONMENT.getChunkAtAsync(block);
    }

    public static void teleportAsync(Entity entity, Location location) {
        ENVIRONMENT.teleportAsync(entity, location);
    }

    public static CompletableFuture<Boolean> teleportAsync(Entity entity, Location location, TeleportCause cause) {
        return ENVIRONMENT.teleportAsync(entity, location, cause);
    }

    private interface Environment {
        default CompletableFuture<Chunk> getChunkAtAsync(Location location) {
            return this.getChunkAtAsync(location.getBlock());
        }

        CompletableFuture<Chunk> getChunkAtAsync(Block block);

        default CompletableFuture<Boolean> teleportAsync(Entity entity, Location location) {
            return this.teleportAsync(entity, location, TeleportCause.PLUGIN);
        }

        CompletableFuture<Boolean> teleportAsync(Entity entity, Location location, TeleportCause cause);
    }

    private static class Spigot implements Environment {
        @Override
        public CompletableFuture<Chunk> getChunkAtAsync(Block block) {
            return CompletableFuture.completedFuture(block.getChunk());
        }

        @Override
        public CompletableFuture<Boolean> teleportAsync(Entity entity, Location location, TeleportCause cause) {
            entity.teleport(location, cause);
            return CompletableFuture.completedFuture(true);
        }
    }

    private static class Paper implements Environment {
        @Override
        public CompletableFuture<Chunk> getChunkAtAsync(Block block) {
            CompletableFuture<Chunk> future = new CompletableFuture<>();
            Hyperion.scheduler.region(block.getLocation()).run(() -> {
                future.complete(block.getChunk());
            });
            return future;
        }

        @Override
        public CompletableFuture<Boolean> teleportAsync(Entity entity, Location location, TeleportCause cause) {
            return Hyperion.scheduler.teleportAsync(entity, location, cause);
        }
    }

    private static class Folia implements Environment {
        @Override
        public CompletableFuture<Chunk> getChunkAtAsync(Block block) {
            CompletableFuture<Chunk> future = new CompletableFuture<>();
            // Use CJCrafter's scheduler to run synchronously in the region thread
            Hyperion.scheduler.region(block.getLocation()).run(task -> {
                Chunk chunk = block.getWorld().getChunkAt(block);
                future.complete(chunk);
            });
            return future;
        }

        @Override
        public CompletableFuture<Boolean> teleportAsync(Entity entity, Location location, TeleportCause cause) {
            // Folia supports async teleport natively
            return Hyperion.scheduler.teleportAsync(entity, location, cause);
        }
    }
}
