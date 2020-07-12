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

package com.github.primordialmoros.hyperion.abilities.firebending;

import com.github.primordialmoros.hyperion.Hyperion;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.firebending.BlazeArc;
import com.projectkorra.projectkorra.firebending.util.FireDamageTimer;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class Fireball extends FireAbility implements AddonAbility {
	private Location location;
	private double distanceTravelled;
	private long range;
	private long cooldown;
	private double damage;

	public Fireball(Player player) {
		super(player);

		if (isWater(player.getEyeLocation().getBlock()) || !bPlayer.canBend(this)) {
			return;
		}

		damage = Hyperion.getPlugin().getConfig().getDouble("Abilities.Fire.Fireball.Damage");
		cooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Fire.Fireball.Cooldown");
		range = Hyperion.getPlugin().getConfig().getLong("Abilities.Fire.Fireball.Range");

		location = player.getEyeLocation();

		damage = getDayFactor(damage, player.getWorld());
		range = (int) getDayFactor(range, player.getWorld());

		bPlayer.addCooldown(this);
		start();
	}

	@Override
	public void progress() {
		if (!bPlayer.canBendIgnoreBindsCooldowns(this)) {
			remove();
			return;
		}
		for (int i = 0; i < 4; i++) {
			distanceTravelled += 0.4;
			if (distanceTravelled > range) {
				remove();
				return;
			}

			final Vector direction = player.getEyeLocation().getDirection();
			location.add(direction.multiply(0.4));
			if (ThreadLocalRandom.current().nextInt(5) == 0) {
				playFirebendingSound(location);
			}

			if (location.getBlock().isLiquid() || !isTransparent(location.getBlock())) {
				remove();
				return;
			}

			ParticleEffect.SMOKE_NORMAL.display(location, 2, ThreadLocalRandom.current().nextFloat() / 4, ThreadLocalRandom.current().nextFloat() / 4, ThreadLocalRandom.current().nextFloat() / 4);
			playFirebendingParticles(location, 5, ThreadLocalRandom.current().nextFloat() / 4, ThreadLocalRandom.current().nextFloat() / 4, ThreadLocalRandom.current().nextFloat() / 4);

			if (distanceTravelled > 2) new BlazeArc(player, location, direction, 2);

			if (i % 2 == 0) {
				if (GeneralMethods.isRegionProtectedFromBuild(this, location)) {
					remove();
					return;
				}
				checkDamage();
			}
		}
	}

	public void checkDamage() {
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 1.5)) {
			if (entity instanceof LivingEntity && entity.getEntityId() != player.getEntityId() && !(entity instanceof ArmorStand)) {
				if (entity instanceof Player && Commands.invincible.contains(entity.getName())) {
					continue;
				}
				DamageHandler.damageEntity(entity, damage, this);
				entity.setFireTicks(30);
				new FireDamageTimer(entity, player);
				remove();
				return;
			}
		}
	}

	@Override
	public boolean isEnabled() {
		return Hyperion.getPlugin().getConfig().getBoolean("Abilities.Fire.Fireball.Enabled");
	}

	@Override
	public String getName() {
		return "Fireball";
	}

	@Override
	public String getDescription() {
		return Hyperion.getPlugin().getConfig().getString("Abilities.Fire.Fireball.Description");
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
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
		return location;
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
	public void load() {
	}

	@Override
	public void stop() {
	}
}
