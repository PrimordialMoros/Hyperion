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

package me.moros.hyperion.util;

import me.moros.hyperion.Hyperion;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Set;

public class MaterialCheck {
	private static final Set<Material> LEAVES = EnumSet.of(
			Material.ACACIA_LEAVES, Material.BIRCH_LEAVES, Material.DARK_OAK_LEAVES, Material.JUNGLE_LEAVES,
			Material.OAK_LEAVES, Material.SPRUCE_LEAVES, Material.AZALEA_LEAVES, Material.FLOWERING_AZALEA_LEAVES
	);

	private static final Set<Material> CONTAINERS = EnumSet.of(
			Material.CHEST, Material.TRAPPED_CHEST, Material.ENDER_CHEST, Material.BARREL, Material.SHULKER_BOX, Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER,
			Material.DISPENSER, Material.DROPPER, Material.ENCHANTING_TABLE, Material.BREWING_STAND, Material.BEACON, Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL,
			Material.GRINDSTONE, Material.CARTOGRAPHY_TABLE, Material.LOOM, Material.SMITHING_TABLE, Material.JUKEBOX
	);

	private static final Set<Material> UNBREAKABLES = EnumSet.of(
			Material.BARRIER, Material.BEDROCK, Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.NETHER_PORTAL,
			Material.END_PORTAL, Material.END_PORTAL_FRAME, Material.END_GATEWAY
	);

	private static final Set<Material> LOCKABLE_CONTAINERS = EnumSet.of(
			Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL, Material.SHULKER_BOX,
			Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER, Material.BEACON,
			Material.DISPENSER, Material.DROPPER, Material.HOPPER, Material.BREWING_STAND
	);

	private static final Set<Material> METAL_ARMOR = EnumSet.of(
			Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
			Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
			Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
			Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS
	);

	public static boolean isUnbreakable(Block block) {
		return UNBREAKABLES.contains(block.getType()) || (block.getState() instanceof InventoryHolder) ||
				CONTAINERS.contains(block.getType()) || (block.getState() instanceof CreatureSpawner) ||
				Hyperion.getPlugin().getConfig().getStringList("ExtraUnbreakableMaterials").contains(block.getType().name());
	}

	public static boolean isLeaf(Block block) {
		return LEAVES.contains(block.getType());
	}

	public static boolean isLockable(Block block) {
		return LOCKABLE_CONTAINERS.contains(block.getType());
	}

	public static boolean hasMetalArmor(LivingEntity entity) {
		EntityEquipment equipment = entity.getEquipment();
		if (equipment == null) {
			return false;
		}
		for (ItemStack item : equipment.getArmorContents()) {
			if (item != null && METAL_ARMOR.contains(item.getType())) {
				return true;
			}
		}
		return false;
	}
}
