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


import com.cjcrafter.foliascheduler.FoliaCompatibility;
import com.cjcrafter.foliascheduler.ServerImplementation;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.util.TempBlock;
import com.projectkorra.projectkorra.util.TempFallingBlock;
import me.moros.hyperion.abilities.Elements.FireAbility;
import me.moros.hyperion.commands.HyperionCommand;
import me.moros.hyperion.configuration.ConfigManager;
import me.moros.hyperion.listeners.AbilityListener;
import me.moros.hyperion.listeners.CoreListener;
import me.moros.hyperion.listeners.PlayerJoinListener;
import me.moros.hyperion.methods.CoreMethods;
import me.moros.hyperion.util.*;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.logging.Logger;

import static com.projectkorra.projectkorra.util.TempFallingBlock.get;
import static com.projectkorra.projectkorra.util.TempFallingBlock.manage;

public class Hyperion extends JavaPlugin {
	public static Hyperion plugin;
	private static String author;
	private static String version;
	public static Logger log;
	private static PersistentDataLayer layer;
	public static boolean isFolia;
	public static boolean paper;
	public static boolean luminol;
	public static boolean spigot;
	private PotionEffectAdapter potionEffectAdapter;
	public static ServerImplementation scheduler;
	private static UpdateChecker updateChecker;
    private BukkitAudiences adventure;


	@Override
	public void onEnable() {
		plugin = this;
		scheduler = new FoliaCompatibility(plugin).getServerImplementation();
		log = getLogger();
		version = getDescription().getVersion();
		author = getDescription().getAuthors().get(0);
        this.adventure = BukkitAudiences.create(this);
		try {
			Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
			isFolia = true;
		} catch (ClassNotFoundException ignored) {}

		try {
			Class.forName("com.destroystokyo.paper.PaperConfig");
			paper = true;
		} catch (ClassNotFoundException ignored) {}

		try {
			Class.forName("me.earthme.luminol.api.ThreadedRegion");
			luminol = true;
		} catch (ClassNotFoundException ignored) {}

		if (!isLuminol() && isPaper()) {
			getLogger().info("Hyperion is running on Paper/Folia");
		}

		if (isLuminol() && !spigot) {
			getLogger().info("Hyperion is running on Luminol");
		}

		if (!isFolia && !paper && !luminol) {
			spigot = true;
			getLogger().info("Hyperion is running on Spigot");
		}

		new Metrics(this, 8212);
		new ConfigManager();
		new HyperionCommand();
		new Elements();
		updateChecker = new UpdateChecker(this, "Hihelloy-main/Hyperion");

        ThreadUtil.runAsync(() -> updateChecker.checkForUpdate());
		getLogger().info("Initialized Hyperion Elements/Configs/Commands/Metrics/UpdateChecker");
		getLogger().info("Attempting to load PaperLib");
		new PaperLib();
		layer = new PersistentDataLayer();
		checkMaintainer();
		CoreMethods.loadAbilities();

		getServer().getPluginManager().registerEvents(new AbilityListener(), this);
		getServer().getPluginManager().registerEvents(new CoreListener(), this);
		getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);

        ThreadUtil.runGlobalTimer(() -> {
            manage();
        }, 1L, 5L);

        ThreadUtil.runGlobalTimer(() -> {
            TempArmorStand.manage();
        }, 1L, 1L);

        ThreadUtil.runGlobalTimer(() -> {
            BendingFallingBlock.manage();
        }, 1L, 5L);

        ThreadUtil.runGlobalTimer(() -> {
            FireAbility.getAbilities();
        }, 1L, 5L);


		PotionEffectAdapterFactory potionEffectAdapterFactory = new PotionEffectAdapterFactory();
		potionEffectAdapter = potionEffectAdapterFactory.getAdapter();

		if (author.contains("Hihelloy (Maintainer)")) {
			getLogger().info("Hihelloy is the current maintainer of Hyperion");
		} else {
			getLogger().warning("Hihelloy is the current maintainer of Hyperion, but the plugin had trouble loading that D:");
		}
	}

	@Override
	public void onDisable() {
		ThreadUtil.shutdown();
		TempFallingBlock.removeAllFallingBlocks();
		TempBlock.removeAll();
		BendingFallingBlock.removeAll();
		TempArmorStand.removeAll();

		if (!isFolia && !luminol) {
			getServer().getScheduler().cancelTasks(this);
		}

		if (isFolia || luminol) {
			scheduler.global().cancelTasks();
            scheduler.async().cancelTasks();
            scheduler.cancelTasks();
		}

        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
	}

	public static void reload() {
		Hyperion.getPlugin().reloadConfig();
		ConfigManager.modifiersConfig.reloadConfig();
		BendingFallingBlock.removeAll();
		TempArmorStand.removeAll();
		CoreMethods.loadAbilities();
		getLog().info("Trying to initialize commands once more");
		try {
			new HyperionCommand();
		} catch (Exception e) {
			e.printStackTrace();
		}
		getLog().info("Hyperion Reloaded.");
	}

	public static void checkMaintainer() {
		if (!author.contains("Hihelloy (Maintainer)")) {
			author = author + ", Hihelloy (Maintainer)";
		}
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

	public PotionEffectAdapter getPotionEffectAdapter() {
		return this.potionEffectAdapter;
	}

	public static boolean isFolia() {
		return isFolia;
	}

	public static boolean isPaper() {
		return paper;
	}

	public static boolean isLuminol() {
		return luminol;
	}

	public static boolean isSpigot() {
		return spigot;
	}

	public static ServerImplementation getScheduler() {
		return scheduler;
	}

	public static UpdateChecker getUpdateChecker() {
        return updateChecker;
	}

    @NotNull
    public BukkitAudiences adventure() {
        if (this.adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return this.adventure;
    }

}
