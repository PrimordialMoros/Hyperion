package me.moros.hyperion.abilities.Elements;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.SubAbility;
import me.moros.hyperion.Elements;
import me.moros.hyperion.Hyperion;
import me.moros.hyperion.util.RainbowParticleEffect;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public abstract class RainbowFireAbility extends FireAbility implements SubAbility {

    public RainbowFireAbility(Player player) {
        super(player);
    }

    /**
     * Always plays rainbow fire particles (no fallback).
     *
     * @param loc     Location to play particles at
     * @param amount  Number of particles
     * @param xOffset X offset for particle spread
     * @param yOffset Y offset for particle spread
     * @param zOffset Z offset for particle spread
     */
    public static void playRainbowFireParticles(Location loc, int amount, double xOffset, double yOffset, double zOffset) {

        RainbowParticleEffect.spawnRainbowParticleEffect(loc, amount, xOffset, yOffset, zOffset);
    }

    @Override
    public Class<? extends Ability> getParentAbility() {
        return FireAbility.class;
    }

    @Override
    public Element getElement() {
        return Elements.RAINBOWFIRE;
    }

    public static double getDamageFactor() {
        FileConfiguration config = Hyperion.getPlugin().getConfig();
        return config.getDouble("Properties.Fire.RainbowFire.DamageFactor", 1.0);
    }

    public static double getCooldownFactor() {
        FileConfiguration config = Hyperion.getPlugin().getConfig();
        return config.getDouble("Properties.Fire.RainbowFire.CooldownFactor", 1.0);
    }

    public static double getRangeFactor() {
        FileConfiguration config = Hyperion.getPlugin().getConfig();
        return config.getDouble("Properties.Fire.RainbowFire.RangeFactor", 1.0);
    }
}
