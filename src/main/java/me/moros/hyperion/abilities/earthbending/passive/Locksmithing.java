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

package me.moros.hyperion.abilities.earthbending.passive;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.MetalAbility;
import com.projectkorra.projectkorra.ability.PassiveAbility;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.ActionBar;
import me.moros.hyperion.Hyperion;
import me.moros.hyperion.util.MaterialCheck;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Lockable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class Locksmithing extends MetalAbility implements AddonAbility, PassiveAbility {
	private static final String OVERRIDE = "bending.admin.overridelock";
	private static final ChatColor[] COLORS = Arrays.stream(ChatColor.values()).filter(ChatColor::isColor).toArray(ChatColor[]::new);
	private static CoreAbility instance;

	private static final Set<Material> METAL_KEYS = Set.of(
			Material.IRON_INGOT, Material.GOLD_INGOT, Material.COPPER_INGOT, Material.NETHERITE_INGOT
	);

	public Locksmithing(Player player) {
		super(player);
	}

	@Override
	public void progress() {
	}

	private static String getOrCreateKey(ItemStack item, ItemMeta meta) {
		String keyName = meta.getDisplayName();
		if (!Hyperion.getLayer().hasLocksmithingKey(meta)) {
			Hyperion.getLayer().addLockSmithingKey(meta);
			ChatColor randomColor = COLORS[ThreadLocalRandom.current().nextInt(COLORS.length)];
			keyName = randomColor + UUID.randomUUID().toString();
			meta.setDisplayName(keyName);
			item.setItemMeta(meta);
		}
		return keyName;
	}

	public static void act(BendingPlayer bPlayer, Block block) {
		Player player = bPlayer.getPlayer();
		PlayerInventory inv = player.getInventory();
		ItemStack key = inv.getItemInMainHand();
		ItemMeta meta = key.getItemMeta();
		if (meta != null && METAL_KEYS.contains(key.getType()) && MaterialCheck.isLockable(block)) {
			if (instance == null) {
				instance = getAbility(Locksmithing.class);
			}
			if (!instance.isEnabled() || !bPlayer.canBendPassive(instance) || !bPlayer.canUsePassive(instance)) {
				return;
			}
			bPlayer.addCooldown(instance, instance.getCooldown());
			if (RegionProtection.isRegionProtected(player, block.getLocation())) {
				return;
			}
			BlockState state = block.getState();
			if (state instanceof Lockable container) {
				Location loc = block.getLocation().add(0.5, 0.5, 0.5);
				if (!player.isSneaking() && !container.isLocked()) {
					String keyName = getOrCreateKey(key, meta);
					container.setLock(keyName);
					state.update();
					block.getWorld().playSound(loc, Sound.BLOCK_CHEST_LOCKED, 1, 1);
					ActionBar.sendActionBar(Element.METAL.getColor() + "Locked", player);
				} else if (player.isSneaking() && (player.hasPermission(OVERRIDE) || validKey(container, meta))) {
					container.setLock(null);
					state.update();
					block.getWorld().playSound(loc, Sound.BLOCK_CHEST_LOCKED, 1, 2);
					ActionBar.sendActionBar(Element.METAL.getColor() + "Unlocked", player);
				}
			}
		}
	}

	public static boolean canBreak(Player player, Lockable container) {
		if (!container.isLocked() || player.hasPermission(OVERRIDE)) {
			return true;
		}
		PlayerInventory inv = player.getInventory();
		return validKey(container, inv.getItemInMainHand().getItemMeta()) || validKey(container, inv.getItemInOffHand().getItemMeta());
	}

	private static boolean validKey(Lockable container, ItemMeta meta) {
		if (meta == null || !Hyperion.getLayer().hasLocksmithingKey(meta)) {
			return false;
		}
		return container.getLock().equals(meta.getDisplayName());
	}


	@Override
	public boolean isInstantiable() {
		return false;
	}

	@Override
	public boolean isProgressable() {
		return false;
	}

	@Override
	public boolean isEnabled() {
		return Hyperion.getPlugin().getConfig().getBoolean("Abilities.Earth.LockSmithing.Enabled");
	}

	@Override
	public String getName() {
		return "LockSmithing";
	}

	@Override
	public String getDescription() {
		return Hyperion.getPlugin().getConfig().getString("Abilities.Earth.LockSmithing.Description");
	}

	@Override
	public String getInstructions() {
		return Hyperion.getPlugin().getConfig().getString("Abilities.Earth.LockSmithing.Instructions");
	}

	@Override
	public String getAuthor() {
		return Hyperion.getAuthor();
	}

	@Override
	public String getVersion() {
		return Hyperion.getVersion();
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	@Override
	public boolean isSneakAbility() {
		return true;
	}

	@Override
	public boolean isHiddenAbility() {
		return false;
	}

	@Override
	public long getCooldown() {
		return Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.LockSmithing.Cooldown");
	}

	@Override
	public Location getLocation() {
		return player.getLocation();
	}

	@Override
	public void load() {
	}

	@Override
	public void stop() {
	}
}
