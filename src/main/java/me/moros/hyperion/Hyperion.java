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

package me.moros.hyperion;

import me.moros.hyperion.commands.HyperionCommand;
import me.moros.hyperion.configuration.ConfigManager;
import me.moros.hyperion.listeners.AbilityListener;
import me.moros.hyperion.listeners.CoreListener;
import me.moros.hyperion.methods.CoreMethods;
import me.moros.hyperion.util.BendingFallingBlock;
import me.moros.hyperion.util.TempArmorStand;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class Hyperion extends JavaPlugin {
	private static Hyperion plugin;
	private static String author;
	private static String version;
	private static Logger log;
	private static PersistentDataLayer layer;

	@Override
	public void onEnable() {
		plugin = this;
		log = getLogger();
		version = getDescription().getVersion();
		author = getDescription().getAuthors().get(0);

		new Metrics(this, 8212);
		new ConfigManager();
		new HyperionCommand();
		layer = new PersistentDataLayer();
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
		ConfigManager.modifiersConfig.reloadConfig();
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

	public static PersistentDataLayer getLayer() {
		return layer;
	}
}
