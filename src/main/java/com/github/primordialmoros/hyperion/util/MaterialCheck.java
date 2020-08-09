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

package com.github.primordialmoros.hyperion.util;

import com.projectkorra.projectkorra.ability.ElementalAbility;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.inventory.InventoryHolder;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class MaterialCheck {
	private static final Set<Material> air = EnumSet.of(
		Material.AIR, Material.CAVE_AIR
	);

	private static final Set<Material> leaves = EnumSet.of(
		Material.ACACIA_LEAVES, Material.BIRCH_LEAVES, Material.DARK_OAK_LEAVES, Material.JUNGLE_LEAVES, Material.OAK_LEAVES, Material.SPRUCE_LEAVES
	);

	private static final Set<Material> containers = EnumSet.of(
		Material.CHEST, Material.TRAPPED_CHEST, Material.ENDER_CHEST, Material.BARREL, Material.SHULKER_BOX, Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER,
		Material.DISPENSER, Material.DROPPER, Material.ENCHANTING_TABLE, Material.BREWING_STAND, Material.BEACON, Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL,
		Material.GRINDSTONE, Material.CARTOGRAPHY_TABLE, Material.LOOM, Material.SMITHING_TABLE
	);

	private static final Set<Material> unbreakables = EnumSet.of(
		Material.BARRIER, Material.BEDROCK, Material.OBSIDIAN, Material.NETHER_PORTAL,
		Material.END_PORTAL, Material.END_PORTAL_FRAME
	);

	public static boolean isUnbreakable(Block block) {
		return unbreakables.contains(block.getType()) || (block.getState() instanceof InventoryHolder) || containers.contains(block.getType()) || (block.getState() instanceof CreatureSpawner);
	}

	public static boolean isAir(Block block) {
		return air.contains(block.getType());
	}

	public static boolean isLeaf(Block block) {
		return leaves.contains(block.getType());
	}

	public static Set<Material> getIgnoreMaterialSet() {
		Set<Material> trans = new HashSet<>(ElementalAbility.getTransparentMaterialSet());
		trans.addAll(air);
		return trans;
	}
}
