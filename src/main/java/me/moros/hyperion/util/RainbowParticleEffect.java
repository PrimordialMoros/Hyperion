package me.moros.hyperion.util;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class RainbowParticleEffect {

    private static final Random random = new Random();

    /**
     * Spawns a fixed number of rainbow particles gradually over time.
     *
     * @param center  The center location
     * @param amount  Total number of particles to spawn
     * @param xOffset Horizontal spread
     * @param yOffset Vertical spread
     * @param zOffset Depth spread
     */
    public static void spawnRainbowParticleEffect(Location center, int amount, double xOffset, double yOffset, double zOffset) {
        World world = center.getWorld();
        if (world == null) return;

        AtomicInteger spawned = new AtomicInteger(0);
        AtomicReference<Float> hue = new AtomicReference<>(0f);

        Runnable task = () -> {
            if (!center.isWorldLoaded()) {
                ThreadUtil.cancelTask(center);
                return;
            }

            int remaining = amount - spawned.get();
            if (remaining <= 0) {
                ThreadUtil.cancelTask(center);
                return;
            }

            int particlesThisTick = Math.min(5, remaining); // spawn up to 5 per tick
            for (int i = 0; i < particlesThisTick; i++) {
                spawnSingleRainbowParticle(world, center, hue.get(), xOffset, yOffset, zOffset);
                spawned.incrementAndGet();
            }

            float newHue = hue.get() + 3f;
            if (newHue >= 360f) newHue -= 360f;
            hue.set(newHue);
        };

        ThreadUtil.ensureLocationTimer(center, task, 1L, 1L);
    }

    /**
     * Animated rainbow effect — spawns exactly `amount` particles total over time, or until duration is reached.
     *
     * @param center   Center location
     * @param amount   Total number of particles to spawn
     * @param plugin   Your plugin instance (for scheduler)
     * @param duration Maximum duration in ticks
     */
    public static void spawnAnimatedRainbow(Location center, int amount, Plugin plugin, int duration) {
        World world = center.getWorld();
        if (world == null) return;

        AtomicInteger ticks = new AtomicInteger(0);
        AtomicInteger spawned = new AtomicInteger(0);
        AtomicReference<Float> hue = new AtomicReference<>(0f);

        Runnable task = () -> {
            if (ticks.incrementAndGet() >= duration || !center.isWorldLoaded()) {
                ThreadUtil.cancelTask(center);
                return;
            }

            int remaining = amount - spawned.get();
            if (remaining <= 0) {
                ThreadUtil.cancelTask(center);
                return;
            }

            int particlesThisTick = Math.min(5, remaining); // spawn up to 5 per tick
            for (int i = 0; i < particlesThisTick; i++) {
                spawnSingleRainbowParticle(world, center, hue.get(), 0.3, 0.4, 0.3);
                spawned.incrementAndGet();
            }

            float newHue = hue.get() + 3f;
            if (newHue >= 360f) newHue -= 360f;
            hue.set(newHue);
        };

        ThreadUtil.ensureLocationTimer(center, task, 1L, 1L);
    }

    /**
     * Spawns a single rainbow particle (smooth hue-based).
     */
    private static void spawnSingleRainbowParticle(World world, Location center, float baseHue,
                                                   double xOffset, double yOffset, double zOffset) {
        Particle particle = Particle.REDSTONE;

        float hue = (baseHue + random.nextFloat() * 20f) % 360f;
        Color color = hsvToColor(hue, 1f, 1f);

        float size = 1.2f + random.nextFloat() * 0.8f;
        DustOptions data = new DustOptions(color, size);

        double offsetX = (random.nextDouble() - 0.5) * 2 * xOffset;
        double offsetY = random.nextDouble() * yOffset;
        double offsetZ = (random.nextDouble() - 0.5) * 2 * zOffset;
        Location loc = center.clone().add(offsetX, offsetY, offsetZ);

        Vector velocity = new Vector(
                (random.nextDouble() - 0.5) * 0.08,
                random.nextDouble() * 0.12,
                (random.nextDouble() - 0.5) * 0.08
        );

        world.spawnParticle(
                particle,
                loc,
                0,
                velocity.getX(),
                velocity.getY(),
                velocity.getZ(),
                0,
                data
        );
    }

    /**
     * Converts HSV to Bukkit Color.
     */
    private static Color hsvToColor(float hue, float saturation, float value) {
        int rgb = java.awt.Color.HSBtoRGB(hue / 360f, saturation, value);
        return Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }
}
