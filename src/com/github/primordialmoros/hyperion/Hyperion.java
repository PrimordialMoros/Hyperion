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

package com.github.primordialmoros.hyperion;

import com.github.primordialmoros.hyperion.commands.HyperionCommand;
import com.github.primordialmoros.hyperion.configuration.ConfigManager;
import com.github.primordialmoros.hyperion.listeners.AbilityListener;
import com.github.primordialmoros.hyperion.listeners.CoreListener;
import com.github.primordialmoros.hyperion.methods.CoreMethods;
import com.github.primordialmoros.hyperion.util.BendingFallingBlock;
import com.github.primordialmoros.hyperion.util.MetricsLite;
import com.github.primordialmoros.hyperion.util.TempArmorStand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class Hyperion extends JavaPlugin {
	private static Hyperion plugin;
	private static String author;
	private static String version;
	private static Logger log;

	@Override
	public void onEnable() {
		plugin = this;
		log = getLogger();
		version = getDescription().getVersion();
		author = getDescription().getAuthors().get(0);

		new MetricsLite(this, 8212);
		new ConfigManager();
		new HyperionCommand();
		CoreMethods.loadAbilities();

		getServer().getPluginManager().registerEvents(new AbilityListener(), this);
		getServer().getPluginManager().registerEvents(new CoreListener(), this);
		getServer().getScheduler().scheduleSyncRepeatingTask(this, TempArmorStand::manage, 0, 1);
		getServer().getScheduler().scheduleSyncRepeatingTask(this, BendingFallingBlock::manage, 0, 5);
	}

	@Override
	public void onDisable() {
		BendingFallingBlock.removeAll();
		TempArmorStand.removeAll();
		getServer().getScheduler().cancelTasks(this);
	}

	public static void reload() {
		Hyperion.getPlugin().reloadConfig();
		BendingFallingBlock.removeAll();
		TempArmorStand.removeAll();
		CoreMethods.loadAbilities();
		getLog().info("Hyperion Reloaded.");
	}

	public static Hyperion getPlugin() {
		return plugin;
	}

	public static String getAuthor() {
		return author;
	}

	public static String getVersion() {
		return version;
	}

	public static Logger getLog() {
		return log;
	}
}
