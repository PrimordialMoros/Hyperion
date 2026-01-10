package me.moros.hyperion.abilities.firebending;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.DamageHandler;
import me.moros.hyperion.Hyperion;
import me.moros.hyperion.abilities.Elements.FireAbility;
import me.moros.hyperion.abilities.Elements.RainbowFireAbility;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static me.moros.hyperion.Elements.RAINBOWFIRE;
import static me.moros.hyperion.abilities.Elements.RainbowFireAbility.playRainbowFireParticles;

public class RainbowWave extends FireAbility implements AddonAbility {

    private static final String path = "Abilities.Fire.RainbowWave.";

    private boolean started;
    private double speed;
    private final double fallSpeed = -0.05;
    private final double hitRadius = 1.5;

    private double damage;
    private int maxTicks;
    private long cooldown;

    private int ticksLived = 0;
    private Location currentLocation;
    private Vector direction;
    private final Set<LivingEntity> hitEntities = new HashSet<>();


    public RainbowWave(Player player) {
        super(player);

        if (!player.isOnline()) return;
        if (!bPlayer.canBend(this)) return;
        if (hasAbility(player, RainbowWave.class)) return;
        if (bPlayer.isOnCooldown(this)) return;
        if (bPlayer.canUseSubElement(RAINBOWFIRE)) {
            damage *= RainbowFireAbility.getDamageFactor();
            cooldown *= RainbowFireAbility.getCooldownFactor();
        }

        loadConfigValues();

        this.currentLocation = player.getEyeLocation().add(0, -0.5, 0);
        this.direction = currentLocation.getDirection().normalize().multiply(speed);

        start();
    }

    private void loadConfigValues() {
        FileConfiguration config = Hyperion.getPlugin().getConfig();

        this.cooldown = config.getLong(path + "Cooldown");
        this.damage = config.getDouble(path + "Damage");
        double range = config.getDouble(path + "Range");
        this.speed = config.getDouble(path + "Speed");

        if (speed <= 0) speed = 0.1; // prevent zero or negative speed
        this.maxTicks = (int) (range / speed);
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || !bPlayer.canBend(this)) {
            remove();
            return;
        }

        if (!started) return;

        currentLocation.add(direction);
        currentLocation.add(0, fallSpeed, 0);

        playFirebendingParticles(currentLocation, 10, hitRadius, hitRadius, hitRadius);

        List<Entity> nearby = GeneralMethods.getEntitiesAroundPoint(currentLocation, hitRadius);
        for (Entity entity : nearby) {
            if (entity instanceof LivingEntity living && !entity.equals(player) && !hitEntities.contains(living)) {
                DamageHandler.damageEntity(living, damage, this);
                hitEntities.add(living);
                Vector knockback = living.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.5);
                knockback.setY(0.2);
                living.setVelocity(living.getVelocity().add(knockback));
            }
        }

        ticksLived++;
        if (ticksLived > maxTicks) {
            bPlayer.addCooldown(this);
            remove();
        }
    }

    public void triggerWave() {
        started = true;
    }

    @Override
    public boolean isSneakAbility() {
        return false;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public String getName() {
        return "RainbowWave";
    }

    @Override
    public Location getLocation() {
        return currentLocation;
    }

    @Override
    public void load() {}

    @Override
    public void stop() {}

    @Override
    public String getAuthor() {
        return Hyperion.getAuthor();
    }

    @Override
    public String getVersion() {
        return Hyperion.getVersion();
    }

    @Override
    public String getInstructions() {
        return "Punch twice";
    }

    @Override
    public String getDescription() {
        return "Shoot a beam/wave of RainbowFire at your opponent";
    }
}
