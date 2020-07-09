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

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;

public class BendingBoardInstance {
	private final String[] cachedSlots = new String[10];
	private final Set<String> combos = new HashSet<>();

	private final BendingPlayer bendingPlayer;

	private final Scoreboard bendingBoard;
	private final Objective bendingSlots;
	private int selectedSlot;

	public BendingBoardInstance(final Player player) {
		bendingPlayer = BendingPlayer.getBendingPlayer(player);
		selectedSlot = player.getInventory().getHeldItemSlot() + 1;

		bendingBoard = Bukkit.getScoreboardManager().getNewScoreboard();
		bendingSlots = bendingBoard.registerNewObjective("Board Slots", "dummy", ChatColor.BOLD + "Slots");
		bendingSlots.setDisplaySlot(DisplaySlot.SIDEBAR);
		player.setScoreboard(bendingBoard);

		Arrays.fill(cachedSlots, "--");
		Map<Integer, String> abilities = new HashMap<>(bendingPlayer.getAbilities());
		for (int i = 1; i <= 9; i++) {
			setSlot(i, abilities.get(i), false);
		}
	}

	public void disableScoreboard() {
		bendingBoard.clearSlot(DisplaySlot.SIDEBAR);
		bendingSlots.unregister();
		bendingPlayer.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
	}

	private void setSlot(int slot, String name, boolean cooldown) {
		if (slot < 1 || slot > 9 || !bendingPlayer.getPlayer().getScoreboard().equals(bendingBoard)) return;
		StringBuilder sb = new StringBuilder(slot == selectedSlot ? ">" : "  ");
		if (name == null || name.isEmpty()) {
			sb.append(ChatColor.GRAY).append("-- Slot ").append(slot).append(" --");
		} else {
			CoreAbility coreAbility = CoreAbility.getAbility(name);
			if (coreAbility == null || coreAbility instanceof ComboAbility) return;

			sb.append(coreAbility.getElement().getColor());
			if (cooldown || bendingPlayer.isOnCooldown(coreAbility)) sb.append(ChatColor.STRIKETHROUGH);
			sb.append(coreAbility.getName());
		}
		sb.append(ChatColor.RESET.toString().repeat(slot));

		if (!cachedSlots[slot].equals(sb.toString())) {
			bendingBoard.resetScores(cachedSlots[slot]);
		}
		cachedSlots[slot] = sb.toString();
		bendingSlots.getScore(sb.toString()).setScore(-slot);
	}

	public void updateAll() {
		final HashMap<Integer, String> boundAbilities = bendingPlayer.getAbilities();
		for (int i = 1; i < 10; i++) {
			setSlot(i, boundAbilities.getOrDefault(i, ""), false);
		}
	}

	public void clearSlot(int slot) {
		setSlot(slot, null, false);
	}

	public void setActiveSlot(int oldSlot, int newSlot) {
		selectedSlot = newSlot;
		setSlot(oldSlot, bendingPlayer.getAbilities().get(oldSlot), false);
		setSlot(newSlot, bendingPlayer.getAbilities().get(newSlot), false);
	}

	public void setAbility(String name, boolean cooldown) {
		final Map<Integer, String> boundAbilities = bendingPlayer.getAbilities();
		boundAbilities.keySet().stream().filter(key -> name.equals(boundAbilities.get(key))).forEach(slot -> setSlot(slot, name, cooldown));
	}

	public void updateCombo(String text, boolean show) {
		if (show) {
			if (combos.isEmpty()) {
				bendingSlots.getScore("  -----------  ").setScore(-10);
			}
			combos.add(text);
			bendingSlots.getScore(text).setScore(-10);

		} else {
			combos.remove(text);
			bendingBoard.resetScores(text);
			if (combos.isEmpty()) {
				bendingBoard.resetScores("  -----------  ");
			}
		}
	}
}
