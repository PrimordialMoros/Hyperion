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

package me.moros.hyperion.abilities.earthbending;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import me.moros.hyperion.Hyperion;
import me.moros.hyperion.methods.CoreMethods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EarthGlove extends EarthAbility implements AddonAbility {
	public enum Side {RIGHT, LEFT}

	private static final Map<UUID, Side> lastUsedSide = new ConcurrentHashMap<>();
	private static final double GLOVE_SPEED = 1.2;
	private static final double GLOVE_GRABBED_SPEED = 0.6;

	private LivingEntity grabbedTarget;
	private Vector lastVelocity;
	private Item glove;

	@Attribute(Attribute.DAMAGE)
	private double damage;
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.RANGE)
	private int range;

	public boolean returning;
	public boolean grabbed;

	public EarthGlove(Player player) {
		super(player);

		if (getAbilities(player, EarthGlove.class).size() >= 2 || !bPlayer.canBendIgnoreCooldowns(this)) {
			return;
		}

		damage = Hyperion.getPlugin().getConfig().getDouble("Abilities.Earth.EarthGlove.Damage");
		cooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.EarthGlove.Cooldown");
		range = Hyperion.getPlugin().getConfig().getInt("Abilities.Earth.EarthGlove.Range");

		if (launchEarthGlove()) {
			start();
		}
	}

	@Override
	public void progress() {
		if (!bPlayer.canBendIgnoreBindsCooldowns(this) || glove == null || !glove.isValid()) {
			remove();
			return;
		}
		if (!glove.getWorld().equals(player.getWorld()) || glove.getLocation().distanceSquared(player.getLocation()) > Math.pow(range + 5, 2)) {
			remove();
			return;
		}
		if (glove.getLocation().distanceSquared(player.getLocation()) > range * range) {
			returning = true;
		}

		Vector vector = lastVelocity.clone(); // Record velocity
		if (returning) {
			if (!player.isSneaking()) {
				shatterGlove();
				return;
			}
			Location returnLocation = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(1.5));
			if (glove.getLocation().distanceSquared(returnLocation) < 1) {
				if (grabbed && grabbedTarget != null) grabbedTarget.setVelocity(new Vector());
				remove();
				return;
			}
			if (grabbed) {
				if (grabbedTarget == null || !grabbedTarget.isValid() || (grabbedTarget instanceof Player && !((Player) grabbedTarget).isOnline())) {
					shatterGlove();
					return;
				}
				grabbedTarget.setVelocity(GeneralMethods.getDirection(grabbedTarget.getLocation(), returnLocation).normalize().multiply(GLOVE_GRABBED_SPEED));
				glove.teleport(grabbedTarget.getEyeLocation().subtract(0, grabbedTarget.getHeight() / 2, 0));
				return;
			} else {
				setGloveVelocity(GeneralMethods.getDirection(glove.getLocation(), returnLocation).normalize().multiply(GLOVE_SPEED));
			}
		} else {
			setGloveVelocity(lastVelocity.clone().normalize().multiply(GLOVE_SPEED));
			checkDamage();
			if (grabbed) {
				Location returnLocation = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(1.5));
				final Vector returnVector = GeneralMethods.getDirection(glove.getLocation(), returnLocation).normalize();
				grabbedTarget.setVelocity(returnVector.clone().multiply(GLOVE_GRABBED_SPEED));
				setGloveVelocity(returnVector.clone().multiply(GLOVE_GRABBED_SPEED));
				return;
			}
		}

		double velocityLimit = (grabbed ? GLOVE_GRABBED_SPEED : GLOVE_SPEED) - 0.2;
		if (glove.isOnGround() || vector.angle(glove.getVelocity()) > Math.PI / 4 || glove.getVelocity().length() < velocityLimit) {
			shatterGlove();
		}
	}

	private boolean launchEarthGlove() {
		final Location gloveSpawnLocation;
		Side side = lastUsedSide.get(player.getUniqueId());
		if (side != null && bPlayer.isOnCooldown(getCooldownForSide(side))) {
			return false;
		}
		side = lastUsedSide.compute(player.getUniqueId(), (u, s) -> (s == null || s == Side.LEFT) ? Side.RIGHT : Side.LEFT);
		bPlayer.addCooldown(getCooldownForSide(side), cooldown);
		if (side == Side.RIGHT) {
			gloveSpawnLocation = GeneralMethods.getRightSide(player.getLocation(), 0.5).add(0, 0.8, 0);
		} else {
			gloveSpawnLocation = GeneralMethods.getLeftSide(player.getLocation(), 0.5).add(0, 0.8, 0);
		}
		final Entity targetedEntity = GeneralMethods.getTargetedEntity(player, range, Collections.singletonList(player));
		final Vector velocityVector;
		if (targetedEntity instanceof LivingEntity) {
			Location targetLoc = ((LivingEntity) targetedEntity).getEyeLocation().subtract(0, targetedEntity.getHeight() / 2, 0);
			velocityVector = GeneralMethods.getDirection(gloveSpawnLocation, targetLoc);
		} else {
			velocityVector = GeneralMethods.getDirection(gloveSpawnLocation, GeneralMethods.getTargetedLocation(player, range));
		}
		glove = buildGlove(gloveSpawnLocation);
		setGloveVelocity(velocityVector.normalize().multiply(GLOVE_SPEED));
		return true;
	}

	private Item buildGlove(Location spawnLocation) {
		final Item item = spawnLocation.getWorld().dropItem(spawnLocation, new ItemStack(Material.STONE, 1));
		item.setGravity(false);
		item.setInvulnerable(true);
		item.setMetadata(CoreMethods.NO_PICKUP_KEY, new FixedMetadataValue(Hyperion.getPlugin(), ""));
		item.setMetadata(CoreMethods.GLOVE_KEY, new FixedMetadataValue(Hyperion.getPlugin(), this));
		return item;
	}

	public void grabTarget(final LivingEntity entity) {
		if (grabbed || grabbedTarget != null || entity == null) {
			return;
		}
		returning = true;
		grabbed = true;
		grabbedTarget = entity;
		glove.teleport(grabbedTarget.getEyeLocation().subtract(0, grabbedTarget.getHeight() / 2, 0));
	}

	public void checkDamage() {
		Location testLocation = glove.getLocation().clone();
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(testLocation, 0.8)) {
			if (entity instanceof LivingEntity livingEntity && entity.getEntityId() != player.getEntityId() && !(entity instanceof ArmorStand)) {
				if (entity instanceof Player && Commands.invincible.contains(entity.getName())) {
					continue;
				}
				if (player.isSneaking()) {
					grabTarget(livingEntity);
					return;
				}
				DamageHandler.damageEntity(livingEntity, damage, this);
				livingEntity.setNoDamageTicks(0);
				livingEntity.setVelocity(new Vector());
				shatterGlove();
				return;
			}
		}
	}

	public void setGloveVelocity(final Vector velocity) {
		glove.setVelocity(velocity.clone());
		lastVelocity = velocity.clone();
	}

	@Override
	public boolean isEnabled() {
		return Hyperion.getPlugin().getConfig().getBoolean("Abilities.Earth.EarthGlove.Enabled");
	}

	@Override
	public String getName() {
		return "EarthGlove";
	}

	@Override
	public String getDescription() {
		return Hyperion.getPlugin().getConfig().getString("Abilities.Earth.EarthGlove.Description");
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
		return (glove == null) ? player.getLocation() : glove.getLocation();
	}

	@Override
	public boolean isCollidable() {
		return true;
	}

	@Override
	public double getCollisionRadius() {
		return 0.6;
	}

	@Override
	public void handleCollision(Collision collision) {
		collision.setRemovingSecond(collision.getAbilitySecond() instanceof EarthGlove);
		super.handleCollision(collision);
	}

	@Override
	public void load() {
	}

	@Override
	public void stop() {
	}

	@Override
	public void remove() {
		if (glove.hasMetadata(CoreMethods.GLOVE_KEY)) {
			EarthGlove ability = (EarthGlove) glove.getMetadata(CoreMethods.GLOVE_KEY).get(0).value();
			if (ability != null && player.equals(ability.getPlayer())) {
				glove.remove();
			}
		}
		super.remove();
	}

	public void shatterGlove() {
		if (!glove.isValid()) {
			return;
		}
		ParticleEffect.BLOCK_CRACK.display(glove.getLocation(), 3, 0, 0, 0, Material.STONE.createBlockData());
		ParticleEffect.BLOCK_DUST.display(glove.getLocation(), 2, 0, 0, 0, Material.STONE.createBlockData());
		glove.remove();
		remove();
	}

	public static void attemptDestroy(final Player player) {
		for (Entity targetedEntity : GeneralMethods.getEntitiesAroundPoint(player.getEyeLocation(), 8)) {
			if (targetedEntity instanceof Item && player.hasLineOfSight(targetedEntity) && targetedEntity.hasMetadata(CoreMethods.GLOVE_KEY)) {
				EarthGlove ability = (EarthGlove) targetedEntity.getMetadata(CoreMethods.GLOVE_KEY).get(0).value();
				if (ability != null && !player.equals(ability.getPlayer())) {
					ability.shatterGlove();
					return;
				}
			}
		}
	}

	public static String getCooldownForSide(Side s) {
		return switch (s) {
			case LEFT -> "EarthGloveLeft";
			case RIGHT -> "EarthGloveRight";
		};
	}
}
