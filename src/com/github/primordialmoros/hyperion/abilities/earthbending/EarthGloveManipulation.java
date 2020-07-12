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
import com.projectkorra.projectkorra.ability.PassiveAbility;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

public class EarthGloveManipulation extends EarthAbility implements AddonAbility, PassiveAbility {
	public EarthGloveManipulation(Player player) {
		super(player);
	}

	@Override
	public void progress() {
	}

	@Override
	public boolean isEnabled() {
		return Hyperion.getPlugin().getConfig().getBoolean("Abilities.Earth.EarthGlove.Enabled");
	}

	@Override
	public String getName() {
		return "EarthGloveManipulation";
	}

	@Override
	public String getDescription() {
		return "Allows the earthbender to destroy or redirect others' earthgloves.";
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
		return false;
	}

	@Override
	public void load() {
	}

	@Override
	public void stop() {
	}

	public static void attemptDestroy(Player p) {
		if (hasAbility(p, EarthGloveManipulation.class)) {
			getAbility(p, EarthGloveManipulation.class).attemptAction(true);
		}
	}

	public static void attemptRedirect(Player p) {
		if (hasAbility(p, EarthGloveManipulation.class)) {
			getAbility(p, EarthGloveManipulation.class).attemptAction(false);
		}
	}

	private void attemptAction(boolean destroy) {
		if (bPlayer.getBoundAbility() == null || !bPlayer.getBoundAbility().getName().equals("EarthGlove")) {
			return;
		}

		for (Entity targetedEntity : GeneralMethods.getEntitiesAroundPoint(player.getEyeLocation(), 8)) {
			if (targetedEntity instanceof Item && player.hasLineOfSight(targetedEntity) && targetedEntity.hasMetadata(CoreMethods.GLOVE_KEY)) {
				EarthGlove ability = (EarthGlove) targetedEntity.getMetadata(CoreMethods.GLOVE_KEY).get(0).value();
				if (ability != null && !player.equals(ability.getPlayer())) {
					if (destroy) {
						ability.shatterGlove();
					} else {
						ability.redirect(player);
					}
					return;
				}
			}
		}
	}
}
