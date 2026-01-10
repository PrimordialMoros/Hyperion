package me.moros.hyperion.abilities.Elements;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.Element.SubElement;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.LightManager;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import me.moros.hyperion.Elements;


import me.moros.hyperion.Hyperion;
import net.kyori.adventure.key.Key;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Fire;
import org.bukkit.entity.Player;

import static me.moros.hyperion.Hyperion.plugin;

public abstract class FireAbility extends ElementalAbility {
    private static final Map<Block, Player> SOURCE_PLAYERS = new ConcurrentHashMap<>();
    private static final Set<BlockFace> IGNITE_FACES;

    public FireAbility(Player player) {
        super(player);
    }

    @Override
    public boolean isIgniteAbility() {
        return true;
    }

    @Override
    public boolean isExplosiveAbility() {
        return false;
    }

    @Override
    public Element getElement() {
        return Element.FIRE;
    }

    @Override
    public void handleCollision(Collision collision) {
        super.handleCollision(collision);
        if (collision.isRemovingFirst()) {
            ParticleEffect.BLOCK_CRACK.display(collision.getLocationFirst(), 10, 1.0F, 1.0F, 1.0F, 0.1, this.getFireType().createBlockData());
        }
    }

    public Material getFireType() {
        return this.getBendingPlayer().canUseSubElement(SubElement.BLUE_FIRE) ? Material.SOUL_FIRE : Material.FIRE;
    }

    public static boolean canFireGrief() {
        return getConfig().getBoolean("Properties.Fire.FireGriefing");
    }

    public void createTempFire(Location loc) {
        this.createTempFire(loc, getConfig().getLong("Properties.Fire.RevertTicks") + (long) ((new Random()).nextDouble() * (double) getConfig().getLong("Properties.Fire.RevertTicks")));
    }

    public void createTempFire(Location loc, long time) {
        if (isIgnitable(loc.getBlock())) {
            new TempBlock(loc.getBlock(), createFireState(loc.getBlock(), this.getFireType() == Material.SOUL_FIRE), time);
            SOURCE_PLAYERS.put(loc.getBlock(), this.getPlayer());
        }
    }

    public double getDayFactor(double value) {
        return this.player != null ? value * getDayFactor(this.player.getWorld()) : value;
    }

    public static double getDayFactor() {
        return getConfig().getDouble("Properties.Fire.DayFactor");
    }

    public static double getDayFactor(double value, World world) {
        return isDay(world) ? value * getDayFactor() : value;
    }

    public static double getDayFactor(World world) {
        return getDayFactor(1.0F, world);
    }

    public static ChatColor getSubChatColor() {
        return ChatColor.valueOf(ConfigManager.getConfig().getString("Properties.Chat.Colors.FireSub"));
    }

    public static boolean isIgnitable(Block block) {
        Block support = block.getRelative(BlockFace.DOWN);
        Location loc = support.getLocation();
        boolean supported = support.getBoundingBox().overlaps(loc.add(0.0F, 0.8, 0.0F).toVector(), loc.add(1.0F, 1.0F, 1.0F).toVector());
        return !isWater(block) && !block.isLiquid() && GeneralMethods.isTransparent(block) && (supported && support.getType().isSolid() || IGNITE_FACES.stream().map((face) -> block.getRelative(face).getType()).anyMatch(FireAbility::isIgnitable));
    }

    public static boolean isIgnitable(Material material) {
        return material.isFlammable() || material.isBurnable();
    }

    public static BlockData createFireState(Block position, boolean blue) {
        Fire fire = (Fire) Material.FIRE.createBlockData();
        if (isIgnitable(position) && position.getRelative(BlockFace.DOWN).getType().isSolid()) {
            return blue ? Material.SOUL_FIRE.createBlockData() : fire;
        } else {
            for (BlockFace face : IGNITE_FACES) {
                fire.setFace(face, false);
                if (isIgnitable(position.getRelative(face))) {
                    fire.setFace(face, true);
                }
            }
            return fire;
        }
    }

