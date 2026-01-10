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
import me.moros.hyperion.util.HexColor;
import me.moros.hyperion.util.ThreadUtil;
import me.moros.hyperion.util.UpdateChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.units.qual.C;
import org.jetbrains.annotations.NotNull;

import java.util.List;



public class HyperionCommand extends PKCommand {
	public HyperionCommand() {
		super("hyperion", "/bending hyperion <reload>", "Show information about Hyperion and optionally reload its config.", new String[]{"hyperion"});
	}

	@Override
	public void execute(CommandSender sender, List<String> args) {
		if (!hasPermission(sender) || !correctLength(sender, args.size(), 0, 1)) return;

		if (args.isEmpty()) {
			Hyperion.plugin.adventure().sender(sender).sendMessage(Component.text("Hyperion Version: ", HexColor.GREEN).append(Component.text(Hyperion.getVersion(), NamedTextColor.RED)));
			Hyperion.plugin.adventure().sender(sender).sendMessage(Component.text("Developed by: ", NamedTextColor.GREEN).append(Component.text(Hyperion.getAuthor(), NamedTextColor.RED)));
		} else if (args.size() == 1) {
			String sub = args.get(0).toLowerCase();

			if (sub.equals("reload") && hasPermission(sender, "reload")) {
                ThreadUtil.runGlobal(Hyperion::reload);
				Hyperion.plugin.adventure().sender(sender).sendMessage(Component.text("Hyperion config has been reloaded.", NamedTextColor.GREEN));
			}


			else if (sub.equals("checkupdate") && hasPermission(sender, "checkupdate")) {
				UpdateChecker checker = Hyperion.getUpdateChecker();

				if (checker == null) {
					Hyperion.plugin.adventure().sender(sender).sendMessage(Component.text("Update checker not initialized.", NamedTextColor.RED));
					return;
				}

                if (!checker.hasChecked()) {
					Hyperion.plugin.adventure().sender(sender).sendMessage(Component.text("Still checking for updates, please try again shortly.", NamedTextColor.GRAY));
					return;
				}

				if (checker.isUpdateAvailable()) {
					String current = checker.getCurrentVersion() != null ? checker.getCurrentVersion() : "unknown";
					String latest = checker.getLatestVersion() != null ? checker.getLatestVersion() : "unknown";

					Hyperion.plugin.adventure().sender(sender).sendMessage(Component.text("[Hyperion] " + " A new version is available!", HexColor.ORANGE));
					Hyperion.plugin.adventure().sender(sender).sendMessage(Component.text("You're running: " + current, HexColor.RED));
					Hyperion.plugin.adventure().sender(sender).sendMessage(Component.text("Latest version: ", NamedTextColor.GRAY).append(Component.text(latest, HexColor.GREEN)));
					Hyperion.plugin.adventure().sender(sender).sendMessage(Component.text("Download: ", NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED).append(Component.text("https://github.com/Hihelloy-main/Hyperion", NamedTextColor.BLUE).decorate(TextDecoration.UNDERLINED).clickEvent(ClickEvent.openUrl("https://github.com/Hihelloy-main/Hyperion"))));
				} else {
					Hyperion.plugin.adventure().sender(sender).sendMessage(Component.text("You're running the latest version of Hyperion.", HexColor.GREEN));
				}
			}
		}
	}

}
