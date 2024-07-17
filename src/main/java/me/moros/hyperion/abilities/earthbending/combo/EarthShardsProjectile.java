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

package me.moros.hyperion.abilities.earthbending.combo;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import me.moros.hyperion.Hyperion;
import me.moros.hyperion.methods.CoreMethods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class EarthShardsProjectile extends EarthAbility implements AddonAbility {
	private final Location location;
	private final Location origin;
	private final Vector direction;

	@Attribute(Attribute.DAMAGE)
	private final double damage;
	@Attribute(Attribute.RANGE)
	private final int range;

	private double distanceTravelled;

	public EarthShardsProjectile(Player player, Location spawnLocation) {
		super(player);

		damage = Hyperion.getPlugin().getConfig().getDouble("Abilities.Earth.EarthCombo.EarthShards.Damage");
		range = Hyperion.getPlugin().getConfig().getInt("Abilities.Earth.EarthCombo.EarthShards.Range");
		double accuracyDrop = Hyperion.getPlugin().getConfig().getDouble("Abilities.Earth.EarthCombo.EarthShards.AccuracyDrop");

		distanceTravelled = 0;

		origin = spawnLocation.clone();
		final Location targetLocation = GeneralMethods.getTargetedLocation(player, range);
		direction = GeneralMethods.getDirection(origin, CoreMethods.withGaussianOffset(targetLocation, accuracyDrop * targetLocation.distance(origin)));
		location = origin.clone();

		start();
	}

	@Override
	public void progress() {
		if (!bPlayer.canBendIgnoreBindsCooldowns(this)) {
			remove();
			return;
		}
		for (int i = 0; i < 5; i++) {
			location.add(direction.clone().normalize().multiply(0.3));
			distanceTravelled += 0.3;
			if (distanceTravelled > range) {
				remove();
				return;
			}

			Block block = location.getBlock();
			if (GeneralMethods.isSolid(block) || block.isLiquid()) {
				ParticleEffect.BLOCK_CRACK.display(location, 5, 0.1F, 0.1F, 0.1F, 0, Material.STONE.createBlockData());
				remove();
				return;
			}
			ParticleEffect.BLOCK_DUST.display(location, 1, 0, 0, 0, 0, Material.STONE.createBlockData());
			if (i % 2 == 0) {
				checkDamage();
			}
		}
	}

	public void checkDamage() {
		Location check = location.clone().add(0, -0.5, 0);
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(check, 1.5)) {
			if (entity instanceof LivingEntity lEnt && entity.getEntityId() != player.getEntityId() && !(entity instanceof ArmorStand)) {
				if (lEnt.getEyeLocation().distanceSquared(check) > 0.5 && lEnt.getLocation().distanceSquared(check) > 0.5) {
					continue;
				}
				if (entity instanceof Player && Commands.invincible.contains(entity.getName())) {
					continue;
				}

				DamageHandler.damageEntity(lEnt, damage, this);
				lEnt.setNoDamageTicks(0);
				Vector dir = GeneralMethods.getDirection(origin, lEnt.getLocation()).normalize().multiply(0.2);
				lEnt.setVelocity(dir.clone());
				remove();
				return;
			}
		}
	}

	@Override
	public boolean isEnabled() {
		return Hyperion.getPlugin().getConfig().getBoolean("Abilities.Earth.EarthCombo.EarthShards.Enabled");
	}

	@Override
	public String getName() {
		return "EarthShardsProjectile";
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
	public long getCooldown() {
		return 0;
	}

	@Override
	public Location getLocation() {
		return location;
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	@Override
	public boolean isHiddenAbility() {
		return true;
	}

	@Override
	public boolean isSneakAbility() {
		return false;
	}

	@Override
	public void load() {
	}

	@Override
	public void stop() {
	}
}
