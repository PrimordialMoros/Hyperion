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
import com.github.primordialmoros.hyperion.util.TempArmorStand;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.util.MovementHandler;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collections;

public class EarthPrison extends EarthAbility implements AddonAbility {
	private Location location;
	private long cooldown;
	private long duration;
	private int range;
	private LivingEntity target;

	public EarthPrison(Player player) {
		super(player);

		if (!bPlayer.canBend(this)) {
			return;
		}

		cooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.EarthPrison.Cooldown");
		range = Hyperion.getPlugin().getConfig().getInt("Abilities.Earth.EarthPrison.Range");
		duration = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.EarthPrison.Duration");
		double radius = Hyperion.getPlugin().getConfig().getDouble("Abilities.Earth.EarthPrison.Radius");
		int points = Hyperion.getPlugin().getConfig().getInt("Abilities.Earth.EarthPrison.Points");

		if (prepare()) {
			Block blockUnderEntity = target.getLocation().getBlock().getRelative(BlockFace.DOWN);
			Block blockBelow = blockUnderEntity.getRelative(BlockFace.DOWN);
			if (!isEarthbendable(blockUnderEntity) || !isEarthbendable(blockBelow)) {
				return;
			}
			bPlayer.addCooldown(this);
			for (Location loc : CoreMethods.getCirclePoints(location.clone().add(0, -1.05, 0), points, radius, 0)) {
				new TempArmorStand(this, loc.clone(), blockUnderEntity.getType(), duration);
				new TempArmorStand(this, loc.clone().add(0, -0.6, 0), blockBelow.getType(), duration);
			}
			final MovementHandler mh = new MovementHandler(target, CoreAbility.getAbility(EarthPrison.class));
			mh.stopWithDuration(duration / 50, Element.EARTH.getColor() + "* Imprisoned *");
			start();
		}
	}

	@Override
	public void progress() {
		if (!bPlayer.canBendIgnoreBindsCooldowns(this) || target == null || target.isDead() || !target.isValid()) {
			remove();
			return;
		}
		if (System.currentTimeMillis() > getStartTime() + duration) {
			remove();
		}
	}

	public boolean prepare() {
		final Entity e = GeneralMethods.getTargetedEntity(player, range, Collections.singletonList(player));

		if (e instanceof LivingEntity && e.getEntityId() != player.getEntityId() && e.isOnGround() && !GeneralMethods.isRegionProtectedFromBuild(this, e.getLocation())) {
			target = (LivingEntity) e;
			location = target.getLocation().clone();
			return true;
		}
		return false;
	}

	@Override
	public boolean isEnabled() {
		return Hyperion.getPlugin().getConfig().getBoolean("Abilities.Earth.EarthPrison.Enabled");
	}

	@Override
	public String getName() {
		return "EarthPrison";
	}

	@Override
	public String getDescription() {
		return Hyperion.getPlugin().getConfig().getString("Abilities.Earth.EarthPrison.Description");
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
		return cooldown;
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
	public boolean isSneakAbility() {
		return true;
	}

	@Override
	public void load() {
	}

	@Override
	public void stop() {
	}
}
