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

package com.github.primordialmoros.hyperion.abilities.earthbending;

import com.github.primordialmoros.hyperion.Hyperion;
import com.github.primordialmoros.hyperion.methods.CoreMethods;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.MetalAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MetalHook extends MetalAbility implements AddonAbility {
	private Set<Location> pointLocations = new LinkedHashSet<>();
	private Location location;
	private Location origin;
	private Block hookedBlock;
	private BlockData originalData;
	private Arrow hook;

	private long cooldown;
	private int range;

	private boolean reeling;
	private long time;
	private int ticks;

	public MetalHook(Player player) {
		super(player);

		if (hasAbility(player, MetalHook.class) || player.isSneaking() || !hasRequiredInv() || !bPlayer.canBend(this)) {
			return;
		}

		cooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.MetalHook.Cooldown");
		range = Hyperion.getPlugin().getConfig().getInt("Abilities.Earth.MetalHook.Range");

		if (launchHook()) {
			start();
		}
	}

	@Override
	public void progress() {
		if (hook == null || !hook.isValid() || !player.getWorld().equals(hook.getWorld()) || !bPlayer.canBendIgnoreCooldowns(this)) {
			remove();
			return;
		}

		if (origin.distanceSquared(hook.getLocation()) > range * range) {
			remove();
			return;
		}

		if (player.isSneaking()) {
			if (System.currentTimeMillis() > time + 250) {
				remove();
				return;
			}
		} else {
			time = System.currentTimeMillis();
		}

		double distanceToPlayer = player.getLocation().distance(hook.getLocation());

		visualizeLine(distanceToPlayer);
		location = reeling ? player.getLocation() : hook.getLocation();

		if (reeling) {
			if (!hookedBlock.getBlockData().equals(originalData)) {
				remove();
				return;
			}

			final Vector direction = GeneralMethods.getDirection(player.getLocation(), hook.getLocation()).normalize();
			if (distanceToPlayer > 2.5) {
				player.setVelocity(direction.clone().multiply(0.8));
			} else if (distanceToPlayer <= 2.5 && distanceToPlayer > 1.5) {
				player.setVelocity(direction.clone().multiply(0.35));
			} else {
				player.setVelocity(new Vector(0, 0.5, 0));
				remove();
			}
		}
	}

	private boolean launchHook() {
		final Location target = GeneralMethods.getTargetedLocation(player, range);
		if (target.getBlock().isLiquid()) {
			return false;
		}
		final Vector dir = GeneralMethods.getDirection(player.getEyeLocation(), target).normalize();
		origin = player.getEyeLocation().add(player.getEyeLocation().getDirection().normalize().multiply(1.2));
		location = origin.clone();
		final Arrow arrow = player.getWorld().spawnArrow(origin, dir, 1.6F, 0);
		arrow.setShooter(player);
		arrow.setGravity(false);
		arrow.setInvulnerable(true);
		arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
		arrow.setMetadata(CoreMethods.HOOK_KEY, new FixedMetadataValue(Hyperion.getPlugin(), this));
		hook = arrow;
		playMetalbendingSound(arrow.getLocation());
		bPlayer.addCooldown(this);
		return true;
	}

	private void visualizeLine(double distance) {
		ticks++;
		if (ticks % 2 == 0) return;

		pointLocations = CoreMethods.getLinePoints(player.getLocation().add(0, 1.2, 0), hook.getLocation(), NumberConversions.ceil(distance * 2));
		int counter = 0;
		for (final Location tempLocation : pointLocations) {
			if (tempLocation.getBlock().isLiquid() || !isTransparent(tempLocation.getBlock())) {
				counter++;
				if (counter > 2) {
					remove();
					return;
				}
			}
			GeneralMethods.displayColoredParticle("#444444", tempLocation);
		}
	}

	@Override
	public boolean isEnabled() {
		return Hyperion.getPlugin().getConfig().getBoolean("Abilities.Earth.MetalHook.Enabled");
	}

	@Override
	public String getName() {
		return "MetalHook";
	}

	@Override
	public String getDescription() {
		return Hyperion.getPlugin().getConfig().getString("Abilities.Earth.MetalHook.Description");
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
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
		return location;
	}

	@Override
	public List<Location> getLocations() {
		return new ArrayList<>(pointLocations);
	}

	@Override
	public boolean isCollidable() {
		return hook != null;
	}

	@Override
	public double getCollisionRadius() {
		return 0.6;
	}

	@Override
	public void handleCollision(Collision collision) {
		collision.setRemovingSecond(collision.getAbilitySecond() instanceof MetalHook);
		super.handleCollision(collision);
	}

	@Override
	public void load() {
	}

	@Override
	public void stop() {
	}

	@Override
	public void remove() {
		if (hook != null) hook.remove();
		super.remove();
	}

	public void setBlockHit(Block block) {
		if (GeneralMethods.isRegionProtectedFromBuild(player, block.getLocation())) {
			remove();
			return;
		}
		hookedBlock = block;
		originalData = block.getBlockData();
		reeling = true;
	}

	public boolean hasRequiredInv() {
		if (!Hyperion.getPlugin().getConfig().getBoolean("Abilities.Earth.MetalHook.RequireItems")) return true;

		final Material[] inventoryItems = { Material.IRON_CHESTPLATE, Material.IRON_INGOT, Material.IRON_BLOCK };
		final PlayerInventory pi = player.getInventory();
		for (Material mat : inventoryItems) {
			if (pi.contains(mat)) {
				return true;
			}
		}
		return false;
	}
}
