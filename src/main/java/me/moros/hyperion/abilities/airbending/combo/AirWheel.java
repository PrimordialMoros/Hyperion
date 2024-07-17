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

package me.moros.hyperion.abilities.airbending.combo;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import com.projectkorra.projectkorra.ability.util.ComboManager;
import com.projectkorra.projectkorra.airbending.AirScooter;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.DamageHandler;
import me.moros.hyperion.Hyperion;
import me.moros.hyperion.methods.CoreMethods;
import me.moros.hyperion.util.FastMath;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AirWheel extends AirAbility implements AddonAbility, ComboAbility {
	private final Map<Entity, Long> affectedEntities = new ConcurrentHashMap<>();
	private final List<Location> wheelLocations = new ArrayList<>();

	private double damage;
	private double knockback;
	private long cooldown;
	private long affectCooldown;

	private double scooterSpeed;

	private int ticks = 0;

	public AirWheel(Player player) {
		super(player);

		if (!hasAbility(player, AirScooter.class) || hasAbility(player, AirWheel.class) || !bPlayer.canBendIgnoreBinds(this)) {
			return;
		}

		scooterSpeed = getAbility(player, AirScooter.class).getSpeed();
		damage = Hyperion.getPlugin().getConfig().getDouble("Abilities.Air.AirCombo.AirWheel.Damage");
		knockback = Hyperion.getPlugin().getConfig().getDouble("Abilities.Air.AirCombo.AirWheel.Knockback");
		cooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Air.AirCombo.AirWheel.Cooldown");
		affectCooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Air.AirCombo.AirWheel.AffectCooldown");

		start();
	}

	@Override
	public void progress() {
		if (!hasAbility(player, AirScooter.class)) {
			remove();
			return;
		}
		ticks++;
		if (ticks % 200 == 0) affectedEntities.keySet().removeIf(Entity::isDead); // Cleanup every 200 ticks
		if (ticks % 2 == 0) return;

		int limit = (ticks % 3 == 0) ? 270 : 90;
		final Location tempLoc = player.getLocation();
		tempLoc.setPitch(0);
		tempLoc.add(0, 0.8, 0).add(tempLoc.getDirection().multiply(2.2 * scooterSpeed));
		wheelLocations.clear();
		for (int i = -90; i <= limit; i += 12) {
			final Location particleLocation = tempLoc.clone();
			particleLocation.add(particleLocation.getDirection().multiply(1.6 * FastMath.cos(i)));
			particleLocation.setY(particleLocation.getY() + (1.6 * FastMath.sin(i)));
			CoreMethods.displayColoredParticle("ffffff", particleLocation, 1, 0, 0, 0, 2f);
		}

		final long time = System.currentTimeMillis();
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(tempLoc, 1.8)) {
			if (entity instanceof LivingEntity && entity.getEntityId() != player.getEntityId() && !(entity instanceof ArmorStand)) {
				if (entity instanceof Player && Commands.invincible.contains((entity).getName())) {
					continue;
				}
				if (affectedEntities.getOrDefault(entity, 0L) > time) continue;
				DamageHandler.damageEntity(entity, damage, this);
				if (entity.isValid()) {
					entity.setVelocity(CoreMethods.calculateFlatVector(tempLoc, entity.getLocation()).multiply(knockback).setY(0.5));
				}
				affectedEntities.put(entity, time + affectCooldown);
			}
		}
	}

	@Override
	public boolean isEnabled() {
		return Hyperion.getPlugin().getConfig().getBoolean("Abilities.Air.AirCombo.AirWheel.Enabled");
	}

	@Override
	public String getName() {
		return "AirWheel";
	}

	@Override
	public String getDescription() {
		return Hyperion.getPlugin().getConfig().getString("Abilities.Air.AirCombo.AirWheel.Description");
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
		return false;
	}

	@Override
	public Object createNewComboInstance(Player player) {
		return new AirWheel(player);
	}

	@Override
	public ArrayList<ComboManager.AbilityInformation> getCombination() {
		ArrayList<ComboManager.AbilityInformation> combination = new ArrayList<>();
		combination.add(new ComboManager.AbilityInformation("AirScooter", ClickType.SHIFT_DOWN));
		combination.add(new ComboManager.AbilityInformation("AirScooter", ClickType.SHIFT_UP));
		combination.add(new ComboManager.AbilityInformation("AirScooter", ClickType.SHIFT_DOWN));
		combination.add(new ComboManager.AbilityInformation("AirScooter", ClickType.SHIFT_UP));
		combination.add(new ComboManager.AbilityInformation("AirScooter", ClickType.LEFT_CLICK));
		return combination;
	}

	@Override
	public String getInstructions() {
		return "AirScooter (Tap Sneak) > AirScooter (Tap Sneak) > AirScooter (Left Click)";
	}

	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
		return player.getLocation();
	}

	@Override
	public List<Location> getLocations() {
		return wheelLocations;
	}

	@Override
	public boolean isCollidable() {
		return true;
	}

	@Override
	public double getCollisionRadius() {
		return 1.6;
	}

	@Override
	public void handleCollision(Collision collision) {
		// TODO ADD STUFF
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
		bPlayer.addCooldown(this);
		super.remove();
	}
}
