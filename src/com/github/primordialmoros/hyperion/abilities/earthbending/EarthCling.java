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
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.PassiveAbility;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class EarthCling extends EarthAbility implements PassiveAbility {
	private double speed;

	public EarthCling(Player player) {
		super(player);

		if (!isEnabled()) {
			return;
		}

		speed = Hyperion.getPlugin().getConfig().getDouble("Abilities.Earth.EarthGlove.ClingPassive.Speed");
	}

	@Override
	public void progress() {
		if (!player.isSneaking() || player.isOnGround()) {
			return;
		}

		if (bPlayer.getBoundAbility() == null || !bPlayer.getBoundAbility().getName().equals("EarthGlove")) {
			return;
		}

		if (CoreMethods.isAgainstWall(player, true)) {
			switch (getAbilities(player, EarthGlove.class).size()) {
				case 0:
					player.setVelocity(new Vector());
					break;
				case 1:
					Vector vel = player.getVelocity().clone();
					if (vel.getY() < 0) {
						player.setVelocity(vel.multiply(speed));
						ParticleEffect.CRIT.display(player.getEyeLocation(), 2, 0.05F, 0.4F, 0.05F, 0.1F);
						ParticleEffect.BLOCK_CRACK.display(player.getEyeLocation(), 3, 0.1F, 0.4F, 0.1F, 0.1F, Material.STONE.createBlockData());
					}
					break;
				default:
					break;
			}
		}
	}

	@Override
	public boolean isEnabled() {
		return Hyperion.getPlugin().getConfig().getBoolean("Abilities.Earth.EarthGlove.ClingPassive.Enabled");
	}

	@Override
	public String getName() {
		return "EarthCling";
	}

	@Override
	public String getDescription() {
		return Hyperion.getPlugin().getConfig().getString("Abilities.Earth.EarthGlove.ClingPassive.Description");
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
		return 0;
	}

	@Override
	public Location getLocation() {
		return null;
	}

	@Override
	public boolean isInstantiable() {
		return true;
	}

	@Override
	public boolean isProgressable() {
		return true;
	}
}
