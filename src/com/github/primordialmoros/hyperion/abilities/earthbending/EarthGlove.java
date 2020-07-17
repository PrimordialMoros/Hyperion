/*
 *   Copyright 2016, 2017, 2020 Moros <https://github.com/PrimordialMoros>
 *
 * 	  This file is part of Hyperion.
 *
 *    Hyperion is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    Hyperion is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with Hyperion.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.primordialmoros.hyperion.abilities.earthbending;

import com.github.primordialmoros.hyperion.Hyperion;
import com.github.primordialmoros.hyperion.methods.CoreMethods;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EarthGlove extends EarthAbility implements AddonAbility {
	private static final Map<UUID, Boolean> side = new ConcurrentHashMap<>();
	private static final double GLOVE_SPEED = 1.2;
	private static final double GLOVE_GRABBED_SPEED = 0.6;

	private LivingEntity grabbedTarget;
	private Vector lastVelocity;
	private Item glove;

	private double damage;
	private long cooldown;
	private int range;

	public boolean returning;
	public boolean grabbed;

	public EarthGlove(Player player) {
		super(player);

		if (getAbilities(player, EarthGlove.class).size() >= 2 || !bPlayer.canBend(this)) {
			return;
		}

		damage = Hyperion.getPlugin().getConfig().getDouble("Abilities.Earth.EarthGlove.Damage");
		cooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.EarthGlove.Cooldown");
		range = Hyperion.getPlugin().getConfig().getInt("Abilities.Earth.EarthGlove.Range");

		if (launchEarthGlove(!side.getOrDefault(player.getUniqueId(), false))) {
			bPlayer.addCooldown(this);
			start();
		}
	}

	public EarthGlove(Player player, Item gloveItem) {
		super(player);

		if (!bPlayer.canBendIgnoreCooldowns(this)) {
			return;
		}

		if (getAbilities(player, EarthGlove.class).size() >= 2) {
			getAbility(player, EarthGlove.class).remove();
		}

		damage = Hyperion.getPlugin().getConfig().getDouble("Abilities.Earth.EarthGlove.Damage");
		cooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.EarthGlove.Cooldown");
		range = Hyperion.getPlugin().getConfig().getInt("Abilities.Earth.EarthGlove.Range");

		gloveItem.removeMetadata(CoreMethods.GLOVE_KEY, Hyperion.getPlugin());
		gloveItem.setMetadata(CoreMethods.GLOVE_KEY, new FixedMetadataValue(Hyperion.getPlugin(), this));
		glove = gloveItem;
		bPlayer.addCooldown(this);

		setGloveVelocity(GeneralMethods.getDirection(GeneralMethods.getMainHandLocation(player), glove.getLocation()).normalize().multiply(GLOVE_SPEED));
		start();
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
			final Vector returnVector = GeneralMethods.getDirection(glove.getLocation(), returnLocation).normalize();
			if (grabbed) {
				if (grabbedTarget == null || !grabbedTarget.isValid() || (grabbedTarget instanceof Player && !((Player) grabbedTarget).isOnline())) {
					shatterGlove();
					return;
				}
				grabbedTarget.setVelocity(returnVector.clone().multiply(GLOVE_GRABBED_SPEED));
				setGloveVelocity(returnVector.clone().multiply(GLOVE_GRABBED_SPEED));
			} else {
				setGloveVelocity(returnVector.clone().multiply(GLOVE_SPEED));
			}
		} else {
			setGloveVelocity(lastVelocity.clone().normalize().multiply(GLOVE_SPEED));
			checkDamage();
		}

		double velocityLimit = (grabbed ? GLOVE_GRABBED_SPEED : GLOVE_SPEED) - 0.2;
		if (glove.isOnGround() || lastVelocity.angle(glove.getVelocity()) > Math.PI / 4 || glove.getVelocity().length() < velocityLimit) {
			shatterGlove();
		}
	}

	private boolean launchEarthGlove(boolean right) {
		if (!player.isSneaking()) return false;

		side.put(player.getUniqueId(), right);
		final Location gloveSpawnLocation = right ? GeneralMethods.getRightSide(player.getLocation(), 0.5) : GeneralMethods.getLeftSide(player.getLocation(), 0.5);
		gloveSpawnLocation.add(0, 0.8, 0);

		final Entity targetedEntity = GeneralMethods.getTargetedEntity(player, range, Collections.singletonList(player));
		final Vector velocityVector;
		if (targetedEntity instanceof LivingEntity) {
			velocityVector = GeneralMethods.getDirection(gloveSpawnLocation, ((LivingEntity) targetedEntity).getEyeLocation());
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
		glove.teleport(grabbedTarget.getEyeLocation());
	}

	public void checkDamage() {
		Location testLocation = glove.getLocation().clone();
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(testLocation, 0.8)) {
			if (entity instanceof LivingEntity && entity.getEntityId() != player.getEntityId() && !(entity instanceof ArmorStand)) {
				if (entity instanceof Player && Commands.invincible.contains(entity.getName())) {
					continue;
				}
				final LivingEntity livingEntity = (LivingEntity) entity;
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

	public void redirect(final Player newOwner) {
		new EarthGlove(newOwner, glove);
		remove();
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
}
