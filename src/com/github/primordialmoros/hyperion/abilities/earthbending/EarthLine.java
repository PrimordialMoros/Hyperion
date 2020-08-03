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
import com.github.primordialmoros.hyperion.util.TempArmorStand;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.earthbending.RaiseEarth;
import com.projectkorra.projectkorra.earthbending.passive.DensityShift;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.MovementHandler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

public class EarthLine extends EarthAbility implements AddonAbility {
	private Location location;
	private Location endLocation;
	private LivingEntity target;
	private Vector direction;
	private Block sourceBlock;
	private BlockData sourceData;

	private double damage;
	private long cooldown;
	private int range;
	private int prepareRange;

	private long prisonCooldown;
	private long prisonDuration;
	private double prisonRadius;
	private int prisonPoints;

	private boolean launched;
	private boolean started;
	private boolean prison;
	private boolean targetLocked;

	private int ticks;

	public EarthLine(Player player) {
		super(player);

		if (!bPlayer.canBend(this)) {
			return;
		}

		if (hasAbility(player, EarthLine.class)) {
			getAbility(player, EarthLine.class).prepare();
			return;
		}

		launched = false;
		started = false;
		prison = false;
		targetLocked = false;

		damage = Hyperion.getPlugin().getConfig().getDouble("Abilities.Earth.EarthLine.Damage");
		cooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.EarthLine.Cooldown");
		range = Hyperion.getPlugin().getConfig().getInt("Abilities.Earth.EarthLine.Range");
		prepareRange = Hyperion.getPlugin().getConfig().getInt("Abilities.Earth.EarthLine.PrepareRange");

		if (prepare()) {
			started = true;
			start();
		}
	}

	@Override
	public void progress() {
		if (launched) {
			if (!bPlayer.canBendIgnoreBindsCooldowns(this)) {
				remove();
				return;
			}

			if (prison) {
				if (targetLocked) {
					if (target == null || !target.isValid() || ticks > 20) {
						remove();
					} else {
						imprisonTarget();
					}
					return;
				}
			}

			if (player.isSneaking()) {
				endLocation = GeneralMethods.getTargetedLocation(player, range + prepareRange);
				direction = CoreMethods.calculateFlatVector(sourceBlock.getLocation(), endLocation);
			}
			if (ThreadLocalRandom.current().nextInt(5) == 0) {
				playEarthbendingSound(location);
			}
			summonTrailBlock(location.clone().add(0, -1, 0));
			checkDamage();
			advanceLocation();
		} else {
			if (!bPlayer.canBendIgnoreCooldowns(this) || sourceBlock.getLocation().distanceSquared(player.getLocation()) > Math.pow(prepareRange + 5, 2)) {
				remove();
			}
		}
	}

	public void setPrisonMode() {
		if (prison) return;
		prisonCooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.EarthLine.Prison.Cooldown");
		prisonDuration = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.EarthLine.Prison.Duration");
		prisonRadius = Hyperion.getPlugin().getConfig().getDouble("Abilities.Earth.EarthLine.Prison.Radius");
		prisonPoints = Hyperion.getPlugin().getConfig().getInt("Abilities.Earth.EarthLine.Prison.Points");
		ticks = 0;
		prison = true;
	}

	private void imprisonTarget() {
		if (!target.isOnGround()) {
			target.setVelocity(new Vector(0, -1, 0));
			ticks++;
		} else {
			location = target.getLocation();
			Material material = null;
			final Block blockToCheck = location.getBlock().getRelative(BlockFace.DOWN);
			if (isEarthbendable(blockToCheck)) { // Prefer to use the block under the entity first
				material = blockToCheck.getType() == Material.GRASS_BLOCK ? Material.DIRT : blockToCheck.getType();
			} else {
				for (Block block : GeneralMethods.getBlocksAroundPoint(blockToCheck.getLocation(), 1)) {
					if (isEarthbendable(block)) {
						material = block.getType() == Material.GRASS_BLOCK ? Material.DIRT : block.getType();
						break;
					}
				}
			}
			if (material == null) {
				remove();
				return;
			}
			bPlayer.addCooldown("EarthPrison", prisonCooldown);
			for (Location loc : CoreMethods.getCirclePoints(location.clone().add(0, -1.05, 0), prisonPoints, prisonRadius, 0)) {
				new TempArmorStand(this, loc, material, prisonDuration, true);
				new TempArmorStand(this, loc.add(0, -0.6, 0), material, prisonDuration, true);
			}
			final MovementHandler mh = new MovementHandler(target, CoreAbility.getAbility(EarthLine.class));
			mh.stopWithDuration(prisonDuration / 50, Element.EARTH.getColor() + "* Imprisoned *");
			remove();
		}
	}

