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

package me.moros.hyperion.configuration;

import me.moros.hyperion.Hyperion;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Config {

	private final Path path;
	private final FileConfiguration config;

	public Config(String name) {
		path = Paths.get(Hyperion.getPlugin().getDataFolder().toString(), name);
		config = YamlConfiguration.loadConfiguration(path.toFile());
		reloadConfig();
	}

	private void createConfig() {
		try {
			Files.createFile(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public FileConfiguration getConfig() {
		return config;
	}

	public void reloadConfig() {
		if (Files.notExists(path)) createConfig();
		try {
			config.load(path.toFile());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void saveConfig() {
		try {
			config.options().copyDefaults(true);
			config.save(path.toFile());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
