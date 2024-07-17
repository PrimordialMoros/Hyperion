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

package me.moros.hyperion.listeners;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.event.AbilityStartEvent;
import com.projectkorra.projectkorra.event.BendingReloadEvent;
import me.moros.hyperion.Hyperion;
import me.moros.hyperion.abilities.chiblocking.Smokescreen;
import me.moros.hyperion.abilities.chiblocking.Smokescreen.SmokescreenData;
import me.moros.hyperion.abilities.earthbending.EarthGuard;
import me.moros.hyperion.abilities.earthbending.EarthShot;
import me.moros.hyperion.abilities.earthbending.MetalCable;
import me.moros.hyperion.abilities.earthbending.passive.Locksmithing;
import me.moros.hyperion.configuration.ConfigManager;
import me.moros.hyperion.methods.CoreMethods;
import me.moros.hyperion.util.BendingFallingBlock;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Nameable;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Lockable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

public class CoreListener implements Listener {
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void EntityChangeBlockEvent(final EntityChangeBlockEvent event) {
		if (event.getEntityType().equals(EntityType.FALLING_BLOCK)) {
			final FallingBlock fb = (FallingBlock) event.getEntity();
			if (BendingFallingBlock.isBendingFallingBlock(fb)) {
				final BendingFallingBlock bfb = BendingFallingBlock.get(fb);
				final CoreAbility ability = bfb.getAbility();
				if (ability instanceof EarthShot) {
					((EarthShot) ability).checkBlast(true);
				} else if (ability instanceof EarthGuard) {
					ability.remove();
				}
				bfb.remove();
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onArrowHit(final ProjectileHitEvent event) {
		if (event.getEntity() instanceof Arrow && event.getEntity().hasMetadata(CoreMethods.CABLE_KEY)) {
			final MetalCable cable = (MetalCable) event.getEntity().getMetadata(CoreMethods.CABLE_KEY).get(0).value();
			if (cable != null) {
				if (event.getHitBlock() != null) {
					cable.setHitBlock(event.getHitBlock());
				} else if (event.getHitEntity() instanceof LivingEntity) {
					cable.setHitEntity(event.getHitEntity());
				} else {
					event.getEntity().remove();
				}
			}
		} else if (event.getEntity() instanceof Snowball && event.getEntity().hasMetadata(CoreMethods.SMOKESCREEN_KEY)) {
			final SmokescreenData data = (SmokescreenData) event.getEntity().getMetadata(CoreMethods.SMOKESCREEN_KEY).get(0).value();
			if (data != null) {
				Smokescreen.createCloud(event.getEntity().getLocation(), data);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
		if (event.getDamager() instanceof Arrow && event.getDamager().hasMetadata(CoreMethods.CABLE_KEY)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onEntityDamageEvent(final EntityDamageEvent event) {
		if (event.getCause() != DamageCause.FIRE && event.getCause() != DamageCause.FIRE_TICK) {
			return;
		}
		if (event.getEntity() instanceof Player player) {
			if (CoreAbility.hasAbility(player, EarthGuard.class)) {
				if (CoreAbility.getAbility(player, EarthGuard.class).hasActiveArmor()) {
					player.setFireTicks(0);
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onInteractAtEntity(final PlayerInteractAtEntityEvent event) {
		if (event.getRightClicked().hasMetadata(CoreMethods.NO_INTERACTION_KEY)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onItemPickup(final EntityPickupItemEvent event) {
		if (event.getItem().hasMetadata(CoreMethods.NO_PICKUP_KEY)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onHopperItemPickup(final InventoryPickupItemEvent event) {
		if (event.getItem().hasMetadata(CoreMethods.NO_PICKUP_KEY)) {
			event.setCancelled(true);
			event.getItem().remove();
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onInventoryClick(final InventoryClickEvent event) {
		ItemStack item = event.getCurrentItem();
		if (item == null || !(event.getClickedInventory() instanceof PlayerInventory)) {
			return;
		}

		ItemMeta meta = item.getItemMeta();
		if (meta != null && Hyperion.getLayer().hasEarthGuardKey(meta)) {
			final PlayerInventory inventory = (PlayerInventory) event.getClickedInventory();
			if (inventory.getHolder() instanceof Player) {
				final Player player = ((Player) inventory.getHolder()).getPlayer();
				if (!CoreAbility.hasAbility(player, EarthGuard.class)) {
					inventory.remove(item);
				}
			}
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onItemMerge(final ItemMergeEvent event) {
		if (event.getEntity().hasMetadata(CoreMethods.GLOVE_KEY) || event.getTarget().hasMetadata(CoreMethods.GLOVE_KEY)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onPlayerDeath(final PlayerDeathEvent event) {
		if (CoreAbility.hasAbility(event.getEntity(), EarthGuard.class)) {
			final EarthGuard guard = CoreAbility.getAbility(event.getEntity(), EarthGuard.class);
			if (guard.hasActiveArmor()) {
				event.getDrops().removeIf(item -> guard.getArmor(false).contains(item));
				event.getDrops().addAll(guard.getArmor(true));
				guard.remove();
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerChangeWorld(final PlayerChangedWorldEvent event) {
		if (CoreAbility.hasAbility(event.getPlayer(), EarthGuard.class)) {
			CoreAbility.getAbility(event.getPlayer(), EarthGuard.class).remove();
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerLogout(final PlayerQuitEvent event) {
		if (CoreAbility.hasAbility(event.getPlayer(), EarthGuard.class)) {
			CoreAbility.getAbility(event.getPlayer(), EarthGuard.class).remove();
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPKReload(final BendingReloadEvent event) {
		Bukkit.getScheduler().runTaskLater(Hyperion.getPlugin(), Hyperion::reload, 1);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onAbilityStart(final AbilityStartEvent event) {
		if (event.getAbility() instanceof CoreAbility ability) {
			final Player player = ability.getPlayer();
			final BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
			if (player == null || bPlayer == null) return;
			if (bPlayer.isAvatarState()) {
				ConfigurationSection section = ConfigManager.modifiersConfig.getConfig().getConfigurationSection("AvatarState." + ability.getName());
				if (section != null) {
					CoreMethods.setAttributes(section, ability);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		Block block = event.getBlock();
		if (block.getState() instanceof Lockable lockable && !Locksmithing.canBreak(player, lockable)) {
			String name = ((Nameable) lockable).getCustomName();
			if (name == null) {
				name = "Container";
			}
			player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TranslatableComponent("container.isLocked", name));
			Location loc = block.getLocation().add(0.5, 0.5, 0.5);
			block.getWorld().playSound(loc, Sound.BLOCK_CHEST_LOCKED, 1, 1);
			event.setCancelled(true);
		}
	}
}
