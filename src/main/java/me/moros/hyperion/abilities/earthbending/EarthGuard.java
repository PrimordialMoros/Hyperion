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

package me.moros.hyperion.abilities.earthbending;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.earthbending.EarthArmor;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import com.projectkorra.projectkorra.util.TempPotionEffect;
import me.moros.hyperion.Hyperion;
import me.moros.hyperion.util.BendingFallingBlock;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class EarthGuard extends EarthAbility implements AddonAbility {
	private final List<ItemStack> oldArmor = new ArrayList<>(4);
	private final List<ItemStack> newArmor = new ArrayList<>(4);
	private BendingFallingBlock armorFallingBlock;
	private BlockData blockData;
	private GameMode originalMode;

	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.DURATION)
	private long duration;
	@Attribute(Attribute.SELECT_RANGE)
	private int selectRange;
	@Attribute("Resistance")
	private int resistance;

	private boolean formed;
	private boolean metal;
	private boolean gold;
	private long time;

	public EarthGuard(Player player) {
		super(player);

		if (hasAbility(player, EarthGuard.class) || !bPlayer.canBend(this)) {
			return;
		}

		cooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.EarthGuard.Cooldown");
		duration = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.EarthGuard.Duration");
		selectRange = Hyperion.getPlugin().getConfig().getInt("Abilities.Earth.EarthGuard.SelectRange");

		formed = false;
		metal = false;
		gold = false;

		final Block sourceBlock = getTargetEarthBlock(selectRange);
		if (isEarthbendable(sourceBlock)) {
			blockData = sourceBlock.getBlockData().clone();
			if (isMetal(sourceBlock)) {
				metal = true;
				if (sourceBlock.getType().equals(Material.GOLD_BLOCK)) {
					gold = true;
				}
				resistance = Hyperion.getPlugin().getConfig().getInt("Abilities.Earth.EarthGuard.MetalResistance") - 1;
				playMetalbendingSound(sourceBlock.getLocation());
			} else {
				resistance = Hyperion.getPlugin().getConfig().getInt("Abilities.Earth.EarthGuard.BaseResistance") - 1;
				playEarthbendingSound(sourceBlock.getLocation());
			}
			new TempBlock(sourceBlock, Material.AIR.createBlockData(), ThreadLocalRandom.current().nextInt(2500, 5000) + duration);
			armorFallingBlock = new BendingFallingBlock(sourceBlock.getLocation().add(0.5, 0, 0.5), blockData, new Vector(0, 0.2, 0), this, false);
			start();
			bPlayer.addCooldown(this);
		}
	}

	@Override
	public void progress() {
		if (!formed) {
			if (!bPlayer.canBendIgnoreBindsCooldowns(this)) {
				remove();
				return;
			}
			moveBlock();
		} else {
			if (!canRemainActive()) {
				remove();
				return;
			}
			if (System.currentTimeMillis() > time + duration) {
				player.getLocation().getWorld().playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 2, 1);
				ParticleEffect.BLOCK_CRACK.display(player.getEyeLocation(), 8, 0.1, 0.1, 0.1, blockData);
				remove();
				return;
			}
			player.setFireTicks(0);
		}
	}

	private boolean canRemainActive() {
		final List<String> disabledWorlds = getConfig().getStringList("Properties.DisabledWorlds");
		if (!this.player.isOnline() || this.player.isDead()) {
			return false;
		} else if (!this.bPlayer.canBind(this)) {
			return false;
		} else if (this.getPlayer() != null && this.getLocation() != null && !this.getLocation().getWorld().equals(this.player.getWorld())) {
			return false;
		} else if (disabledWorlds.contains(this.player.getWorld().getName())) {
			return false;
		} else if (Commands.isToggledForAll || !this.bPlayer.isToggled() || !this.bPlayer.isElementToggled(this.getElement())) {
			return false;
		} else if (this.player.getGameMode() == GameMode.SPECTATOR) {
			return false;
		} else if (RegionProtection.isRegionProtected(this.player, this.player.getLocation(), this.getName())) {
			return false;
		}
		return true;
	}

	private void formArmor(Material material) {
		if (formed) return;

		final ItemStack head, chest, leggings, boots;
		Color color = Color.fromRGB(EarthArmor.getColor(material));
		if (metal) {
			if (gold) {
				head = new ItemStack(Material.GOLDEN_HELMET, 1);
				chest = new ItemStack(Material.GOLDEN_CHESTPLATE, 1);
				leggings = new ItemStack(Material.GOLDEN_LEGGINGS, 1);
				boots = new ItemStack(Material.GOLDEN_BOOTS, 1);
			} else {
				head = new ItemStack(Material.IRON_HELMET, 1);
				chest = new ItemStack(Material.IRON_CHESTPLATE, 1);
				leggings = new ItemStack(Material.IRON_LEGGINGS, 1);
				boots = new ItemStack(Material.IRON_BOOTS, 1);
			}
		} else {
			head = new ItemStack(Material.LEATHER_HELMET, 1);
			chest = new ItemStack(Material.LEATHER_CHESTPLATE, 1);
			leggings = new ItemStack(Material.LEATHER_LEGGINGS, 1);
			boots = new ItemStack(Material.LEATHER_BOOTS, 1);
		}

		newArmor.add(boots);
		newArmor.add(leggings);
		newArmor.add(chest);
		newArmor.add(head);

		for (ItemStack item : newArmor) {
			ItemMeta generalMeta = item.getItemMeta();
			generalMeta.setUnbreakable(true);
			if (generalMeta instanceof LeatherArmorMeta meta) {
				meta.setColor(color);
			}
			generalMeta.setDisplayName(ChatColor.GREEN + "Earth Guard Armor");
			generalMeta.setLore(Collections.singletonList(ChatColor.DARK_GREEN + "Temporary"));
			Hyperion.getLayer().addEarthGuardKey(generalMeta);
			item.setItemMeta(generalMeta);
		}

		for (ItemStack item : player.getInventory().getArmorContents()) {
			if (item != null) {
				ItemMeta meta = item.getItemMeta();
				if (meta == null || !Hyperion.getLayer().hasEarthGuardKey(meta)) {
					oldArmor.add(item);
					continue;
				}
			}
			oldArmor.add(new ItemStack(Material.AIR, 1));
		}

		originalMode = player.getGameMode();
		player.getInventory().setArmorContents(newArmor.toArray(new ItemStack[4]));
		new TempPotionEffect(player, new PotionEffect(PotionEffectType.RESISTANCE, NumberConversions.round(duration / 50F), resistance));
		time = System.currentTimeMillis();
		formed = true;
	}

	private void moveBlock() {
		if (!player.getWorld().equals(armorFallingBlock.getFallingBlock().getWorld())) {
			remove();
			return;
		}

		Location loc = armorFallingBlock.getFallingBlock().getLocation().clone().add(0, 0.5, 0);

		Block currentBlock = loc.getBlock();
		if (isTransparent(currentBlock) && !currentBlock.isLiquid()) {
			GeneralMethods.breakBlock(currentBlock);
		} else if (!isEarthbendable(currentBlock) && !currentBlock.isLiquid() && currentBlock.getType() != Material.AIR) {
			ParticleEffect.BLOCK_CRACK.display(currentBlock.getLocation(), 8, 0.5, 0.5, 0.5, 1, blockData);
			remove();
			return;
		}

		final double distanceSquared = player.getEyeLocation().distanceSquared(loc);
		final double speedFactor = (distanceSquared > selectRange * selectRange) ? 1.5 : 0.8;
		if (distanceSquared <= 0.5 * 0.5) {
			Material mat = armorFallingBlock.getFallingBlock().getBlockData().getMaterial();
			armorFallingBlock.remove();
			formArmor(mat);
			return;
		}

		armorFallingBlock.getFallingBlock().setVelocity(GeneralMethods.getDirection(loc, player.getEyeLocation()).normalize().multiply(speedFactor));
	}

	public List<ItemStack> getArmor(boolean old) {
		List<ItemStack> armor = new ArrayList<>(4);
		if (!formed) return armor;
		if (old) {
			armor.addAll(oldArmor);
		} else {
			armor.addAll(newArmor);
		}
		return armor;
	}

	public boolean hasActiveArmor() {
		return formed;
	}

	public boolean isMetalArmor() {
		return metal;
	}

	@Override
	public boolean isEnabled() {
		return Hyperion.getPlugin().getConfig().getBoolean("Abilities.Earth.EarthGuard.Enabled");
	}

	@Override
	public String getName() {
		return "EarthGuard";
	}

	@Override
	public String getDescription() {
		return Hyperion.getPlugin().getConfig().getString("Abilities.Earth.EarthGuard.Description");
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
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
		return player.getLocation();
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
	public void load() {
	}

	@Override
	public void remove() {
		if (armorFallingBlock != null) {
			armorFallingBlock.remove();
		}
		if (formed) {
			player.removePotionEffect(PotionEffectType.RESISTANCE);
			if (!originalMode.equals(player.getGameMode())) {
				for (ItemStack armorItem : oldArmor) {
					if (armorItem != null && armorItem.getType() != Material.AIR) {
						player.getWorld().dropItem(player.getLocation(), armorItem);
					}
				}
				List<ItemStack> clear = new ArrayList<>(4);
				List<ItemStack> denied = getArmor(false);
				for (ItemStack armorItem : player.getInventory().getArmorContents()) {
					if (denied.contains(armorItem)) {
						clear.add(new ItemStack(Material.AIR, 1));
					} else {
						clear.add(armorItem);
					}
				}
				player.getInventory().setArmorContents(clear.toArray(new ItemStack[4]));
			} else {
				player.getInventory().setArmorContents(oldArmor.toArray(new ItemStack[4]));
			}
		}
		super.remove();
	}

	@Override
	public void stop() {
	}
}
