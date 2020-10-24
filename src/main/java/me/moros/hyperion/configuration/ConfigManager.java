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

package me.moros.hyperion.configuration;

import me.moros.hyperion.Hyperion;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

	public static Config modifiersConfig;

	public ConfigManager() {
		setupMainConfig();
		modifiersConfig = new Config("modifiers.yml");
		setupModifiersConfig();
	}

	public void setupModifiersConfig() {
		FileConfiguration config = modifiersConfig.getConfig();

		config.addDefault("AvatarState.Combustion.ChargeTime", 0);
		config.addDefault("AvatarState.Bolt.Cooldown", 0);
		config.addDefault("AvatarState.Bolt.ChargeTime", 0);
		config.addDefault("AvatarState.EarthLine.Damage", 10);
		config.addDefault("AvatarState.IceCrawl.Range", 48);
		config.addDefault("AvatarState.EarthSmash.ChargeTime", 0);
		config.addDefault("AvatarState.EarthSmash.Cooldown", 0);

		config.options().copyDefaults(true);
		modifiersConfig.saveConfig();
	}

	public void setupMainConfig() {
		FileConfiguration config = Hyperion.getPlugin().getConfig();

		config.addDefault("EnableCollisions", true);

		final String[] extras = {
			Material.ANCIENT_DEBRIS.name(),
			Material.NETHERITE_BLOCK.name()
		};

		config.addDefault("ExtraUnbreakableMaterials", extras);

		config.addDefault("Abilities.Air.Evade.Enabled", true);
		config.addDefault("Abilities.Air.Evade.Description", "As an airbender you can manipulate the streams of air around you quickly roll and evade potential attacks. Left click to use, the direction of your evasive maneuver depends on whether you are looking up or down.");
		config.addDefault("Abilities.Air.Evade.Cooldown", 1000);

		config.addDefault("Abilities.Air.AirCombo.AirWheel.Enabled", true);
		config.addDefault("Abilities.Air.AirCombo.AirWheel.Description", "Upgrade your airscooter to an airwheel that deals damage and knocks back nearby entities.");
		config.addDefault("Abilities.Air.AirCombo.AirWheel.Damage", 2.0);
		config.addDefault("Abilities.Air.AirCombo.AirWheel.Knockback", 1.5);
		config.addDefault("Abilities.Air.AirCombo.AirWheel.Cooldown", 15000);
		config.addDefault("Abilities.Air.AirCombo.AirWheel.AffectCooldown", 750);

		config.addDefault("Abilities.Earth.EarthLine.Enabled", true);
		config.addDefault("Abilities.Earth.EarthLine.Description", "Tap sneak while looking at an earthbendable block on the ground and then left click in a direction to launch a narrow line of earth towards your target to deal damage and knock them back. Additionally, you can hold sneak to control the direction of the line. Left clicking will cause spikes to erupt at the line's current location. Right click (you need an item in your offhand for right click to be detected if you are targeting air) instead to imprison your target without dealing damage.");
		config.addDefault("Abilities.Earth.EarthLine.Damage", 3.0);
		config.addDefault("Abilities.Earth.EarthLine.Cooldown", 3000);
		config.addDefault("Abilities.Earth.EarthLine.Range", 24);
		config.addDefault("Abilities.Earth.EarthLine.SelectRange", 6);
		config.addDefault("Abilities.Earth.EarthLine.AllowUnderWater", true);
		config.addDefault("Abilities.Earth.EarthLine.Magma.DamageModifier", 2.0);
		config.addDefault("Abilities.Earth.EarthLine.Magma.RegenDelay", 20000);
		config.addDefault("Abilities.Earth.EarthLine.PrisonCooldown", 15000);
		config.addDefault("Abilities.Earth.EarthLine.PrisonDuration", 3000);
		config.addDefault("Abilities.Earth.EarthLine.PrisonRadius", 0.8);
		config.addDefault("Abilities.Earth.EarthLine.PrisonPoints", 8);

		config.addDefault("Abilities.Earth.EarthShot.Enabled", true);
		config.addDefault("Abilities.Earth.EarthShot.Description", "EarthShot is an offensive earth projectile move. Tap sneak while looking at a nearby earthbendable block and it will ascend to your eye height. Left click to launch your projectile. Once thrown, you can hold sneak to control its movement. If you are a Lavabender you can hold sneak while looking at your raised source to turn it into a more powerful MagmaShot!");
		config.addDefault("Abilities.Earth.EarthShot.Damage", 3.0);
		config.addDefault("Abilities.Earth.EarthShot.Cooldown", 1500);
		config.addDefault("Abilities.Earth.EarthShot.Range", 60);
		config.addDefault("Abilities.Earth.EarthShot.SelectRange", 6);
		config.addDefault("Abilities.Earth.EarthShot.MagmaShot.AllowConvert", true);
		config.addDefault("Abilities.Earth.EarthShot.MagmaShot.DamageModifier", 2.0);
		config.addDefault("Abilities.Earth.EarthShot.MagmaShot.ChargeTime", 1500);

		config.addDefault("Abilities.Earth.EarthGlove.Enabled", true);
		config.addDefault("Abilities.Earth.EarthGlove.Description", "Dai Li agents use this technique for various purposes. Hold sneak and click to launch your glove and attempt to grab your target. If you continue holding sneak, the gloves will attempt to return to you. You can also destroy other players' gloves by tapping sneak while looking at them.");
		config.addDefault("Abilities.Earth.EarthGlove.Damage", 1.0);
		config.addDefault("Abilities.Earth.EarthGlove.Cooldown", 5000);
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

		final String[] materials = {
			Material.COBBLESTONE.name(),
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
		config.addDefault("Abilities.Earth.LavaDisk.Description", "Tap sneak to select a nearby earth or lava source. This disk made of molten earth will destroy any earthbendable and any soft materials it comes in contact with. The closer you are to the LavaDisk the faster it spins and the more damage it deals.");
		config.addDefault("Abilities.Earth.LavaDisk.MaxDamage", 6.0);
		config.addDefault("Abilities.Earth.LavaDisk.MinDamage", 1.0);
		config.addDefault("Abilities.Earth.LavaDisk.Cooldown", 7000);
		config.addDefault("Abilities.Earth.LavaDisk.Range", 24);
		config.addDefault("Abilities.Earth.LavaDisk.RegenDelay", 10000);
		config.addDefault("Abilities.Earth.LavaDisk.PassThroughEntities", true);
		config.addDefault("Abilities.Earth.LavaDisk.AdditionalMeltableBlocks", materials);

		final String[] cableMaterials = {
			Material.IRON_CHESTPLATE.name(),
			Material.IRON_INGOT.name(),
			Material.IRON_BLOCK.name()
		};

		config.addDefault("Abilities.Earth.MetalCable.Enabled", true); // TODO FIX
		config.addDefault("Abilities.Earth.MetalCable.Description", "This incredibly versatile ability is used by Metalbenders for all purposes. Left click to launch a metal cable. If it connects to an entity or a block you will be pulled towards it. If you hold sneak, the entity or block will be pulled towards you instead. You can also left click again to throw any grabbed targets towards the direction you are looking at. Thrown blocks also deal damage. Note, depending on the server config, you might need some form of metal in your inventory to use this ability.");
		config.addDefault("Abilities.Earth.MetalCable.Damage", 4.0);
		config.addDefault("Abilities.Earth.MetalCable.BlockSpeed", 1.4);
		config.addDefault("Abilities.Earth.MetalCable.Cooldown", 5000);
		config.addDefault("Abilities.Earth.MetalCable.Range", 30);
		config.addDefault("Abilities.Earth.MetalCable.RegenDelay", 10000);
		config.addDefault("Abilities.Earth.MetalCable.RequiredItems", cableMaterials);

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
		config.addDefault("Abilities.Fire.Combustion.ChargeTime", 0);
		config.addDefault("Abilities.Fire.Combustion.Power", 3);
		config.addDefault("Abilities.Fire.Combustion.MisfireModifier", -1);
		config.addDefault("Abilities.Fire.Combustion.RegenDelay", 15000);

		config.addDefault("Abilities.Fire.FireCombo.FireWave.Enabled", true);
		config.addDefault("Abilities.Fire.FireCombo.FireWave.Description", "Master Jeong Jeong used this advanced technique to cast a great fire wave that grows in size while it advances forward.");
		config.addDefault("Abilities.Fire.FireCombo.FireWave.Damage", 3.0);
		config.addDefault("Abilities.Fire.FireCombo.FireWave.Cooldown", 20000);
		config.addDefault("Abilities.Fire.FireCombo.FireWave.Range", 16);
		config.addDefault("Abilities.Fire.FireCombo.FireWave.Duration", 5000);
		config.addDefault("Abilities.Fire.FireCombo.FireWave.MoveRate", 250);
		config.addDefault("Abilities.Fire.FireCombo.FireWave.MaxHeight", 6);
		config.addDefault("Abilities.Fire.FireCombo.FireWave.Width", 2);

		config.addDefault("Abilities.Water.IceBreath.Enabled", true);
		config.addDefault("Abilities.Water.IceBreath.Description", "As demonstrated by Katara, a Waterbender is able to freeze their breath. With this ability bound, simply hold sneak to charge and then release a breath of ice that freeze water and nearby entities.");
		config.addDefault("Abilities.Water.IceBreath.Damage", 3.0);
		config.addDefault("Abilities.Water.IceBreath.Range", 5);
		config.addDefault("Abilities.Water.IceBreath.Cooldown", 10000);
		config.addDefault("Abilities.Water.IceBreath.ChargeTime", 1000);
		config.addDefault("Abilities.Water.IceBreath.FrostDuration", 5000);

		config.addDefault("Abilities.Water.IceCrawl.Enabled", true);
		config.addDefault("Abilities.Water.IceCrawl.Description", "Tap sneak at a water or ice source block and then left click in a direction to launch forward a narrow line of ice. Upon colliding with an enemy, it deals damage and freezes the target's feet.");
		config.addDefault("Abilities.Water.IceCrawl.Damage", 5.0);
		config.addDefault("Abilities.Water.IceCrawl.Cooldown", 5000);
		config.addDefault("Abilities.Water.IceCrawl.Range", 24);
		config.addDefault("Abilities.Water.IceCrawl.SelectRange", 8);
		config.addDefault("Abilities.Water.IceCrawl.FreezeDuration", 2000);
		config.addDefault("Abilities.Water.IceCrawl.IceDuration", 8000);

		config.addDefault("Abilities.Water.WaterCombo.IceDrill.Enabled", true);
		config.addDefault("Abilities.Water.WaterCombo.IceDrill.Description", "Given a large enough source of water or ice, a master waterbender is able to form a vast spike of ice capable of drilling through blocks.");
		config.addDefault("Abilities.Water.WaterCombo.IceDrill.Cooldown", 15000);
		config.addDefault("Abilities.Water.WaterCombo.IceDrill.SelectRange", 16);
		config.addDefault("Abilities.Water.WaterCombo.IceDrill.MaxLength", 16);
		config.addDefault("Abilities.Water.WaterCombo.IceDrill.Duration", 10000);
		config.addDefault("Abilities.Water.WaterCombo.IceDrill.RegenDelay", 10000);

		config.options().copyDefaults(true);
		Hyperion.getPlugin().saveConfig();
	}
}
