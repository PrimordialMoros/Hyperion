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
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.MetalAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MetalHook extends MetalAbility implements AddonAbility {
	private long cooldown;
	private int range;
	private boolean requireSource;
	private boolean reeling;
	private boolean setByEvent;
	private long time;
	private Location location;
	private Location hookLocation;
	private List<Location> pointLocations = new ArrayList<>();
	private Arrow hook;
	private int counter;

	public MetalHook(Player player) {
		super(player);

		if (hasAbility(player, MetalHook.class) || !bPlayer.canBend(this) || (requireSource && !hasRequiredInv()) || player.isSneaking()) {
			return;
		}

		cooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.MetalHook.Cooldown");
		range = Hyperion.getPlugin().getConfig().getInt("Abilities.Earth.MetalHook.Range");
		requireSource = Hyperion.getPlugin().getConfig().getBoolean("Abilities.Earth.MetalHook.RequireItem");

		reeling = false;
		setByEvent = false;
		counter = 0;

		launchHook();
		start();
	}

	@Override
	public void progress() {
		if (!bPlayer.canBendIgnoreCooldowns(this) || hook == null || counter > 2) {
			remove();
			return;
		}
		if (player.isSneaking()) {
			if (System.currentTimeMillis() > time + 500) {
				remove();
				return;
			}
		} else {
			time = System.currentTimeMillis();
		}

		double distance;
		if (!setByEvent) {
			location = hook.getLocation();
			distance = player.getLocation().distance(location);
		} else {
			distance = player.getLocation().distance(hookLocation);
		}

		if (hook.isDead() || !player.getWorld().equals(hook.getWorld()) || location == null) {
			remove();
			return;
		}

		if (distance > range) {
			remove();
			return;
		}

		pointLocations = CoreMethods.getLinePoints(player.getLocation().add(0, 1.2, 0), location, (int) Math.ceil(distance) * 2);
		for (Location tempLocation : pointLocations) {
			if (tempLocation != location) {
				if (!isTransparent(tempLocation.getBlock()) || tempLocation.getBlock().isLiquid()) {
					counter++;
				}
			}
			GeneralMethods.displayColoredParticle("#444444", tempLocation);
		}

		if (reeling) {
			Vector direction = GeneralMethods.getDirection(player.getLocation(), hookLocation);
			if (distance >= 2.5) {
				player.setVelocity(direction.normalize().multiply(0.8));
			} else if (distance < 2.5 && distance > 1.5) {
				player.setVelocity(direction.normalize().multiply(0.35));
			} else {
				player.setVelocity(new Vector(0, 0.5, 0));
				remove();
			}
		}
	}

	public void launchHook() {
		Vector dir = GeneralMethods.getDirection(player.getEyeLocation(), GeneralMethods.getTargetedLocation(player, range)).normalize();
		Arrow arrow = player.getWorld().spawnArrow(player.getEyeLocation().add(player.getEyeLocation().getDirection().normalize().multiply(1.2)), dir, 1.6F, 0);
		arrow.setShooter(player);
		arrow.setGravity(false);
		arrow.setInvulnerable(true);
		arrow.setMetadata(CoreMethods.HOOK_KEY, new FixedMetadataValue(Hyperion.getPlugin(), this));
		arrow.setMetadata(CoreMethods.NO_PICKUP_KEY, new FixedMetadataValue(Hyperion.getPlugin(), ""));
		hook = arrow;
		playMetalbendingSound(arrow.getLocation());
		bPlayer.addCooldown(this);
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
		return null;
	}

	@Override
	public List<Location> getLocations() {
		return pointLocations;
	}

	@Override
	public boolean isCollidable() {
		return hook != null;
	}

	@Override
	public double getCollisionRadius() {
		return 0.4;
	}

	@Override
	public void handleCollision(Collision collision) {
		if (!collision.getAbilitySecond().getElement().equals(Element.ICE)) {
			super.handleCollision(collision);
		}
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

	public void setBlockHit(Block blockHit) {
		if (!setByEvent) {
			hookLocation = blockHit.getLocation();
			reeling = true;
			setByEvent = true;
		}
	}

	public boolean hasRequiredInv() {
		final List<Material> inventoryItems = Arrays.asList(Material.IRON_CHESTPLATE, Material.IRON_INGOT, Material.IRON_BLOCK);
		final PlayerInventory pi = player.getInventory();
		for (Material mat : inventoryItems) {
			if (pi.contains(mat)) {
				return true;
			}
		}
		return false;
	}
}
