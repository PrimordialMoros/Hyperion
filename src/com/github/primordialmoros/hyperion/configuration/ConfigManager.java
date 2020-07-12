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

package com.github.primordialmoros.hyperion.configuration;

import com.github.primordialmoros.hyperion.Hyperion;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {
	public static FileConfiguration boardConfig;
	public static String boardFileName;

	public ConfigManager() {
		setupMainConfig();

		boardFileName = Hyperion.getPlugin().getDataFolder() + File.separator + "board.yml";
		File boardFile = new File(boardFileName);
		try {
			if (boardFile.createNewFile()) {
				Hyperion.getLog().info("Generating new file for board.yml");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		boardConfig = YamlConfiguration.loadConfiguration(boardFile);
		boardConfig.addDefault("Enabled", true);
		boardConfig.addDefault("DisabledPlayers", new String[]{});
		saveBoardConfig();
	}

	public static void saveBoardConfig() {
		try {
			boardConfig.options().copyDefaults(true);
			boardConfig.save(boardFileName);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setupMainConfig() {
		FileConfiguration config = Hyperion.getPlugin().getConfig();

		config.addDefault("EnableCollisions", true);

		config.addDefault("Abilities.Earth.EarthLine.Enabled", true);
		config.addDefault("Abilities.Earth.EarthLine.Description", "Tap sneak at an earthbendable block on the ground and then left click in a direction to launch a narrow line of earth towards your target to deal damage and knock them back. Additionally, you can hold sneak to control the direction of the line. Left clicking will cause spikes to erupt at the line's current location.");
		config.addDefault("Abilities.Earth.EarthLine.Damage", 3.0);
		config.addDefault("Abilities.Earth.EarthLine.Cooldown", 3000);
		config.addDefault("Abilities.Earth.EarthLine.Range", 24);
		config.addDefault("Abilities.Earth.EarthLine.PrepareRange", 6);

		config.addDefault("Abilities.Earth.EarthShot.Enabled", true);
		config.addDefault("Abilities.Earth.EarthShot.Description", "EarthShot is an offensive earth projectile move. Tap sneak while looking at a nearby earthbendable block and it will ascend to your eye height. Left click to launch your projectile. Once thrown, you can hold sneak to control its movement. If you are a Lavabender you can hold sneak while looking at your raised source to turn it into a more powerful MagmaShot!");
		config.addDefault("Abilities.Earth.EarthShot.Damage", 3.0);
		config.addDefault("Abilities.Earth.EarthShot.Cooldown", 1500);
		config.addDefault("Abilities.Earth.EarthShot.Range", 60);
		config.addDefault("Abilities.Earth.EarthShot.PrepareRange", 6);
		config.addDefault("Abilities.Earth.EarthShot.MagmaShot.Enabled", true);
		config.addDefault("Abilities.Earth.EarthShot.MagmaShot.DamageModifier", 2.0);
		config.addDefault("Abilities.Earth.EarthShot.MagmaShot.PrepareTime", 1500);

		config.addDefault("Abilities.Earth.EarthGlove.Enabled", true);
		config.addDefault("Abilities.Earth.EarthGlove.Description", "Dai Li agents use this technique for various purposes. Hold sneak and click to launch your glove and attempt to grab your target. If you continue holding sneak, the gloves will attempt to return to you. You can also redirect or destroy other players' gloves.");
		config.addDefault("Abilities.Earth.EarthGlove.Damage", 1.0);
		config.addDefault("Abilities.Earth.EarthGlove.Cooldown", 500);
		config.addDefault("Abilities.Earth.EarthGlove.Range", 24);
		config.addDefault("Abilities.Earth.EarthGlove.ClingPassive.Enabled", true);
		config.addDefault("Abilities.Earth.EarthGlove.ClingPassive.Description", "EarthGloves can be used to either slow or stop your descend while opposing a wall made of earthbendable materials. Hold sneak to use.");
		config.addDefault("Abilities.Earth.EarthGlove.ClingPassive.Speed", 0.5);

		config.addDefault("Abilities.Earth.EarthGuard.Enabled", true);
		config.addDefault("Abilities.Earth.EarthGuard.Description", "This ability encases the Earthbender in temporary armor. To use, click on a block that is earthbendable, it will then fly towards you and grant you temporary armor and damage reduction. If you are a metalbender and it is a metal block, then you will get metal armor instead! Tap sneak with this ability bound to raise a small temporary wall to protect yourself from incoming attacks.");
		config.addDefault("Abilities.Earth.EarthGuard.Cooldown", 20000);
		config.addDefault("Abilities.Earth.EarthGuard.Duration", 15000);
		config.addDefault("Abilities.Earth.EarthGuard.SelectRange", 6);
		config.addDefault("Abilities.Earth.EarthGuard.BaseResistance", 2);
		config.addDefault("Abilities.Earth.EarthGuard.MetalResistance", 3);
		config.addDefault("Abilities.Earth.EarthGuard.WallDuration", 2000);
		config.addDefault("Abilities.Earth.EarthGuard.WallCooldown", 3000);

		config.addDefault("Abilities.Earth.EarthPrison.Enabled", true);
		config.addDefault("Abilities.Earth.EarthPrison.Description", "Trap your target inside a prison of earth blocks.");
		config.addDefault("Abilities.Earth.EarthPrison.Cooldown", 15000);
		config.addDefault("Abilities.Earth.EarthPrison.Duration", 3000);
		config.addDefault("Abilities.Earth.EarthPrison.Range", 8);
		config.addDefault("Abilities.Earth.EarthPrison.Radius", 0.8);
		config.addDefault("Abilities.Earth.EarthPrison.Points", 8);

		final String[] meltable = {
			Material.COBBLESTONE.name(),
			Material.ACACIA_LEAVES.name(),
			Material.BIRCH_LEAVES.name(),
			Material.DARK_OAK_LEAVES.name(),
			Material.JUNGLE_LEAVES.name(),
			Material.OAK_LEAVES.name(),
			Material.SPRUCE_LEAVES.name(),
			Material.ACACIA_LOG.name(),
			Material.BIRCH_LOG.name(),
			Material.DARK_OAK_LOG.name(),
			Material.JUNGLE_LOG.name(),
			Material.OAK_LOG.name(),
			Material.SPRUCE_LOG.name(),
			Material.ICE.name(),
			Material.PACKED_ICE.name()
		};
		config.addDefault("Abilities.Earth.LavaDisk.Enabled", true);
		config.addDefault("Abilities.Earth.LavaDisk.Description", "Hold sneak while targeting a lava source block to generate a disk of lava. Release sneak to shoot the disk forward. Tap and hold sneak again to attempt and bring the lava disk back to you.");
		config.addDefault("Abilities.Earth.LavaDisk.Damage", 2.0);
		config.addDefault("Abilities.Earth.LavaDisk.Cooldown", 7000);
		config.addDefault("Abilities.Earth.LavaDisk.Duration", 3000);
		config.addDefault("Abilities.Earth.LavaDisk.Regen", 10000);
		config.addDefault("Abilities.Earth.LavaDisk.RecallLimit", 3);
		config.addDefault("Abilities.Earth.LavaDisk.LavaSourceOnly", false);
		config.addDefault("Abilities.Earth.LavaDisk.PassThroughEntities", true);
		config.addDefault("Abilities.Earth.LavaDisk.BlockDamage", true);
		config.addDefault("Abilities.Earth.LavaDisk.LavaTrail", false);
		config.addDefault("Abilities.Earth.LavaDisk.AdditionalMeltableBlocks", meltable);

		config.addDefault("Abilities.Earth.MetalHook.Enabled", true);
		config.addDefault("Abilities.Earth.MetalHook.Description", "This ability is used by Metalbenders to launch metal grappling hooks, allowing them to easily maneuver terrain. To use this ability, you must have some form of iron in your inventory. Left click to launch a grappling hook, once attached it will automatically pull you towards it. Hold sneak or switch slots to disengage the hook.");
		config.addDefault("Abilities.Earth.MetalHook.Cooldown", 1500);
		config.addDefault("Abilities.Earth.MetalHook.Range", 30);
		config.addDefault("Abilities.Earth.MetalHook.RequireItems", true);

		config.addDefault("Abilities.Earth.EarthCombo.EarthShards.Enabled", true);
		config.addDefault("Abilities.Earth.EarthCombo.EarthShards.Description", "Dai Li agents can disintegrate their earth gloves in multiple shards of rock and throw them at an enemy to damage them.");
		config.addDefault("Abilities.Earth.EarthCombo.EarthShards.Damage", 1.0);
		config.addDefault("Abilities.Earth.EarthCombo.EarthShards.Cooldown", 15000);
		config.addDefault("Abilities.Earth.EarthCombo.EarthShards.ShotCooldown", 100);
		config.addDefault("Abilities.Earth.EarthCombo.EarthShards.Range", 20);
		config.addDefault("Abilities.Earth.EarthCombo.EarthShards.AccuracyDrop", 0.2);
		config.addDefault("Abilities.Earth.EarthCombo.EarthShards.MaxShards", 10);

		config.addDefault("Abilities.Fire.Bolt.Enabled", true);
		config.addDefault("Abilities.Fire.Bolt.Description", "Hold sneak to charge up a lightning bolt. Once charged, release to discharge and strike the targeted location.");
		config.addDefault("Abilities.Fire.Bolt.Damage", 6.0);
		config.addDefault("Abilities.Fire.Bolt.Cooldown", 2000);
		config.addDefault("Abilities.Fire.Bolt.Range", 30);
		config.addDefault("Abilities.Fire.Bolt.ChargeTime", 1500);

		config.addDefault("Abilities.Fire.Combustion.Enabled", true);
		config.addDefault("Abilities.Fire.Combustion.Description", "Hold sneak to focus large amounts of energy into your body. Once charged, release shift to fire a powerful maneuverable Combustion beam Left click to detonate the beam manually.");
		config.addDefault("Abilities.Fire.Combustion.Damage", 6.0);
		config.addDefault("Abilities.Fire.Combustion.Cooldown", 10000);
		config.addDefault("Abilities.Fire.Combustion.Range", 80);
		config.addDefault("Abilities.Fire.Combustion.ChargeTime", 1500);
		config.addDefault("Abilities.Fire.Combustion.Power", 3);
		config.addDefault("Abilities.Fire.Combustion.MisfireModifier", -1);
		config.addDefault("Abilities.Fire.Combustion.RegenBlockTime", 15000);

		config.addDefault("Abilities.Fire.Fireball.Enabled", true);
		config.addDefault("Abilities.Fire.Fireball.Description", "Left click to shoot a maneuverable fireball at your target which also leaves a trail of fire!");
		config.addDefault("Abilities.Fire.Fireball.Damage", 3.0);
		config.addDefault("Abilities.Fire.Fireball.Cooldown", 3000);
		config.addDefault("Abilities.Fire.Fireball.Range", 30);

		config.addDefault("Abilities.Fire.FireCombo.FireWave.Enabled", true);
		config.addDefault("Abilities.Fire.FireCombo.FireWave.Description", "Master Jeong Jeong used this advanced technique to cast a great fire wave that grows in size while it advances forward.");
		config.addDefault("Abilities.Fire.FireCombo.FireWave.Damage", 3.0);
		config.addDefault("Abilities.Fire.FireCombo.FireWave.Cooldown", 20000);
		config.addDefault("Abilities.Fire.FireCombo.FireWave.Range", 16);
		config.addDefault("Abilities.Fire.FireCombo.FireWave.Duration", 5000);
		config.addDefault("Abilities.Fire.FireCombo.FireWave.MoveRate", 250);
		config.addDefault("Abilities.Fire.FireCombo.FireWave.MaxHeight", 6);
		config.addDefault("Abilities.Fire.FireCombo.FireWave.Width", 3);

		config.addDefault("Abilities.Water.FrostBreath.Enabled", true);
		config.addDefault("Abilities.Water.FrostBreath.Description", "As demonstrated by Katara, a Waterbender is able to freeze their breath, causing anything it touches to be frozen! With this ability bound, simply hold sneak to start breathing frost!");
		config.addDefault("Abilities.Water.FrostBreath.Damage", 3.0);
		config.addDefault("Abilities.Water.FrostBreath.Range", 5);
		config.addDefault("Abilities.Water.FrostBreath.Cooldown", 10000);
		config.addDefault("Abilities.Water.FrostBreath.ChargeTime", 1000);
		config.addDefault("Abilities.Water.FrostBreath.FrostDuration", 5000);

		config.addDefault("Abilities.Water.IceCrawl.Enabled", true);
		config.addDefault("Abilities.Water.IceCrawl.Description", "Tap sneak at a water or ice source block and then left click in a direction to launch forward a narrow line of ice. Upon colliding with an enemy, it deals damage and freezes the target's feet.");
		config.addDefault("Abilities.Water.IceCrawl.Damage", 5.0);
		config.addDefault("Abilities.Water.IceCrawl.Cooldown", 5000);
		config.addDefault("Abilities.Water.IceCrawl.Range", 24);
		config.addDefault("Abilities.Water.IceCrawl.PrepareRange", 8);
		config.addDefault("Abilities.Water.IceCrawl.FreezeDuration", 2000);

		config.addDefault("Abilities.Water.WaterCombo.IceDrill.Enabled", true);
		config.addDefault("Abilities.Water.WaterCombo.IceDrill.Description", "Given a large enough source of water or ice, a master waterbender is able to form a vast spike of ice capable of drilling through blocks.");
		config.addDefault("Abilities.Water.WaterCombo.IceDrill.Cooldown", 15000);
		config.addDefault("Abilities.Water.WaterCombo.IceDrill.SourceRange", 16);
		config.addDefault("Abilities.Water.WaterCombo.IceDrill.MaxLength", 16);
		config.addDefault("Abilities.Water.WaterCombo.IceDrill.Duration", 10000);
		config.addDefault("Abilities.Water.WaterCombo.IceDrill.RegenBlockTime", 10000);

		config.options().copyDefaults(true);
		Hyperion.getPlugin().saveConfig();
	}
}
