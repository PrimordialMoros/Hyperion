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

package me.moros.hyperion.commands;

import com.projectkorra.projectkorra.command.PKCommand;
import me.moros.hyperion.Hyperion;
import me.moros.hyperion.configuration.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class HyperionCommand extends PKCommand {
	public HyperionCommand() {
		super("hyperion", "/bending hyperion <reload>", "Show information about Hyperion and optionally reload its config.", new String[]{"hyperion"});
	}

	@Override
	public void execute(CommandSender sender, List<String> args) {
		if (!hasPermission(sender) || !correctLength(sender, args.size(), 0, 1)) return;
		if (args.size() == 0) {
			sender.sendMessage(ChatColor.GREEN + "Hyperion Version: " + ChatColor.RED + Hyperion.getVersion());
			sender.sendMessage(ChatColor.GREEN + "Developed by: " + ChatColor.RED + Hyperion.getAuthor());
		} else if (args.size() == 1) {
			if (args.get(0).equals("reload") && hasPermission(sender, "reload")) {
				Hyperion.getPlugin().reloadConfig();
				ConfigManager.modifiersConfig.reloadConfig();
				sender.sendMessage(ChatColor.GREEN + "Hyperion config has been reloaded.");
			}
		}
	}
}
