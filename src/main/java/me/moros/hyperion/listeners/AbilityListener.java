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

package me.moros.hyperion.listeners;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.PKListener;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import me.moros.hyperion.abilities.airbending.Evade;
import me.moros.hyperion.abilities.earthbending.EarthGlove;
import me.moros.hyperion.abilities.earthbending.EarthGuard;
import me.moros.hyperion.abilities.earthbending.EarthGuardWall;
import me.moros.hyperion.abilities.earthbending.EarthLine;
import me.moros.hyperion.abilities.earthbending.EarthShot;
import me.moros.hyperion.abilities.earthbending.LavaDisk;
import me.moros.hyperion.abilities.earthbending.MetalCable;
import me.moros.hyperion.abilities.firebending.Bolt;
import me.moros.hyperion.abilities.firebending.Combustion;
import me.moros.hyperion.abilities.waterbending.IceBreath;
import me.moros.hyperion.abilities.waterbending.IceCrawl;
import me.moros.hyperion.abilities.waterbending.combo.IceDrill;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;

public class AbilityListener implements Listener {

	@EventHandler
	public void onPlayerSneak(final PlayerToggleSneakEvent event) {
		final Player player = event.getPlayer();
		final BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

		if (bPlayer == null) {
			return;
		}
		final CoreAbility coreAbility = bPlayer.getBoundAbility();
		final String abilityName = bPlayer.getBoundAbilityName();

		if (!(coreAbility instanceof AddonAbility)) {
			return;
		}
		if (!player.isSneaking() && bPlayer.canBendIgnoreCooldowns(coreAbility)) {
			if (!bPlayer.canCurrentlyBendWithWeapons()) {
				return;
			}
			if (coreAbility instanceof EarthAbility && bPlayer.isElementToggled(Element.EARTH)) {
				if (abilityName.equalsIgnoreCase("earthline")) {
					new EarthLine(player);
				} else if (abilityName.equalsIgnoreCase("earthshot")) {
					new EarthShot(player);
				} else if (abilityName.equalsIgnoreCase("lavadisk")) {
					new LavaDisk(player);
				} else if (abilityName.equalsIgnoreCase("earthguard")) {
					new EarthGuardWall(player);
				} else if (abilityName.equalsIgnoreCase("earthglove")) {
					EarthGlove.attemptDestroy(player);
				} else if (abilityName.equalsIgnoreCase("metalcable")) {
					MetalCable.attemptDestroy(player);
				}
			} else if (coreAbility instanceof FireAbility && bPlayer.isElementToggled(Element.FIRE)) {
				if (abilityName.equalsIgnoreCase("combustion")) {
					new Combustion(player);
				} else if (abilityName.equalsIgnoreCase("bolt")) {
					new Bolt(player);
				}
			} else if (coreAbility instanceof WaterAbility && bPlayer.isElementToggled(Element.WATER)) {
				if (abilityName.equalsIgnoreCase("icebreath")) {
					new IceBreath(player);
				} else if (abilityName.equalsIgnoreCase("icecrawl")) {
					new IceCrawl(player);
				}
			}
		}
	}

	@EventHandler
	public void onPlayerSwing(final PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
			if (CoreAbility.hasAbility(player, EarthLine.class)) {
				CoreAbility.getAbility(player, EarthLine.class).setPrisonMode();
			}
			return;
		}
		if (event.getHand() != EquipmentSlot.HAND) {
			return;
		}
		final BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		if (bPlayer == null) {
			return;
		} else if (PKListener.getRightClickInteract().contains(player.getUniqueId())) {
			return;
		}
		final CoreAbility coreAbility = bPlayer.getBoundAbility();
		final String abilityName = bPlayer.getBoundAbilityName();

		if (!(coreAbility instanceof AddonAbility)) {
			return;
		}
		if (bPlayer.canBendIgnoreCooldowns(coreAbility)) {
			if (!bPlayer.canCurrentlyBendWithWeapons()) {
				return;
			}
			if (coreAbility instanceof AirAbility && bPlayer.isElementToggled(Element.AIR)) {
				if (abilityName.equalsIgnoreCase("evade")) {
					new Evade(player);
				}
			} else if (coreAbility instanceof EarthAbility && bPlayer.isElementToggled(Element.EARTH)) {
				if (abilityName.equalsIgnoreCase("earthline")) {
					EarthLine.shootLine(player);
				} else if (abilityName.equalsIgnoreCase("earthshot")) {
					EarthShot.throwProjectile(player);
				} else if (abilityName.equalsIgnoreCase("metalcable")) {
					new MetalCable(player);
				} else if (abilityName.equalsIgnoreCase("earthguard")) {
					new EarthGuard(player);
				} else if (abilityName.equalsIgnoreCase("earthglove")) {
					new EarthGlove(player);
				}
			} else if (coreAbility instanceof FireAbility && bPlayer.isElementToggled(Element.FIRE)) {
				if (abilityName.equalsIgnoreCase("combustion")) {
					Combustion.attemptExplode(player);
				}
			} else if (coreAbility instanceof WaterAbility && bPlayer.isElementToggled(Element.WATER)) {
				if (abilityName.equalsIgnoreCase("icecrawl")) {
					IceCrawl.shootLine(player);
					if (player.isSneaking()) {
						IceDrill.setClicked(player);
					}
				}
			}
		}
	}
}
