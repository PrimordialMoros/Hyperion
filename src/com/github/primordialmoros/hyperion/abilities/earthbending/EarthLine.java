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
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.earthbending.RaiseEarth;
import com.projectkorra.projectkorra.earthbending.passive.DensityShift;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
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
	private Block sourceBlock;
	private BlockData sourceData;
	private boolean progressing;
	private boolean hasHit;
	private boolean hasClicked;
	private boolean started;
	private long cooldown;
	private int range;
	private int prepareRange;
	private double damage;

	public EarthLine(Player player) {
		super(player);

		if (!bPlayer.canBend(this)) {
			return;
		}

		if (hasAbility(player, EarthLine.class)) {
			getAbility(player, EarthLine.class).prepare();
			return;
		}

		progressing = false;
		hasHit = false;
		hasClicked = false;
		started = false;

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
		if (!progressing) {
			if (!bPlayer.canBendIgnoreCooldowns(this)) {
				remove();
				return;
			}

			if (sourceBlock.getLocation().distanceSquared(player.getLocation()) > prepareRange * prepareRange) {
				remove();
				return;
			}
			return;
		} else {
			if (!bPlayer.canBendIgnoreBindsCooldowns(this)) {
				remove();
				return;
			}
		}

		if (player.isSneaking()) {
			endLocation = GeneralMethods.getTargetedLocation(player, range + prepareRange);
		}

		if (ThreadLocalRandom.current().nextInt(5) == 0) {
			playEarthbendingSound(location);
		}

		checkDamage(CoreMethods.calculateFlatVector(location, endLocation));

		if (hasHit || hasClicked) {
			RaiseEarth pillar1 = new RaiseEarth(player, location.clone().add(0, -1, 0), 1);
			pillar1.setCooldown(0);
			pillar1.setInterval(100);

			location = location.add(CoreMethods.calculateFlatVector(location, endLocation));
			RaiseEarth pillar2 = new RaiseEarth(player, location.clone().add(0, -1, 0), 2);
			pillar2.setCooldown(0);
			pillar2.setInterval(100);
			remove();
			return;
		}

		if (location.distanceSquared(endLocation) < 0.4) {
			remove();
			return;
		}

		location = location.add(CoreMethods.calculateFlatVector(sourceBlock.getLocation(), endLocation).multiply(0.7));
		Block below = location.getBlock().getRelative(BlockFace.DOWN);

		if (isEarthbendable(location.getBlock())) {
			location.add(0, 1, 0);
		} else if (isEarthbendable(below.getRelative(BlockFace.DOWN)) && isTransparent(below)) {
			location.add(0, -1, 0);
		}

		if (location.distanceSquared(sourceBlock.getLocation()) > range * range) {
			remove();
			return;
		}
		summonTrailBlock(location.clone().add(0, -1, 0));
	}

	private void checkDamage(Vector push) {
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 1.25)) {
			if (entity instanceof LivingEntity && entity.getEntityId() != player.getEntityId() && !(entity instanceof ArmorStand)) {
				if (entity instanceof Player && Commands.invincible.contains(entity.getName())) {
					continue;
				}
				entity.setVelocity(push.add(new Vector(0, 0.75, 0)));
				DamageHandler.damageEntity(entity, damage, this);
				hasHit = true;
			}
		}
	}

	public boolean prepare() {
		if (progressing) {
			return false;
		}

		if (sourceBlock != null) {
			sourceBlock.setBlockData(sourceData);
		}

		final Block block = getEarthSourceBlock(prepareRange);

		if (block == null || block.getRelative(BlockFace.UP).isLiquid() || !isTransparent(block.getRelative(BlockFace.UP))) {
			if (started) {
				remove();
			}
			return false;
		}

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
		TempArmorStand tas = new TempArmorStand(this, spawnLoc.clone().add(0.5 + x, -0.75, 0.5 + z), spawnLoc.getBlock().getType(), 700);
		ParticleEffect.BLOCK_CRACK.display(tas.getArmorStand().getEyeLocation().add(0, 0.2, 0), 6, ThreadLocalRandom.current().nextFloat() / 4, ThreadLocalRandom.current().nextFloat() / 8, ThreadLocalRandom.current().nextFloat() / 4, 0, spawnLoc.getBlock().getBlockData());
		ParticleEffect.BLOCK_DUST.display(tas.getArmorStand().getEyeLocation().add(0, 0.2, 0), 8, ThreadLocalRandom.current().nextFloat() / 4, ThreadLocalRandom.current().nextFloat() / 8, ThreadLocalRandom.current().nextFloat() / 4, 0, spawnLoc.getBlock().getBlockData());
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
		return progressing;
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
		if (progressing) {
			hasClicked = true;
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
		progressing = true;
		playEarthbendingSound(sourceBlock.getLocation());
		bPlayer.addCooldown(this);
	}
}