    public static void dryWetBlocks(Block block, CoreAbility ability, boolean playSound) {
        if (!GeneralMethods.isRegionProtectedFromBuild(ability, block.getLocation())) {
            if (block.getType() == Material.WET_SPONGE) {
                block.setType(Material.SPONGE);
                if (playSound) {
                    plugin.adventure().world(Key.key(block.getWorld().getName())).playSound(net.kyori.adventure.sound.Sound.sound(Key.key("block.fire.extinguish"), net.kyori.adventure.sound.Sound.Source.PLAYER, 0.5F, 1.0F), block.getLocation().getX(), block.getLocation().getY(), block.getLocation().getZ());
                }
            } else if (isSnow(block)) {
                block.getWorld().spawnParticle(Particle.BLOCK_DUST, block.getLocation().add(0.5F, 0.5F, 0.5F), 2, 0.5F, 0.5F, 0.5F, 0.1, Material.SNOW_BLOCK.createBlockData());
                block.setType(Material.AIR);
                if (playSound) {
                    block.getWorld().playSound(block.getLocation(), Sound.BLOCK_SNOW_BREAK, 1.0F, 1.0F);
                }
            }
        }
    }

    public static void dryWetBlocks(Block block, CoreAbility ability) {
        dryWetBlocks(block, ability, false);
    }

    @Deprecated
    public static boolean isWithinFireShield(Location loc) {
        List<String> list = new ArrayList<>();
        list.add("FireShield");
        return GeneralMethods.blockAbilities(null, list, loc, 0.0F);
    }

    public static void playCombustionSound(Location loc) {
        if (getConfig().getBoolean("Properties.Fire.PlaySound")) {
            float volume = (float) getConfig().getDouble("Properties.Fire.CombustionSound.Volume");
            float pitch = (float) getConfig().getDouble("Properties.Fire.CombustionSound.Pitch");
            Sound sound = Sound.ENTITY_FIREWORK_ROCKET_BLAST;

            try {
                sound = Sound.valueOf(getConfig().getString("Properties.Fire.CombustionSound.Sound"));
            } catch (IllegalArgumentException var8) {
                ProjectKorra.log.warning("Your current value for 'Properties.Fire.CombustionSound.Sound' is not valid.");
            } finally {
                loc.getWorld().playSound(loc, sound, volume, pitch);
            }
        }
    }

    public void emitFirebendingLight(Location location) {
        if (getConfig().getBoolean("Properties.Fire.DynamicLight.Enabled")) {
            int brightness = getConfig().getInt("Properties.Fire.DynamicLight.Brightness");
            long keepAlive = getConfig().getLong("Properties.Fire.DynamicLight.KeepAlive");
            if (brightness < 1 || brightness > 15) {
                throw new IllegalArgumentException("Properties.Fire.DynamicLight.Brightness must be between 1 and 15.");
            }
            LightManager.createLight(location).brightness(brightness).timeUntilFadeout(keepAlive).emit();
        }
    }

    /**
     * Plays the firebending particles, prioritizing RAINBOWFIRE subelement first,
     * then BLUE_FIRE, then default flame particles.
     */
    public void playFirebendingParticles(Location loc, int amount, double xOffset, double yOffset, double zOffset) {
        if (this.getBendingPlayer().canUseSubElement(Elements.RAINBOWFIRE)) {
            // Instantiate and start the swirling rainbow fire particles task
            RainbowFireAbility.playRainbowFireParticles(loc, amount, xOffset, yOffset, zOffset);
        } else if (this.getBendingPlayer().canUseSubElement(SubElement.BLUE_FIRE)) {
            ParticleEffect.SOUL_FIRE_FLAME.display(loc, amount, xOffset, yOffset, zOffset);
        } else {
            ParticleEffect.FLAME.display(loc, amount, xOffset, yOffset, zOffset);
        }
    }

    public static void playFirebendingSound(Location loc) {
        if (getConfig().getBoolean("Properties.Fire.PlaySound")) {
            float volume = (float) getConfig().getDouble("Properties.Fire.FireSound.Volume");
            float pitch = (float) getConfig().getDouble("Properties.Fire.FireSound.Pitch");
            Sound sound = Sound.BLOCK_FIRE_AMBIENT;

            try {
                sound = Sound.valueOf(getConfig().getString("Properties.Fire.FireSound.Sound"));
            } catch (IllegalArgumentException var8) {
                ProjectKorra.log.warning("Your current value for 'Properties.Fire.FireSound.Sound' is not valid.");
            } finally {
                loc.getWorld().playSound(loc, sound, volume, pitch);
            }
        }
    }

