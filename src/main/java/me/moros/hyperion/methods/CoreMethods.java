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

package me.moros.hyperion.methods;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import com.projectkorra.projectkorra.airbending.AirShield;
import com.projectkorra.projectkorra.earthbending.EarthSmash;
import com.projectkorra.projectkorra.firebending.FireShield;
import com.projectkorra.projectkorra.util.ColoredParticle;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.waterbending.SurgeWall;
import com.projectkorra.projectkorra.waterbending.SurgeWave;
import me.moros.hyperion.Hyperion;
import me.moros.hyperion.abilities.earthbending.EarthGlove;
import me.moros.hyperion.abilities.earthbending.EarthLine;
import me.moros.hyperion.abilities.earthbending.EarthShot;
import me.moros.hyperion.abilities.earthbending.LavaDisk;
import me.moros.hyperion.abilities.earthbending.MetalCable;
import me.moros.hyperion.abilities.firebending.Combustion;
import me.moros.hyperion.abilities.firebending.FlameRush;
import me.moros.hyperion.abilities.firebending.combo.FireWave;
import me.moros.hyperion.abilities.waterbending.IceBreath;
import me.moros.hyperion.abilities.waterbending.IceCrawl;
import me.moros.hyperion.util.FastMath;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class CoreMethods {
	private static final BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST};

	public static final String NO_INTERACTION_KEY = "BENDING_HYPERION_NO_INTERACTION";
	public static final String NO_PICKUP_KEY = "BENDING_HYPERION_NO_PICKUP";
	public static final String GLOVE_KEY = "BENDING_HYPERION_EARTH_GLOVE";
	public static final String CABLE_KEY = "BENDING_HYPERION_METAL_CABLE_KEY";
	public static final String SMOKESCREEN_KEY = "BENDING_HYPERION_SMOKESCREEN_KEY";

	public static List<Location> getCirclePoints(Location location, int points, double size) {
		List<Location> locations = new ArrayList<>();
		for (int i = 0; i < 360; i += 360 / points) {
			locations.add(location.clone().add(size * FastMath.cos(i), 0, size * FastMath.sin(i)));
		}
		return locations;
	}

	public static void displayColoredParticle(String hexVal, final Location loc, final int amount, final double offsetX, final double offsetY, final double offsetZ, float size) {
		int r = 0;
		int g = 0;
		int b = 0;
		if (hexVal.length() <= 6) {
			r = Integer.valueOf(hexVal.substring(0, 2), 16);
			g = Integer.valueOf(hexVal.substring(2, 4), 16);
			b = Integer.valueOf(hexVal.substring(4, 6), 16);
		}
		new ColoredParticle(Color.fromRGB(r, g, b), size).display(loc, amount, offsetX, offsetY, offsetZ);
	}

	public static void playFocusParticles(final Player player) {
		final Location smokeLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().normalize().multiply(1.2)).add(0, 0.3, 0);
		ParticleEffect.SMOKE_NORMAL.display(smokeLoc, 2, 0.05, 0.05, 0.05);
	}

	public static void playExtinguishEffect(Location location, int amount) {
		if (location == null) return;
		for (int i = 0; i < amount; i++) {
			ParticleEffect.CLOUD.display(location, 1, 0.3, 0.3, 0.3);
		}
		location.getWorld().playSound(location, Sound.BLOCK_LAVA_EXTINGUISH, 1, 1);
	}

	public static BlockFace getLeftBlockFace(BlockFace forward) {
		return switch (forward) {
			case NORTH_WEST -> BlockFace.SOUTH_WEST;
			case NORTH_EAST -> BlockFace.NORTH_WEST;
			case SOUTH_WEST -> BlockFace.SOUTH_EAST;
			case SOUTH_EAST -> BlockFace.NORTH_EAST;
			case NORTH -> BlockFace.WEST;
			case SOUTH -> BlockFace.EAST;
			case WEST -> BlockFace.SOUTH;
			default -> BlockFace.NORTH;
		};
	}

	public static List<Location> getLinePoints(Location startLoc, Location endLoc, int points) {
		List<Location> locations = new ArrayList<>(points);
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
		final Vector OFFSET_VECTOR = new Vector(0.5, 0.5, 0.5);
		final Vector originVector = origin.getLocation().toVector().add(OFFSET_VECTOR);
		final Vector targetVector = target.getLocation().toVector().add(OFFSET_VECTOR);

		final Vector direction = targetVector.clone().subtract(originVector);
		final double length = target.getLocation().distance(origin.getLocation());

		return new BlockIterator(origin.getWorld(), originVector, direction, 0, NumberConversions.round(length));
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

	public static Location withGaussianOffset(Location loc, double offset) {
		return withGaussianOffset(loc, offset, offset, offset);
	}

	public static Location withGaussianOffset(Location loc, double offsetX, double offsetY, double offsetZ) {
		return loc.clone().add(gaussianVector(offsetX, offsetY, offsetZ));
	}

	public static Vector gaussianVector(double offset) {
		return gaussianVector(offset, offset, offset);
	}

	public static Vector gaussianVector(double offsetX, double offsetY, double offsetZ) {
		ThreadLocalRandom r = ThreadLocalRandom.current();
		return new Vector(r.nextGaussian() * offsetX, r.nextGaussian() * offsetY, r.nextGaussian() * offsetZ);
	}

	public static Vector calculateFlatVector(Location start, Location end) {
		return new Vector(end.getX() - start.getX(), 0, end.getZ() - start.getZ()).normalize();
	}

	public static void rotateAroundAxisX(Vector v, double cos, double sin) {
		double y = v.getY() * cos - v.getZ() * sin;
		double z = v.getY() * sin + v.getZ() * cos;
		v.setY(y).setZ(z);
	}

	public static void rotateAroundAxisY(Vector v, double cos, double sin) {
		double x = v.getX() * cos + v.getZ() * sin;
		double z = v.getX() * -sin + v.getZ() * cos;
		v.setX(x).setZ(z);
	}

	public static void loadAbilities() {
		CoreAbility.registerPluginAbilities(Hyperion.getPlugin(), "me.moros.hyperion.abilities");

		if (Hyperion.getPlugin().getConfig().getBoolean("EnableCollisions")) setupCollisions();
	}

	public static void setupCollisions() {
		ProjectKorra.getCollisionInitializer().addSmallAbility(CoreAbility.getAbility(EarthGlove.class));

		ProjectKorra.getCollisionInitializer().addSmallAbility(CoreAbility.getAbility(EarthShot.class));
		ProjectKorra.getCollisionInitializer().addRemoveSpoutAbility(CoreAbility.getAbility(EarthShot.class));

		ProjectKorra.getCollisionInitializer().addLargeAbility(CoreAbility.getAbility(LavaDisk.class));
		ProjectKorra.getCollisionInitializer().addRemoveSpoutAbility(CoreAbility.getAbility(LavaDisk.class));

		ProjectKorra.getCollisionManager().addCollision(new Collision(CoreAbility.getAbility(IceBreath.class), CoreAbility.getAbility(FireShield.class), true, true));
		ProjectKorra.getCollisionManager().addCollision(new Collision(CoreAbility.getAbility(IceBreath.class), CoreAbility.getAbility(AirShield.class), true, true));

		ProjectKorra.getCollisionInitializer().addSmallAbility(CoreAbility.getAbility(MetalCable.class));

		ProjectKorra.getCollisionManager().addCollision(new Collision(CoreAbility.getAbility(EarthLine.class), CoreAbility.getAbility(AirShield.class), false, true));

		ProjectKorra.getCollisionManager().addCollision(new Collision(CoreAbility.getAbility(IceCrawl.class), CoreAbility.getAbility(AirShield.class), false, true));

		ProjectKorra.getCollisionInitializer().addLargeAbility(CoreAbility.getAbility(Combustion.class));
		ProjectKorra.getCollisionManager().addCollision(new Collision(CoreAbility.getAbility(Combustion.class), CoreAbility.getAbility(FireShield.class), true, false));
		ProjectKorra.getCollisionManager().addCollision(new Collision(CoreAbility.getAbility(Combustion.class), CoreAbility.getAbility(AirShield.class), true, false));

		ProjectKorra.getCollisionManager().addCollision(new Collision(CoreAbility.getAbility(FireWave.class), CoreAbility.getAbility(SurgeWave.class), false, true));
		ProjectKorra.getCollisionManager().addCollision(new Collision(CoreAbility.getAbility(FireWave.class), CoreAbility.getAbility(SurgeWall.class), false, true));

		CoreAbility flameRush = CoreAbility.getAbility(FlameRush.class);
		CoreAbility combustion = CoreAbility.getAbility(Combustion.class);
		CoreAbility esmash = CoreAbility.getAbility(EarthSmash.class);
		ProjectKorra.getCollisionManager().addCollision(new Collision(flameRush, flameRush, true, true));
		ProjectKorra.getCollisionManager().addCollision(new Collision(flameRush, combustion, true, true));
		ProjectKorra.getCollisionManager().addCollision(new Collision(flameRush, esmash, true, true));
		ProjectKorra.getCollisionInitializer().addRemoveSpoutAbility(flameRush);

		Set<CoreAbility> collidables = new HashSet<>(ProjectKorra.getCollisionInitializer().getSmallAbilities());
		collidables.addAll(ProjectKorra.getCollisionInitializer().getLargeAbilities());
		for (CoreAbility abil : collidables) {
			if (abil.getName().equalsIgnoreCase("EarthSmash") || abil.getName().equalsIgnoreCase("Combustion")) {
				continue;
			}
			ProjectKorra.getCollisionManager().addCollision(new Collision(flameRush, abil, false, true));
		}

		Hyperion.getLog().info("Registered collisions.");
	}

	public static void setAttributes(ConfigurationSection section, CoreAbility ability) {
		for (String key : section.getKeys(false)) {
			Number value;
			if (section.isInt(key)) {
				value = section.getInt(key);
			} else if (section.isDouble(key)) {
				value = section.getDouble(key);
			} else if (section.isLong(key)) {
				value = section.getLong(key);
			} else {
				continue;
			}
			ability.setAttribute(key, value);
		}
	}
}
