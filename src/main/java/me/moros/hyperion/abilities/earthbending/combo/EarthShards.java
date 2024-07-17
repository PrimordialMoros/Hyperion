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
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.earthbending.Collapse;
import com.projectkorra.projectkorra.util.ClickType;
import me.moros.hyperion.Hyperion;
import me.moros.hyperion.abilities.earthbending.EarthGlove;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class EarthShards extends EarthAbility implements AddonAbility, ComboAbility {
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	private long shotCooldown;
	private long lastShotTime;
	private int shardsLeft;

	public EarthShards(Player player) {
		super(player);

		if (!bPlayer.canBendIgnoreBinds(this)) {
			return;
		}

		cooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.EarthCombo.EarthShards.Cooldown");
		shotCooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.EarthCombo.EarthShards.ShotCooldown");
		int maxShards = Hyperion.getPlugin().getConfig().getInt("Abilities.Earth.EarthCombo.EarthShards.MaxShards");

		shardsLeft = Math.min(maxShards, 10);
		lastShotTime = 0;

		if (hasAbility(player, EarthGlove.class)) {
			getAbility(player, EarthGlove.class).remove();
		}
		if (hasAbility(player, Collapse.class)) {
			getAbility(player, Collapse.class).remove();
		}
		start();
		bPlayer.addCooldown(this);
	}

	@Override
	public void progress() {
		if (!bPlayer.canBendIgnoreBindsCooldowns(this) || !bPlayer.getBoundAbilityName().equalsIgnoreCase("EarthGlove")) {
			remove();
			return;
		}
		if (hasAbility(player, EarthGlove.class)) {
			getAbility(player, EarthGlove.class).remove();
		}
		if (System.currentTimeMillis() < lastShotTime + shotCooldown) {
			return;
		}
		lastShotTime = System.currentTimeMillis();
		for (int i = 0; i < 2; i++) {
			if (shardsLeft < 1) {
				remove();
				return;
			}
			shardsLeft--;
			final Location shardSpawnLocation = (i == 0) ? GeneralMethods.getRightSide(player.getLocation(), 0.5) : GeneralMethods.getLeftSide(player.getLocation(), 0.5);
			shardSpawnLocation.add(0, 0.8, 0);
			new EarthShardsProjectile(player, shardSpawnLocation);
		}
	}

	@Override
	public boolean isEnabled() {
		return Hyperion.getPlugin().getConfig().getBoolean("Abilities.Earth.EarthCombo.EarthShards.Enabled");
	}

	@Override
	public String getName() {
		return "EarthShards";
	}

	@Override
	public String getDescription() {
		return Hyperion.getPlugin().getConfig().getString("Abilities.Earth.EarthCombo.EarthShards.Description");
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
		return player.getLocation();
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
		return new EarthShards(player);
	}

	@Override
	public ArrayList<AbilityInformation> getCombination() {
		ArrayList<AbilityInformation> combination = new ArrayList<>();
		combination.add(new AbilityInformation("EarthGlove", ClickType.SHIFT_DOWN));
		combination.add(new AbilityInformation("Collapse", ClickType.SHIFT_UP));
		combination.add(new AbilityInformation("Collapse", ClickType.SHIFT_DOWN));
		combination.add(new AbilityInformation("EarthGlove", ClickType.SHIFT_UP));
		return combination;
	}

	@Override
	public String getInstructions() {
		return "EarthGlove (Hold Sneak) > Collapse (Release Sneak) > Collapse (Hold Sneak) > EarthGlove (Release Sneak)";
	}

	@Override
	public void load() {
	}

	@Override
	public void stop() {
	}
}
