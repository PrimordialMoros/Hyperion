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
import com.github.primordialmoros.hyperion.util.BendingFallingBlock;
import com.github.primordialmoros.hyperion.util.RegenTempBlock;
import com.github.primordialmoros.hyperion.util.TempArmorStand;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.earthbending.RaiseEarth;
import com.projectkorra.projectkorra.earthbending.passive.DensityShift;
import com.projectkorra.projectkorra.firebending.util.FireDamageTimer;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.MovementHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class EarthLine extends EarthAbility implements AddonAbility {
	private enum EarthLineMode {
		NORMAL, PRISON, MAGMA
	}

	private final List<Block> collapseBlocks = new ArrayList<>();
	private Location location;
	private Location endLocation;
	private LivingEntity target;
	private Vector direction;
	private TempBlock sourceBlock;
	private EarthLineMode mode;

	private double damage;
	private long cooldown;
	private int range;
	private int prepareRange;

	private long prisonCooldown;
	private long prisonDuration;
	private double prisonRadius;
	private int prisonPoints;

	private double magmaModifier;
	private long regen;

	private boolean launched;
	private boolean targetLocked;
	private boolean collapsing;

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
		targetLocked = false;
		collapsing = false;

		damage = Hyperion.getPlugin().getConfig().getDouble("Abilities.Earth.EarthLine.Damage");
		cooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.EarthLine.Cooldown");
		range = Hyperion.getPlugin().getConfig().getInt("Abilities.Earth.EarthLine.Range");
		prepareRange = Hyperion.getPlugin().getConfig().getInt("Abilities.Earth.EarthLine.PrepareRange");
		magmaModifier = Hyperion.getPlugin().getConfig().getDouble("Abilities.Earth.EarthLine.Magma.DamageModifier");
		regen = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.EarthLine.Magma.Regen");
		prisonCooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.EarthLine.Prison.Cooldown");
		prisonDuration = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.EarthLine.Prison.Duration");
		prisonRadius = Hyperion.getPlugin().getConfig().getDouble("Abilities.Earth.EarthLine.Prison.Radius");
		prisonPoints = Hyperion.getPlugin().getConfig().getInt("Abilities.Earth.EarthLine.Prison.Points");

		if (prepare()) {
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

			if (player.isSneaking() && mode != EarthLineMode.MAGMA) {
				endLocation = GeneralMethods.getTargetedLocation(player, range + prepareRange);
				direction = CoreMethods.calculateFlatVector(sourceBlock.getLocation(), endLocation);
			}

			switch (mode) {
				case PRISON:
					if (targetLocked) {
						if (target == null || !target.isValid() || ticks > 20) {
							remove();
						} else {
							imprisonTarget();
						}
						return;
					}
					break;
				case MAGMA:
					if (collapsing) {
						for (int i = 0; i < 2; i++) {
							if (collapseBlocks.isEmpty()) {
								remove();
								return;
							}
							int randomIndex = ThreadLocalRandom.current().nextInt(collapseBlocks.size());
							Block block = collapseBlocks.get(randomIndex);
							collapseBlocks.remove(randomIndex);
							final Vector velocity = new Vector(ThreadLocalRandom.current().nextDouble(-0.2, 0.2), ThreadLocalRandom.current().nextDouble(0.1), ThreadLocalRandom.current().nextDouble(-0.2, 0.2));
							new RegenTempBlock(block, Material.AIR.createBlockData(), regen);
							new BendingFallingBlock(block.getLocation().add(0.5, 0, 0.5), Material.MAGMA_BLOCK.createBlockData(), velocity, this, true);
						}
					}
					break;
				case NORMAL:
				default:
			}
			summonTrailBlock(location.clone().add(0, -1, 0));
			checkDamage();
			advanceLocation();
			if (ThreadLocalRandom.current().nextInt(5) == 0) playEarthbendingSound(location);
		} else {
			if (!bPlayer.canBendIgnoreCooldowns(this) || sourceBlock.getLocation().distanceSquared(player.getLocation()) > Math.pow(prepareRange + 5, 2)) {
				remove();
			}
		}
	}

	private void collapseWall(final Location center) {
		if (collapsing) return;
		collapsing = true;
		center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.5f);
		ParticleEffect.EXPLOSION_NORMAL.display(center, 1, ThreadLocalRandom.current().nextDouble(), ThreadLocalRandom.current().nextDouble(), ThreadLocalRandom.current().nextDouble(), 0.5f);
		for (final Block block : GeneralMethods.getBlocksAroundPoint(center, 3)) {
			if (block.getY() < center.getBlockY() || !isEarthbendable(block)) continue;
			collapseBlocks.add(block);
		}
		if (collapseBlocks.isEmpty()) {
			remove();
		}
	}

	public void setPrisonMode() {
		if (mode != EarthLineMode.NORMAL) return;
		ticks = 0;
		mode = EarthLineMode.PRISON;
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
			for (Location loc : CoreMethods.getCirclePoints(location.clone().add(0, -1.05, 0), prisonPoints, prisonRadius)) {
				new TempArmorStand(this, loc, material, prisonDuration, true);
				new TempArmorStand(this, loc.add(0, -0.6, 0), material, prisonDuration, true);
			}
			final MovementHandler mh = new MovementHandler(target, CoreAbility.getAbility(EarthLine.class));
			mh.stopWithDuration(prisonDuration / 50, Element.EARTH.getColor() + "* Imprisoned *");
			remove();
		}
	}

	private void raiseSpikes() {
		if (mode != EarthLineMode.NORMAL) return;
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
		location.add(direction.clone().multiply(mode == EarthLineMode.MAGMA ? 0.4 : 0.7));
		Block below = location.getBlock().getRelative(BlockFace.DOWN);
		if (isEarthbendable(location.getBlock())) {
			location.add(0, 1, 0);
		} else if (isEarthbendable(below.getRelative(BlockFace.DOWN)) && isTransparent(below)) {
			location.add(0, -1, 0);
		}
		if (mode == EarthLineMode.MAGMA) {
			if (isMetal(location.getBlock()) || isMetal(location.getBlock().getRelative(BlockFace.UP))) {
				remove();
				return;
			}
			if (isWater(location.getBlock()) || isWater(location.getBlock().getRelative(BlockFace.UP))) {
				for (int i = 0; i < 10; i++) {
					ParticleEffect.CLOUD.display(location, 2, ThreadLocalRandom.current().nextDouble(), ThreadLocalRandom.current().nextDouble(), ThreadLocalRandom.current().nextDouble());
				}
				location.getWorld().playSound(location, Sound.BLOCK_LAVA_EXTINGUISH, 1, 1);
				remove();
				return;
			}
		}
		GeneralMethods.displayColoredParticle("444444", location);
		if (GeneralMethods.isRegionProtectedFromBuild(this, location) || location.distanceSquared(sourceBlock.getLocation()) > Math.pow(range, 2)) {
			remove();
		}
	}

	private void checkDamage() {
		boolean hasHit = false;
		double appliedDamage = (mode == EarthLineMode.NORMAL && isMetalbendable(location.getBlock())) ? getMetalAugment(damage) : damage;
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 1)) {
			if (entity instanceof LivingEntity && entity.getEntityId() != player.getEntityId() && !(entity instanceof ArmorStand)) {
				if (entity instanceof Player && Commands.invincible.contains(entity.getName())) {
					continue;
				}
				if (!hasHit && mode == EarthLineMode.PRISON) {
					target = (LivingEntity) entity;
					target.setVelocity(new Vector(0, -2, 0));
					targetLocked = true;
					return;
				}
				DamageHandler.damageEntity(entity, appliedDamage, this);
				if (mode == EarthLineMode.MAGMA) {
					entity.setFireTicks(40);
					new FireDamageTimer(entity, player);
				}
				hasHit = true;
			}
		}

		if (hasHit) {
			if (mode == EarthLineMode.NORMAL) {
				raiseSpikes();
			} else {
				remove();
			}
		}
	}

	public boolean prepare() {
		if (launched) return false;
		Block block = getLavaSourceBlock(prepareRange);
		if (block == null || !bPlayer.canLavabend()) {
			block = getEarthSourceBlock(prepareRange);
		}
		if (block == null || block.getRelative(BlockFace.UP).isLiquid() || !isTransparent(block.getRelative(BlockFace.UP))) {
			if (isStarted()) remove();
			return false;
		}
		if (sourceBlock != null) {
			if (block == sourceBlock.getBlock()) return false;
			sourceBlock.revertBlock();
		}
		mode = EarthLineMode.NORMAL;
		if (DensityShift.isPassiveSand(block)) DensityShift.revertSand(block);
		final BlockData sourceData;
		switch (block.getType()) {
			case SAND:
				sourceData = Material.SANDSTONE.createBlockData();
				break;
			case RED_SAND:
				sourceData = Material.RED_SANDSTONE.createBlockData();
				break;
			case STONE:
				sourceData = Material.COBBLESTONE.createBlockData();
				break;
			case LAVA:
				sourceData = Material.MAGMA_BLOCK.createBlockData();
				break;
			case GRAVEL:
			default:
				sourceData = Material.STONE.createBlockData();
		}
		sourceBlock = new TempBlock(block, sourceData);
		location = sourceBlock.getLocation().clone();
		return true;
	}

	private void summonTrailBlock(Location spawnLoc) {
		if (!isEarthbendable(spawnLoc.getBlock()) || !isTransparent(spawnLoc.getBlock().getRelative(BlockFace.UP))) {
			if (mode == EarthLineMode.MAGMA) {
				collapseWall(spawnLoc);
				return;
			}
			remove();
			return;
		}
		double x = ThreadLocalRandom.current().nextDouble(-0.125, 0.125);
		double z = ThreadLocalRandom.current().nextDouble(-0.125, 0.125);
		new TempArmorStand(this, spawnLoc.clone().add(0.5 + x, -0.75, 0.5 + z), mode == EarthLineMode.MAGMA ? Material.MAGMA_BLOCK : spawnLoc.getBlock().getType(), 700);
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
		sourceBlock.revertBlock();
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
		if (sourceBlock.getBlock().getType() == Material.MAGMA_BLOCK) {
			mode = EarthLineMode.MAGMA;
			damage = magmaModifier * damage;
		} else if (!isEarthbendable(player, sourceBlock.getBlock())) {
			return;
		}

		final Entity targetedEntity = GeneralMethods.getTargetedEntity(player, range + prepareRange, Collections.singletonList(player));
		if (targetedEntity instanceof LivingEntity && targetedEntity.getLocation().distanceSquared(location) <= range * range) {
			endLocation = targetedEntity.getLocation();
		} else {
			endLocation = GeneralMethods.getTargetedLocation(player, range);
		}
		sourceBlock.revertBlock();
		summonTrailBlock(sourceBlock.getLocation());
		location = sourceBlock.getLocation().clone().add(0, 1, 0);
		direction = CoreMethods.calculateFlatVector(location, endLocation);
		launched = true;
		playEarthbendingSound(sourceBlock.getLocation());
		bPlayer.addCooldown(this);
	}
}
