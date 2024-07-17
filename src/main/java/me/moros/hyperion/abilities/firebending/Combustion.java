/*
 * Copyright 2016-2024 Moros
 *
 * This file is part of Hyperion.
 *
 * Hyperion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Hyperion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Hyperion. If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.hyperion.abilities.firebending;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.CombustionAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.firebending.util.FireDamageTimer;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import me.moros.hyperion.Hyperion;
import me.moros.hyperion.methods.CoreMethods;
import me.moros.hyperion.util.FastMath;
import me.moros.hyperion.util.MaterialCheck;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class Combustion extends CombustionAbility implements AddonAbility {
	private Location location;

	@Attribute(Attribute.DAMAGE)
	private double damage;
	@Attribute(Attribute.SPEED)
	private double speed;
	@Attribute(Attribute.RANGE)
	private int range;
	@Attribute("Power")
	private double power;
	private double misfireModifier;
	@Attribute(Attribute.CHARGE_DURATION)
	private long chargeTime;
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute("RegenDelay")
	private long regenDelay;
	@Attribute(Attribute.FIRE_TICK)
	private int fireTicks;

	private boolean scalingDamageByDistance;
	private boolean charged;
	private boolean launched;
	private boolean hasExploded;

	private double initialHealth;
	private double distanceTravelled;
	private int currentRingPoint;

	private double randomBeamDistance = 0;

	public Combustion(final Player player) {
		super(player);

		if (!isEnabled()) return;

		if (isWater(player.getEyeLocation().getBlock()) || hasAbility(player, Combustion.class) || !bPlayer.canBend(this)) {
			return;
		}

		damage = Hyperion.getPlugin().getConfig().getDouble("Abilities.Fire.Combustion.Damage");
		speed = Hyperion.getPlugin().getConfig().getDouble("Abilities.Fire.Combustion.Speed");
		cooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Fire.Combustion.Cooldown");
		range = Hyperion.getPlugin().getConfig().getInt("Abilities.Fire.Combustion.Range");
		chargeTime = Hyperion.getPlugin().getConfig().getLong("Abilities.Fire.Combustion.ChargeTime");
		power = Hyperion.getPlugin().getConfig().getDouble("Abilities.Fire.Combustion.Power");
		misfireModifier = Hyperion.getPlugin().getConfig().getDouble("Abilities.Fire.Combustion.MisfireModifier");
		regenDelay = Hyperion.getPlugin().getConfig().getLong("Abilities.Fire.Combustion.RegenDelay");
		fireTicks = Hyperion.getPlugin().getConfig().getInt("Abilities.Fire.Combustion.FireTicks");
		scalingDamageByDistance = Hyperion.getPlugin().getConfig().getBoolean("Abilities.Fire.Combustion.ScaleDamageByDistance");

		charged = chargeTime <= 0;
		launched = false;
		hasExploded = false;

		initialHealth = player.getHealth();
		location = player.getEyeLocation();
		damage = getDayFactor(damage, player.getWorld());
		range = (int) getDayFactor(range, player.getWorld());

		start();
	}

	@Override
	public void progress() {
		if (!charged && !player.isSneaking()) {
			remove();
			return;
		}

		if (launched) {
			if (!bPlayer.canBendIgnoreBindsCooldowns(this)) {
				remove();
				return;
			}
			advanceLocation();
		} else {
			if (!bPlayer.canBendIgnoreCooldowns(this) || isWater(player.getEyeLocation().getBlock())) {
				remove();
				return;
			}
			location = player.getEyeLocation();
			if (chargeTime != 0) {
				playParticleRing();
				if (charged) {
					CoreMethods.playFocusParticles(player);
				} else {
					if (System.currentTimeMillis() > getStartTime() + chargeTime) {
						charged = true;
					}
				}
			}
			if (player.getHealth() + 0.5 < initialHealth) {
				createExplosion(player.getEyeLocation(), power + misfireModifier, damage + misfireModifier);
				return;
			}

			if (charged && (!player.isSneaking() || chargeTime <= 0)) {
				bPlayer.addCooldown(this);
				launched = true;
			}
		}
	}

	private void advanceLocation() {
		final Vector direction = player.getEyeLocation().getDirection();
		ThreadLocalRandom rand = ThreadLocalRandom.current();
		if (distanceTravelled >= randomBeamDistance) {
			player.getWorld().playSound(location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.5f, 0.01F);
			randomBeamDistance = distanceTravelled + 7 + 3 * rand.nextGaussian();
			double radius = rand.nextDouble(0.6, 1.6);
			for (int angle = 0; angle <= 360; angle += 12) {
				final Vector temp = GeneralMethods.getOrthogonalVector(direction, angle, 0.2);
				final Vector dir = GeneralMethods.getOrthogonalVector(direction, angle, radius);
				ParticleEffect.FIREWORKS_SPARK.display(location.clone().add(temp), 0, dir.getX(), dir.getY(), dir.getZ(), 0.12);
			}
		}
		for (int i = 0; i < NumberConversions.round(speed / 0.4); i++) {
			distanceTravelled += 0.4;
			if (distanceTravelled > range) {
				remove();
				return;
			}
			location.add(direction.clone().multiply(0.4));
			ParticleEffect.SMOKE_LARGE.display(location, 1, 0, 0, 0, 0.06);
			ParticleEffect.FIREWORKS_SPARK.display(location, 1, 0, 0, 0, 0.06);
			if (i % 2 != 0) {
				if (RegionProtection.isRegionProtected(this, location)) {
					remove();
					return;
				}
				if (rand.nextInt(3) == 0) {
					location.getWorld().playSound(location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 0.01F);
				}
				if (location.getBlock().isLiquid() || !isTransparent(location.getBlock())) {
					createExplosion(location, power, damage);
					return;
				}
				checkDamage();
			}
		}
	}

	private void createExplosion(Location center, double size, double damage) {
		if (hasExploded) return;
		hasExploded = true;
		ParticleEffect.FLAME.display(center, 20, 1, 1, 1, 0.5f, 20);
		ParticleEffect.SMOKE_LARGE.display(center, 20, 1, 1, 1, 0.5f);
		ParticleEffect.FIREWORKS_SPARK.display(center, 20, 1, 1, 1, 0.5f);
		ParticleEffect.SMOKE_LARGE.display(center, 20, 1, 1, 1, 0.5f);
		ParticleEffect.EXPLOSION_HUGE.display(center, 5, 1, 1, 1, 0.5f);
		center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);

		if (regenDelay > 0 && !center.getBlock().isLiquid()) {
			int r = NumberConversions.round(size);
			for (Location l : GeneralMethods.getCircle(center, r, 1, false, true, 0)) {
				if (RegionProtection.isRegionProtected(this, l)) {
					remove();
					return;
				}
				if (l.getBlock().getType().isAir() || MaterialCheck.isUnbreakable(l.getBlock()) || l.getBlock().isLiquid()) {
					continue;
				}
				new TempBlock(l.getBlock(), Material.AIR.createBlockData(), regenDelay + ThreadLocalRandom.current().nextInt(1000));
				if (ThreadLocalRandom.current().nextInt(3) == 0 && l.getBlock().getRelative(BlockFace.DOWN).getType().isSolid()) {
					l.getBlock().setType(Material.FIRE);
				}
			}
		}
		for (Entity e : GeneralMethods.getEntitiesAroundPoint(center, size)) {
			if (e instanceof LivingEntity && !(e instanceof ArmorStand)) {
				if (e instanceof Player && Commands.invincible.contains((e).getName())) {
					continue;
				}
				double factor = 1;
				if (scalingDamageByDistance) {
					double distance = center.distance(e.getLocation());
					double halfSize = size / 2;
					factor = (distance <= halfSize) ? 1 : (distance - halfSize) / size;
				}
				DamageHandler.damageEntity(e, damage * factor, this);
				e.setFireTicks(fireTicks);
				new FireDamageTimer(e, player, this);
			}
		}
		remove();
	}

	private void playParticleRing() {
		for (int i = 0; i < 2; i++) {
			currentRingPoint += 6;
			double x = 1.75 * FastMath.cos(currentRingPoint);
			double z = 1.75 * FastMath.sin(currentRingPoint);
			Location loc = player.getLocation().clone().add(x, 1, z);
			ParticleEffect.FLAME.display(loc, 2, 0, 0, 0, 0.01);
			ParticleEffect.SMOKE_NORMAL.display(loc, 2, 0, 0, 0, 0.01);
		}
	}

	public void checkDamage() {
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 1.6)) {
			if (entity instanceof LivingEntity && entity.getEntityId() != player.getEntityId() && !(entity instanceof ArmorStand)) {
				if (entity instanceof Player && Commands.invincible.contains((entity).getName())) {
					continue;
				}
				createExplosion(entity.getLocation(), power, damage);
				return;
			}
		}
	}

	@Override
	public boolean isEnabled() {
		return Hyperion.getPlugin().getConfig().getBoolean("Abilities.Fire.Combustion.Enabled");
	}

	@Override
	public String getName() {
		return "Combustion";
	}

	@Override
	public String getDescription() {
		return Hyperion.getPlugin().getConfig().getString("Abilities.Fire.Combustion.Description");
	}

	@Override
	public String getAuthor() {
		return Hyperion.getAuthor();
	}

	@Override
	public String getVersion() {
		return Hyperion.getVersion();
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	@Override
	public boolean isSneakAbility() {
		return true;
	}

	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
		return location;
	}

	@Override
	public boolean isCollidable() {
		return launched;
	}

	@Override
	public double getCollisionRadius() {
		return 0.8;
	}

	@Override
	public void handleCollision(Collision collision) {
		if (collision.getAbilitySecond() instanceof Combustion) {
			if (!hasExploded && !((Combustion) collision.getAbilitySecond()).hasExploded) {
				createExplosion(collision.getLocationFirst(), 2 * power, 2 * damage);
				collision.setRemovingSecond(true);
			}
			return;
		} else if (collision.getAbilitySecond().isExplosiveAbility() || collision.getAbilitySecond().getElement().equals(Element.EARTH)) {
			createExplosion(collision.getLocationFirst(), power, damage);
			return;
		}
		super.handleCollision(collision);
	}

	@Override
	public void load() {
	}

	@Override
	public void stop() {
	}

	public static void attemptExplode(Player player) {
		if (hasAbility(player, Combustion.class)) {
			getAbility(player, Combustion.class).attemptExplode();
		}
	}

	private void attemptExplode() {
		createExplosion(location, power, damage); // Allow suicide when combustion isn't launched yet
	}
}
