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

package me.moros.hyperion.abilities.earthbending.util;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import me.moros.hyperion.Hyperion;
import me.moros.hyperion.util.BendingFallingBlock;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Projectile extends EarthAbility implements AddonAbility {
	private final BendingFallingBlock projectile;

	private Location location;

	private final String name;
	private final double damage;

	public Projectile(CoreAbility source, BendingFallingBlock projectile, double damage) {
		super(source.getPlayer());

		this.name = source.getName();
		this.projectile = projectile;
		this.damage = damage;

		start();
	}

	@Override
	public void progress() {
		if (!bPlayer.canBendIgnoreBindsCooldowns(this)) {
			remove();
			return;
		}
		if (projectile == null || !projectile.getFallingBlock().isValid()) {
			remove();
			return;
		}
		location = projectile.getFallingBlock().getLocation();
		checkDamage();
	}

	public void checkDamage() {
		boolean hit = false;
		Location tempLocation = projectile.getFallingBlock().getLocation().clone().add(0, 0.5, 0);
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(tempLocation, 1.5)) {
			if (entity instanceof LivingEntity && entity.getEntityId() != player.getEntityId() && !(entity instanceof ArmorStand)) {
				if (entity instanceof Player && Commands.invincible.contains((entity).getName())) {
					continue;
				}
				DamageHandler.damageEntity(entity, damage, this);
				((LivingEntity) entity).setNoDamageTicks(0);
				hit = true;
			}
		}
		if (hit) {
			ParticleEffect.BLOCK_CRACK.display(tempLocation, 4, 0.25, 0.125, 0.25, 0, projectile.getFallingBlock().getBlockData());
			ParticleEffect.BLOCK_DUST.display(tempLocation, 6, 0.25, 0.125, 0.25, 0, projectile.getFallingBlock().getBlockData());
			remove();
		}
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public String getName() {
		return projectile == null ? "Projectile" : name;
	}

	@Override
	public String getDescription() {
		return "";
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
	public boolean isHiddenAbility() {
		return true;
	}

	@Override
	public long getCooldown() {
		return 0;
	}

	@Override
	public Location getLocation() {
		return location;
	}

	@Override
	public void load() {
	}

	@Override
	public void stop() {
	}

	@Override
	public void remove() {
		projectile.remove();
		super.remove();
	}
}