	private void raiseSpikes() {
		final Location spikeLocation = location.clone().add(0, -1, 0);
		RaiseEarth pillar1 = new RaiseEarth(player, spikeLocation, 1);
		pillar1.setCooldown(0);
		pillar1.setInterval(100);
		RaiseEarth pillar2 = new RaiseEarth(player, spikeLocation.add(direction), 2);
		pillar2.setCooldown(0);
		pillar2.setInterval(100);
		remove();
	}

	private void advanceLocation() {
		location.add(direction.clone().multiply(0.7));
		Block below = location.getBlock().getRelative(BlockFace.DOWN);
		if (isEarthbendable(location.getBlock())) {
			location.add(0, 1, 0);
		} else if (isEarthbendable(below.getRelative(BlockFace.DOWN)) && isTransparent(below)) {
			location.add(0, -1, 0);
		}
		if (GeneralMethods.isRegionProtectedFromBuild(this, location) || location.distanceSquared(sourceBlock.getLocation()) > Math.pow(range, 2)) {
			remove();
		}
	}

	private void checkDamage() {
		boolean hasHit = false;
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 1)) {
			if (entity instanceof LivingEntity && entity.getEntityId() != player.getEntityId() && !(entity instanceof ArmorStand)) {
				if (entity instanceof Player && Commands.invincible.contains(entity.getName())) {
					continue;
				}
				if (!hasHit && prison) {
					target = (LivingEntity) entity;
					target.setVelocity(new Vector(0, -2, 0));
					targetLocked = true;
					return;
				}
				DamageHandler.damageEntity(entity, damage, this);
				hasHit = true;
			}
		}
		if (hasHit) {
			raiseSpikes();
		}
	}

	public boolean prepare() {
		if (launched) return false;
		final Block block = getEarthSourceBlock(prepareRange);
		if (block == null || block.getRelative(BlockFace.UP).isLiquid() || !isTransparent(block.getRelative(BlockFace.UP))) {
			if (started) remove();
			return false;
		}
		if (sourceBlock != null) sourceBlock.setBlockData(sourceData);
		sourceBlock = block;
		if (DensityShift.isPassiveSand(sourceBlock)) {
			DensityShift.revertSand(sourceBlock);
		}
		sourceData = sourceBlock.getBlockData();
		if (sourceBlock.getType() == Material.SAND) {
			sourceBlock.setType(Material.SANDSTONE);
		} else if (sourceBlock.getType() == Material.RED_SAND) {
			sourceBlock.setType(Material.RED_SANDSTONE);
		} else if (sourceBlock.getType() == Material.STONE) {
			sourceBlock.setType(Material.COBBLESTONE);
		} else {
			sourceBlock.setType(Material.STONE);
		}
		location = sourceBlock.getLocation().clone();
		return true;
	}

	private void summonTrailBlock(Location spawnLoc) {
		if (!isEarthbendable(spawnLoc.getBlock()) || !isTransparent(spawnLoc.getBlock().getRelative(BlockFace.UP))) {
			remove();
			return;
		}
		double x = ThreadLocalRandom.current().nextDouble(-0.125, 0.125);
		double z = ThreadLocalRandom.current().nextDouble(-0.125, 0.125);
		new TempArmorStand(this, spawnLoc.clone().add(0.5 + x, -0.75, 0.5 + z), spawnLoc.getBlock().getType(), 700);
	}

	@Override
	public boolean isEnabled() {
		return Hyperion.getPlugin().getConfig().getBoolean("Abilities.Earth.EarthLine.Enabled");
	}

	@Override
	public String getName() {
		return "EarthLine";
	}

	@Override
	public String getDescription() {
		return Hyperion.getPlugin().getConfig().getString("Abilities.Earth.EarthLine.Description");
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
	public boolean isCollidable() {
		return launched;
	}

	@Override
	public double getCollisionRadius() {
		return 0.8;
	}

	@Override
	public void load() {
	}

	@Override
	public void stop() {
	}

	@Override
	public void remove() {
		sourceBlock.setBlockData(sourceData);
		super.remove();
	}

	public static void shootLine(Player player) {
		if (hasAbility(player, EarthLine.class)) {
			getAbility(player, EarthLine.class).shootLine();
		}
	}

	public void shootLine() {
		if (launched) {
			raiseSpikes();
			return;
		}
		if (!isEarthbendable(player, sourceBlock)) {
			return;
		}

		final Entity targetedEntity = GeneralMethods.getTargetedEntity(player, range + prepareRange, Collections.singletonList(player));
		if (targetedEntity instanceof LivingEntity && targetedEntity.getLocation().distanceSquared(location) <= range * range) {
			endLocation = targetedEntity.getLocation();
		} else {
			endLocation = GeneralMethods.getTargetedLocation(player, range);
		}
		sourceBlock.setBlockData(sourceData);
		summonTrailBlock(sourceBlock.getLocation());
		location = sourceBlock.getLocation().clone().add(0, 1, 0);
		direction = CoreMethods.calculateFlatVector(location, endLocation);
		launched = true;
		playEarthbendingSound(sourceBlock.getLocation());
		bPlayer.addCooldown(this);
	}
}
