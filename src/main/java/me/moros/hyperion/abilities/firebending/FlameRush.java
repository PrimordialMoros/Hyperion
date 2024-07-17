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

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.earthbending.EarthSmash;
import com.projectkorra.projectkorra.firebending.util.FireDamageTimer;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import me.moros.hyperion.Hyperion;
import me.moros.hyperion.methods.CoreMethods;
import me.moros.hyperion.util.BendingFallingBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MainHand;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class FlameRush extends FireAbility implements AddonAbility {
	private final Set<Entity> affectedEntities = new HashSet<>();

	// Config
	private long cooldown;
	private double damage;
	private double speed;
	private double range;
	private double knockback;
	private int fireTicks;
	private double chargeFactor;
	private long maxChargeTime;
	private double startingCollisionRadius;

	// Ability
	private boolean charging = true;
	private boolean fullyCharged;
	private FireStream stream;

	public FlameRush(Player player) {
		super(player);
		if (!bPlayer.canBend(this) || hasAbility(player, FlameRush.class)) {
			return;
		}

		setFields();
		start();
	}

	private void setFields() {
		this.cooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Fire.FlameRush.Cooldown");
		this.damage = Hyperion.getPlugin().getConfig().getDouble("Abilities.Fire.FlameRush.Damage");
		this.speed = Hyperion.getPlugin().getConfig().getDouble("Abilities.Fire.FlameRush.Speed");
		this.range = Hyperion.getPlugin().getConfig().getDouble("Abilities.Fire.FlameRush.Range");
		this.chargeFactor = Hyperion.getPlugin().getConfig().getDouble("Abilities.Fire.FlameRush.ChargeFactor");
		this.maxChargeTime = Hyperion.getPlugin().getConfig().getLong("Abilities.Fire.FlameRush.MaxChargeTime");
		this.startingCollisionRadius = Hyperion.getPlugin().getConfig().getDouble("Abilities.Fire.FlameRush.CollisionRadius");
		this.knockback = Hyperion.getPlugin().getConfig().getDouble("Abilities.Fire.FlameRush.Knockback");
		this.fireTicks = Hyperion.getPlugin().getConfig().getInt("Abilities.Fire.FlameRush.FireTicks");
	}

	@Override
	public void progress() {
		if (!bPlayer.canBendIgnoreCooldowns(this)) {
			remove();
			return;
		}

		if (charging) {
			if (player.isSneaking()) {
				Location spawnLoc = getHandLocation();
				playFirebendingParticles(spawnLoc, 1, 0, 0, 0);
				if (System.currentTimeMillis() >= getStartTime() + maxChargeTime) {
					ParticleEffect.SMOKE_LARGE.display(spawnLoc, 1);
				}
			} else {
				launch();
			}
		} else {
			if (!stream.progress()) {
				remove();
			}
		}
	}

	private Location getHandLocation() {
		Location loc = player.getLocation();
		Vector dir = loc.getDirection().multiply(0.4);
		if (player.getMainHand() == MainHand.LEFT) {
			loc = GeneralMethods.getLeftSide(loc, 0.3);
		} else {
			loc = GeneralMethods.getRightSide(loc, 0.3);
		}
		return loc.add(0.0, 1.2, 0.0).add(dir);
	}

	private void launch() {
		long time = System.currentTimeMillis();
		double deltaTime = time - getStartTime();
		double factor = 1;
		if (deltaTime >= maxChargeTime) {
			factor = chargeFactor;
			fullyCharged = true;
		} else if (deltaTime > 0.3 * maxChargeTime) {
			double deltaFactor = (chargeFactor - factor) * deltaTime / maxChargeTime;
			factor += deltaFactor;
		}
		charging = false;
		bPlayer.addCooldown(this);
		stream = new FireStream(player.getLocation().add(0, 1.2, 0), 0.33 * speed, factor);
	}

	private class FireStream {
		private final Location origin;
		private final double factor;
		private final double speed;
		private final double maxRange;

		private Location loc;
		private Vector streamDirection;
		private int currentPoint;
		private double distanceTravelled;
		private double collisionRadius;

		private FireStream(Location origin, double speed, double factor) {
			this.origin = origin;
			this.speed = speed;
			this.factor = factor;
			this.maxRange = range * factor;
			this.loc = origin.clone();
			this.streamDirection = player.getLocation().getDirection().multiply(speed);
		}

		private boolean progress() {
			streamDirection = streamDirection.add(player.getLocation().getDirection().multiply(0.08)).normalize().multiply(speed);
			for (int i = 0; i < 3; i++) {
				render();
				if (ThreadLocalRandom.current().nextInt(3) == 0) {
					loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_AMBIENT, 2, 1);
				}
				checkEntityCollisions();
				if (GeneralMethods.checkDiagonalWall(loc, streamDirection)) {
					return false;
				}
				loc = loc.add(streamDirection);
				distanceTravelled += speed;
				if (loc.distanceSquared(origin) > maxRange * maxRange || RegionProtection.isRegionProtected(FlameRush.this, loc)) {
					return false;
				}
				Block block = loc.getBlock();
				if (block.isLiquid() || !GeneralMethods.isTransparent(block)) {
					return false;
				}
			}
			return true;
		}

		private void render() {
			currentPoint = (currentPoint + 6) % 360;
			double radius = 0.2 * factor + 0.6 * (distanceTravelled / maxRange);
			int amount = NumberConversions.ceil(12 * radius);
			double offset = 0.5 * radius;
			playFirebendingParticles(loc, amount, offset, offset, offset);
			Vector vec = new Vector(radius, radius, radius);
			Vector offsetVector = GeneralMethods.rotateVectorAroundVector(streamDirection, vec, currentPoint);
			Location spiral1 = loc.clone().add(offsetVector);
			Location spiral2 = loc.clone().subtract(offsetVector);
			playFirebendingParticles(spiral1, 1, 0, 0, 0);
			playFirebendingParticles(spiral2, 1, 0, 0, 0);
			ParticleEffect.SMOKE_LARGE.display(spiral1, 1);
			ParticleEffect.SMOKE_LARGE.display(spiral2, 1);
			collisionRadius = startingCollisionRadius + 0.7 * radius;
		}

		private void checkEntityCollisions() {
			GeneralMethods.getEntitiesAroundPoint(loc, collisionRadius).forEach(this::onEntityHit);
		}

		private void onEntityHit(Entity entity) {
			if (entity.getUniqueId().equals(player.getUniqueId()) || Commands.invincible.contains(entity.getName())) {
				return;
			}
			if (entity instanceof LivingEntity living && !(entity instanceof ArmorStand) && affectedEntities.add(living)) {
				DamageHandler.damageEntity(living, player, damage * factor, FlameRush.this);
				if (living.getFireTicks() < fireTicks) {
					living.setFireTicks(fireTicks);
					new FireDamageTimer(living, player, FlameRush.this);
				}
				living.setVelocity(streamDirection.clone().normalize().multiply(knockback));
			}
		}
	}

	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
		return stream == null ? player.getLocation() : stream.loc.clone();
	}

	@Override
	public boolean isEnabled() {
		return Hyperion.getPlugin().getConfig().getBoolean("Abilities.Fire.FlameRush.Enabled");
	}

	@Override
	public String getName() {
		return "FlameRush";
	}

	@Override
	public String getDescription() {
		return Hyperion.getPlugin().getConfig().getString("Abilities.Fire.FlameRush.Description");
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
	public boolean isHiddenAbility() {
		return false;
	}

	@Override
	public boolean isCollidable() {
		return !charging;
	}

	@Override
	public double getCollisionRadius() {
		return stream == null ? startingCollisionRadius : stream.collisionRadius;
	}

	@Override
	public void handleCollision(Collision collision) {
		if (collision.getAbilitySecond() instanceof EarthSmash smash) {
			if (fullyCharged) {
				collision.setRemovingFirst(false);
				Collection<Location> smashBlocks = smash.getBlocks().stream().map(b -> b.getLocation().add(0.5, 0, 0.5))
						.distinct().toList();
				smash.remove();
				if (!smashBlocks.isEmpty()) {
					ProjectKorra.plugin.getServer().getScheduler().runTaskLater(ProjectKorra.plugin, () -> {
						for (Location loc : smashBlocks) {
							Vector vel = CoreMethods.gaussianVector(0.2, 0.1, 0.2);
							new BendingFallingBlock(loc, Material.MAGMA_BLOCK.createBlockData(), vel, this, true, 5000);
						}
					}, 1);
				}
			} else {
				collision.setRemovingSecond(false);
			}
		} else if (collision.getAbilitySecond() instanceof Combustion combustion) {
			Combustion.attemptExplode(combustion.getPlayer());
		} else if (collision.getAbilitySecond() instanceof FlameRush other) {
			double collidedFactor = other.stream.factor;
			if (stream.factor > collidedFactor + 0.1) {
				collision.setRemovingFirst(false);
			}
		}
		super.handleCollision(collision);
	}

	@Override
	public void load() {
	}

	@Override
	public void stop() {
	}
}
