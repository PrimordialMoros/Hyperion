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

package com.github.primordialmoros.hyperion.methods;

import com.github.primordialmoros.hyperion.Hyperion;
import com.github.primordialmoros.hyperion.abilities.earthbending.*;
import com.github.primordialmoros.hyperion.abilities.firebending.Combustion;
import com.github.primordialmoros.hyperion.abilities.firebending.Fireball;
import com.github.primordialmoros.hyperion.abilities.waterbending.FrostBreath;
import com.github.primordialmoros.hyperion.util.FastMath;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import com.projectkorra.projectkorra.airbending.AirShield;
import com.projectkorra.projectkorra.firebending.FireShield;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class CoreMethods {
	private static final BlockFace[] faces = { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST };

	public static final String NO_INTERACTION_KEY = "BENDING_HYPERION_NO_INTERACTION";
	public static final String NO_PICKUP_KEY = "BENDING_HYPERION_NO_PICKUP";
	public static final String GLOVE_KEY = "BENDING_HYPERION_EARTH_GLOVE";
	public static final String HOOK_KEY = "BENDING_HYPERION_METAL_HOOK_KEY";
	public static final String BOLT_KEY = "BENDING_HYPERION_LIGHTNING_KEY";

	public static List<Location> getCirclePoints(Location location, int points, double size, int angleOffset) {
		List<Location> locations = new ArrayList<>();
		for (int i = 0; i < 360; i += 360 / points) {
			double x = size * FastMath.cos(i + angleOffset);
			double z = size * FastMath.sin(i + angleOffset);
			locations.add(location.clone().add(x, 0, z));
		}
		return locations;
	}

	public static void playFocusParticles(final Player player) {
		final Location smokeLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().normalize().multiply(1.2)).add(0, 0.3, 0);
		ParticleEffect.SMOKE_NORMAL.display(smokeLoc, 2, 0.05, 0.05, 0.05);
	}

	public static BlockFace getLeftBlockFace(BlockFace forward) {
		switch (forward) {
			case NORTH_WEST:
				return BlockFace.SOUTH_WEST;
			case NORTH_EAST:
				return BlockFace.NORTH_WEST;
			case SOUTH_WEST:
				return BlockFace.SOUTH_EAST;
			case SOUTH_EAST:
				return BlockFace.NORTH_EAST;
			case NORTH:
				return BlockFace.WEST;
			case SOUTH:
				return BlockFace.EAST;
			case WEST:
				return BlockFace.SOUTH;
			case EAST:
			default:
				return BlockFace.NORTH;
		}
	}

	public static Set<Location> getLinePoints(Location startLoc, Location endLoc, int points) {
		Set<Location> locations = new LinkedHashSet<>();
		Location diff = endLoc.clone().subtract(startLoc);
		double diffX = diff.getX() / points;
		double diffY = diff.getY() / points;
		double diffZ = diff.getZ() / points;
		Location loc = startLoc.clone();
		for (int i = 0; i < points; i++) {
			loc.add(diffX, diffY, diffZ);
			locations.add(loc.clone());
		}
		return locations;
	}

	public static BlockIterator blockRayTrace(Block origin, Block target) {
		if (!origin.getWorld().getName().equals(target.getWorld().getName()) || origin.equals(target)) {
			return new BlockIterator(origin.getLocation());
		}

		final Vector OFFSET_VECTOR = new Vector(0.5, 0.5, 0.5);
		final Vector originVector = origin.getLocation().toVector().add(OFFSET_VECTOR);
		final Vector targetVector = target.getLocation().toVector().add(OFFSET_VECTOR);

		final Vector direction = targetVector.clone().subtract(originVector);
		final double length = target.getLocation().distance(origin.getLocation());

		return new BlockIterator(target.getWorld(), originVector, direction, 0, NumberConversions.round(length));
	}

	public static boolean isAgainstWall(Player player, boolean earthOnly) {
		Block origin = player.getLocation().getBlock();
		for (BlockFace face : faces) {
			Block test = origin.getRelative(face);
			if (GeneralMethods.isSolid(test) && !test.getType().equals(Material.BARRIER)) {
				if (earthOnly && !EarthAbility.isEarthbendable(player, test)) continue;
				return true;
			}
		}
		return false;
	}

	public static Location getRandomOffsetLocation(Location loc, double offset) {
		final double x = ThreadLocalRandom.current().nextDouble(-offset, offset);
		final double y = ThreadLocalRandom.current().nextDouble(-offset, offset);
		final double z = ThreadLocalRandom.current().nextDouble(-offset, offset);
		return loc.clone().add(x, y, z);
	}

	public static Vector calculateFlatVector(Location start, Location end) {
		return new Vector(end.getX() - start.getX(), 0, end.getZ() - start.getZ()).normalize();
	}

	public static Material getBannerColor(final Material material) {
		switch (material) {
			case GRAVEL:
			case CLAY:
			case DIORITE:
			case IRON_BLOCK:
			case QUARTZ_BLOCK:
				return Material.LIGHT_GRAY_BANNER;
			case STONE:
			case STONE_SLAB:
			case STONE_BRICKS:
			case COBBLESTONE:
			case COBBLESTONE_SLAB:
			case ANDESITE:
				return Material.GRAY_BANNER;
			case GOLD_BLOCK:
				return Material.YELLOW_BANNER;
			case RED_SAND:
			case RED_SANDSTONE:
			case RED_SANDSTONE_SLAB:
			case GRANITE:
				return Material.ORANGE_BANNER;
			case NETHERRACK:
				return Material.RED_BANNER;
			case DIRT:
			case GRASS_BLOCK:
			case GRASS_PATH:
			case MYCELIUM:
			case SAND:
			case SANDSTONE:
			case SANDSTONE_SLAB:
			default:
				return Material.BROWN_BANNER;
		}
	}

	public static void loadAbilities() {
		CoreAbility.registerPluginAbilities(Hyperion.getPlugin(), "com.github.primordialmoros.hyperion.abilities");
		boolean collisions = Hyperion.getPlugin().getConfig().getBoolean("EnableCollisions");
		if (collisions) setupCollisions();
	}

	public static void setupCollisions() {
		ProjectKorra.getCollisionInitializer().addSmallAbility(CoreAbility.getAbility(EarthGlove.class));

		ProjectKorra.getCollisionInitializer().addSmallAbility(CoreAbility.getAbility(EarthShot.class));
		ProjectKorra.getCollisionInitializer().addRemoveSpoutAbility(CoreAbility.getAbility(EarthShot.class));

		ProjectKorra.getCollisionInitializer().addLargeAbility(CoreAbility.getAbility(LavaDisk.class));
		ProjectKorra.getCollisionInitializer().addRemoveSpoutAbility(CoreAbility.getAbility(LavaDisk.class));

		ProjectKorra.getCollisionManager().addCollision(new Collision(CoreAbility.getAbility(FrostBreath.class), CoreAbility.getAbility(FireShield.class), true, true));
		ProjectKorra.getCollisionManager().addCollision(new Collision(CoreAbility.getAbility(FrostBreath.class), CoreAbility.getAbility(AirShield.class), true, true));

		ProjectKorra.getCollisionInitializer().addSmallAbility(CoreAbility.getAbility(MetalHook.class));

		ProjectKorra.getCollisionManager().addCollision(new Collision(CoreAbility.getAbility(EarthLine.class), CoreAbility.getAbility(AirShield.class), false, true));

		ProjectKorra.getCollisionInitializer().addLargeAbility(CoreAbility.getAbility(Combustion.class));
		ProjectKorra.getCollisionManager().addCollision(new Collision(CoreAbility.getAbility(Combustion.class), CoreAbility.getAbility(FireShield.class), true, false));
		ProjectKorra.getCollisionManager().addCollision(new Collision(CoreAbility.getAbility(Combustion.class), CoreAbility.getAbility(AirShield.class), true, false));

		ProjectKorra.getCollisionInitializer().addSmallAbility(CoreAbility.getAbility(Fireball.class));

		Hyperion.getLog().info("Registered collisions.");
	}
}
