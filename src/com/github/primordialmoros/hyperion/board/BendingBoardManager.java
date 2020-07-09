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

package com.github.primordialmoros.hyperion.board;

import com.github.primordialmoros.hyperion.configuration.ConfigManager;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BendingBoardManager {
	private static final Set<UUID> disabledPlayers = new HashSet<>();
	private static final Map<Player, BendingBoardInstance> scoreboardPlayers = new ConcurrentHashMap<>();

	private static boolean enabled;

	public static void setup() {
		enabled = ConfigManager.boardConfig.getBoolean("Enabled");
		disabledPlayers.clear();
		scoreboardPlayers.clear();
		for (String s : ConfigManager.boardConfig.getStringList("DisabledPlayers")) {
			disabledPlayers.add(UUID.fromString(s));
		}
		for (Player player : Bukkit.getOnlinePlayers()) {
			canUseScoreboard(player);
		}
	}

	public static void reload() {
		for (BendingBoardInstance pBoard : scoreboardPlayers.values()) {
			pBoard.disableScoreboard();
		}
		setup();
	}

	public static void toggleScoreboard(Player player) {
		if (scoreboardPlayers.containsKey(player)) {
			scoreboardPlayers.get(player).disableScoreboard();
			disabledPlayers.add(player.getUniqueId());
			removeInstance(player);
			player.sendMessage(ChatColor.RED + "You have hidden the bending board.");
		} else {
			disabledPlayers.remove(player.getUniqueId());
			canUseScoreboard(player);
			player.sendMessage(ChatColor.GREEN + "You have made the bending board visible again.");
		}
	}

	public static boolean canUseScoreboard(Player player) {
		if (!enabled || disabledPlayers.contains(player.getUniqueId())) {
			return false;
		}
		if (!scoreboardPlayers.containsKey(player)) {
			scoreboardPlayers.put(player, new BendingBoardInstance(player));
		}
		return true;
	}

	public static void updateAllSlots(Player player) {
		if (canUseScoreboard(player)) {
			scoreboardPlayers.get(player).updateAll();
		}
	}

	public static void updateBoard(Player player, String abilityName, boolean cooldown, int slot) {
		if (canUseScoreboard(player)) {
			if (abilityName == null || abilityName.isEmpty()) {
				scoreboardPlayers.get(player).clearSlot(slot);
				return;
			}

			CoreAbility coreAbility = CoreAbility.getAbility(abilityName);
			if (coreAbility != null && ComboManager.getComboAbilities().containsKey(abilityName)) {
				scoreboardPlayers.get(player).updateCombo("  " + coreAbility.getElement().getColor() + ChatColor.STRIKETHROUGH + abilityName, cooldown);
				return;
			}
			scoreboardPlayers.get(player).setAbility(abilityName, cooldown);
		}
	}

	public static void changeActiveSlot(Player player, int oldSlot, int newSlot) {
		if (canUseScoreboard(player)) {
			scoreboardPlayers.get(player).setActiveSlot(++oldSlot, ++newSlot);
		}
	}

	public static void removeInstance(Player player) {
		scoreboardPlayers.remove(player);
	}

	public static void saveChanges() {
		ConfigManager.boardConfig.set("DisabledPlayers", disabledPlayers.stream().map(UUID::toString).collect(Collectors.toList()));
		ConfigManager.saveBoardConfig();
	}
}