    public static void playLightningbendingParticle(Location loc) {
        playLightningbendingParticle(loc, Math.random(), Math.random(), Math.random());
    }

    public static void playLightningbendingParticle(Location loc, double xOffset, double yOffset, double zOffset) {
        GeneralMethods.displayColoredParticle("#01E1FF", loc, 1, xOffset, yOffset, zOffset);
    }

    public static void playLightningbendingSound(Location loc) {
        if (getConfig().getBoolean("Properties.Fire.PlaySound")) {
            float volume = (float) getConfig().getDouble("Properties.Fire.LightningSound.Volume");
            float pitch = (float) getConfig().getDouble("Properties.Fire.LightningSound.Pitch");
            Sound sound = Sound.ENTITY_CREEPER_HURT;

            try {
                sound = Sound.valueOf(getConfig().getString("Properties.Fire.LightningSound.Sound"));
            } catch (IllegalArgumentException var8) {
                ProjectKorra.log.warning("Your current value for 'Properties.Fire.LightningSound.Sound' is not valid.");
            } finally {
                loc.getWorld().playSound(loc, sound, volume, pitch);
            }
        }
    }

    public static void playLightningbendingChargingSound(Location loc) {
        if (getConfig().getBoolean("Properties.Fire.PlaySound")) {
            float volume = (float) getConfig().getDouble("Properties.Fire.LightningCharge.Volume");
            float pitch = (float) getConfig().getDouble("Properties.Fire.LightningCharge.Pitch");
            Sound sound = Sound.BLOCK_BEEHIVE_WORK;

            try {
                sound = Sound.valueOf(getConfig().getString("Properties.Fire.LightningCharge.Sound"));
            } catch (IllegalArgumentException var8) {
                ProjectKorra.log.warning("Your current value for 'Properties.Fire.LightningCharge.Sound' is not valid.");
            } finally {
                loc.getWorld().playSound(loc, sound, volume, pitch);
            }
        }
    }

    public static void playLightningbendingHitSound(Location loc) {
        if (getConfig().getBoolean("Properties.Fire.PlaySound")) {
            float volume = (float) getConfig().getDouble("Properties.Fire.LightningHit.Volume");
            float pitch = (float) getConfig().getDouble("Properties.Fire.LightningHit.Pitch");
            Sound sound = Sound.ENTITY_LIGHTNING_BOLT_THUNDER;

            try {
                sound = Sound.valueOf(getConfig().getString("Properties.Fire.LightningHit.Sound"));
            } catch (IllegalArgumentException var8) {
                ProjectKorra.log.warning("Your current value for 'Properties.Fire.LightningHit.Sound' is not valid.");
            } finally {
                loc.getWorld().playSound(loc, sound, volume, pitch);
            }
        }
    }

    @Deprecated
    public double applyModifiers(double value) {
        return GeneralMethods.applyModifiers(value, this.getDayFactor(1.0F));
    }

    @Deprecated
    public double applyInverseModifiers(double value) {
        return GeneralMethods.applyInverseModifiers(value, this.getDayFactor(1.0F));
    }

    @Deprecated
    public double applyModifiersDamage(double value) {
        return GeneralMethods.applyModifiers(value, this.getDayFactor(1.0F), this.bPlayer.hasElement(Element.BLUE_FIRE) ? getConfig().getDouble("Properties.Fire.BlueFire.DamageFactor", 1.1) : 1.0);
    }

    @Deprecated
    public double applyModifiersRange(double value) {
        return GeneralMethods.applyModifiers(value, this.getDayFactor(1.0F), this.bPlayer.hasElement(Element.BLUE_FIRE) ? getConfig().getDouble("Properties.Fire.BlueFire.RangeFactor", 1.2) : 1.0);
    }

    @Deprecated
    public long applyModifiersCooldown(long value) {
        return GeneralMethods.applyInverseModifiers(value, this.getDayFactor(1.0F), this.bPlayer.hasElement(Element.BLUE_FIRE) ? 1.0 / getConfig().getDouble("Properties.Fire.BlueFire.CooldownFactor", 0.9) : 1.0);
    }

    public static void stopBending() {
        SOURCE_PLAYERS.clear();
    }

    public static Map<Block, Player> getSourcePlayers() {
        return SOURCE_PLAYERS;
    }

    static {
        IGNITE_FACES = new HashSet<>(Arrays.asList(BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.UP));
    }
}
