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

package com.github.primordialmoros.hyperion.listeners;

import com.github.primordialmoros.hyperion.Hyperion;
import com.github.primordialmoros.hyperion.abilities.earthbending.EarthGuard;
import com.github.primordialmoros.hyperion.abilities.earthbending.EarthShot;
import com.github.primordialmoros.hyperion.abilities.earthbending.MetalHook;
import com.github.primordialmoros.hyperion.abilities.firebending.Bolt;
import com.github.primordialmoros.hyperion.abilities.firebending.Bolt.BoltInfo;
import com.github.primordialmoros.hyperion.board.BendingBoardManager;
import com.github.primordialmoros.hyperion.methods.CoreMethods;
import com.github.primordialmoros.hyperion.util.BendingFallingBlock;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.event.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.BlockIterator;

public class CoreListener implements Listener {

	@EventHandler(priority = EventPriority.HIGHEST)
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

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onArrowHit(final ProjectileHitEvent event) {
		if (event.getEntity() instanceof Arrow && event.getEntity().hasMetadata(CoreMethods.HOOK_KEY)) {
			final BlockIterator blockIterator = new BlockIterator(event.getEntity().getWorld(), event.getEntity().getLocation().toVector(), event.getEntity().getVelocity().normalize(), 0, 4);
			Block blockHit = null;
			while (blockIterator.hasNext()) {
				blockHit = blockIterator.next();
				if (blockHit.getType() != Material.AIR && !blockHit.isLiquid()) {
					break;
				}
			}
			MetalHook hook = (MetalHook) event.getEntity().getMetadata(CoreMethods.HOOK_KEY).get(0).value();
			if (blockHit != null && hook != null) {
				hook.setBlockHit(blockHit);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBendingElementChange(final PlayerChangeElementEvent event) {
		final Player player = event.getTarget();
		final BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		if (bPlayer == null) return;

		if (event.getResult() == PlayerChangeElementEvent.Result.REMOVE || event.getResult() == PlayerChangeElementEvent.Result.PERMAREMOVE) {
			BendingBoardManager.updateAllSlots(player);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBendingSubElementChange(final PlayerChangeSubElementEvent event) {
		final Player player = event.getTarget();
		final BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		if (bPlayer == null) return;

		if (event.getResult() == PlayerChangeSubElementEvent.Result.REMOVE || event.getResult() == PlayerChangeSubElementEvent.Result.PERMAREMOVE) {
			BendingBoardManager.updateAllSlots(player);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBindChange(final PlayerBindChangeEvent event) {
		final Player player = event.getPlayer();
		if (player == null) return;
		BendingBoardManager.updateBoard(player, event.getAbility(), false, event.getSlot());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onCooldownChange(final PlayerCooldownChangeEvent event) {
		final Player player = event.getPlayer();
		if (player == null) return;
		BendingBoardManager.updateBoard(player, event.getAbility(), event.getResult().equals(PlayerCooldownChangeEvent.Result.ADDED), 0);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onLightningStrike(final EntitySpawnEvent event) {
		if (event.getEntity() instanceof LightningStrike && event.getEntity().hasMetadata(CoreMethods.BOLT_KEY)) {
			BoltInfo boltInfo = (BoltInfo) event.getEntity().getMetadata(CoreMethods.BOLT_KEY).get(0).value();
			if (boltInfo != null) {
				if (!Bolt.isNearbyChannel(boltInfo.getLocation(), boltInfo.getAbility().getPlayer()))
					Bolt.dealDamage(boltInfo);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
		if (event.getDamager() instanceof Arrow && event.getDamager().hasMetadata(CoreMethods.HOOK_KEY)) {
			final Arrow arrow = (Arrow) event.getDamager();
			event.setCancelled(true);
			MetalHook hook = (MetalHook) event.getDamager().getMetadata(CoreMethods.HOOK_KEY).get(0).value();
			if (hook != null) hook.remove();
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInteractAtEntity(final PlayerInteractAtEntityEvent event) {
		if (event.getRightClicked().hasMetadata(CoreMethods.NO_INTERACTION_KEY)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onItemPickup(final EntityPickupItemEvent event) {
		if (event.getItem().hasMetadata(CoreMethods.NO_PICKUP_KEY)) {
			event.setCancelled(true);
			event.getItem().remove();
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onArrowPickup(final PlayerPickupArrowEvent event) {
		if (event.getItem().hasMetadata(CoreMethods.NO_PICKUP_KEY)) {
			event.setCancelled(true);
			event.getItem().remove();
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onHopperItemPickup(final InventoryPickupItemEvent event) {
		if (event.getItem().hasMetadata(CoreMethods.NO_PICKUP_KEY)) {
			event.setCancelled(true);
			event.getItem().remove();
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryClick(final InventoryClickEvent event) {
		if (event.isCancelled() || !(event.getClickedInventory() instanceof PlayerInventory) || event.getSlotType() != InventoryType.SlotType.ARMOR) {
			return;
		}
		final PlayerInventory inventory = (PlayerInventory) event.getClickedInventory();
		if (inventory.getHolder() instanceof Player) {
			Player player = ((Player) inventory.getHolder()).getPlayer();
			if (CoreAbility.hasAbility(player, EarthGuard.class)) {
				EarthGuard guard = CoreAbility.getAbility(player, EarthGuard.class);
				if (guard.hasActiveArmor()) event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onItemMerge(final ItemMergeEvent event) {
		if (event.getEntity().hasMetadata(CoreMethods.GLOVE_KEY) || event.getTarget().hasMetadata(CoreMethods.GLOVE_KEY)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerDeath(final PlayerDeathEvent event) {
		if (CoreAbility.hasAbility(event.getEntity(), EarthGuard.class)) {
			final EarthGuard guard = CoreAbility.getAbility(event.getEntity(), EarthGuard.class);
			if (!guard.hasActiveArmor()) {
				return;
			}
			event.getDrops().removeIf(item -> guard.getArmor(false).contains(item));
			event.getDrops().addAll(guard.getArmor(true));
			guard.remove();
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBendingPlayerCreation(final BendingPlayerCreationEvent event) {
		final Player player = event.getBendingPlayer().getPlayer();
		BendingBoardManager.canUseScoreboard(player);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerLogout(final PlayerQuitEvent event) {
		BendingBoardManager.removeInstance(event.getPlayer());
		if (CoreAbility.hasAbility(event.getPlayer(), EarthGuard.class)) {
			EarthGuard guard = CoreAbility.getAbility(event.getPlayer(), EarthGuard.class);
			if (guard.hasActiveArmor()) guard.remove();
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerSlotChange(final PlayerItemHeldEvent event) {
		if (event.getPreviousSlot() == event.getNewSlot()) return;
		BendingBoardManager.changeActiveSlot(event.getPlayer(), event.getPreviousSlot(), event.getNewSlot());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPKReload(final BendingReloadEvent event) {
		Bukkit.getScheduler().runTaskLater(Hyperion.getPlugin(), Hyperion::reload,  4);
	}
}
