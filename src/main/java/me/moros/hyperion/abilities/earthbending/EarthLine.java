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

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.earthbending.RaiseEarth;
import com.projectkorra.projectkorra.earthbending.passive.DensityShift;
import com.projectkorra.projectkorra.firebending.util.FireDamageTimer;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.ActionBar;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.MovementHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import me.moros.hyperion.Hyperion;
import me.moros.hyperion.methods.CoreMethods;
import me.moros.hyperion.util.BendingFallingBlock;
import me.moros.hyperion.util.TempArmorStand;
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
	private enum EarthLineMode {NORMAL, PRISON, MAGMA}

	private final List<Block> collapseBlocks = new ArrayList<>();
	private Location location;
	private Location endLocation;
	private LivingEntity target;
	private Vector direction;
	private TempBlock sourceBlock;
	private EarthLineMode mode;

	@Attribute(Attribute.DAMAGE)
	private double damage;
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.RANGE)
	private int range;
	@Attribute(Attribute.SELECT_RANGE)
	private int selectRange;

	@Attribute("Prison" + Attribute.COOLDOWN)
	private long prisonCooldown;
	@Attribute("Prison" + Attribute.DURATION)
	private long prisonDuration;
	@Attribute(Attribute.RADIUS)
	private double prisonRadius;
	@Attribute("PrisonPoints")
	private int prisonPoints;

	@Attribute("MagmaModifier")
	private double magmaModifier;
	@Attribute("RegenDelay")
	private long regen;

	private boolean allowUnderWater;
	private boolean launched;
	private boolean targetLocked;
	private boolean collapsing;

	private boolean makeSpikes;
	private double earthLineSpeed;
	private double magmaLineSpeed;
	private boolean breakBlocks;

	private int ticks;

	public EarthLine(Player player) {
		super(player);

		if (!isEnabled()) return;

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
		selectRange = Hyperion.getPlugin().getConfig().getInt("Abilities.Earth.EarthLine.SelectRange");
		makeSpikes = Hyperion.getPlugin().getConfig().getBoolean("Abilities.Earth.EarthLine.MakeSpikes");
		earthLineSpeed = Hyperion.getPlugin().getConfig().getDouble("Abilities.Earth.EarthLine.Speed");
		allowUnderWater = Hyperion.getPlugin().getConfig().getBoolean("Abilities.Earth.EarthLine.AllowUnderWater");
		magmaModifier = Hyperion.getPlugin().getConfig().getDouble("Abilities.Earth.EarthLine.Magma.DamageModifier");
		magmaLineSpeed = Hyperion.getPlugin().getConfig().getDouble("Abilities.Earth.EarthLine.Magma.Speed");
		breakBlocks = Hyperion.getPlugin().getConfig().getBoolean("Abilities.Earth.EarthLine.Magma.BreakBlocks");
		regen = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.EarthLine.Magma.RegenDelay");
		prisonCooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.EarthLine.PrisonCooldown");
		prisonDuration = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.EarthLine.PrisonDuration");
		prisonRadius = Hyperion.getPlugin().getConfig().getDouble("Abilities.Earth.EarthLine.PrisonRadius");
		prisonPoints = Hyperion.getPlugin().getConfig().getInt("Abilities.Earth.EarthLine.PrisonPoints");

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
				endLocation = GeneralMethods.getTargetedLocation(player, range + selectRange);
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
							ThreadLocalRandom rand = ThreadLocalRandom.current();
							double x = rand.nextDouble(-0.2, 0.2);
							double y = rand.nextDouble(0.1);
							double z = rand.nextDouble(-0.2, 0.2);
							final Vector velocity = new Vector(x, y, z);
							new TempBlock(block, Material.AIR.createBlockData(), regen);
							new BendingFallingBlock(block.getLocation().add(0.5, 0, 0.5), Material.MAGMA_BLOCK.createBlockData(), velocity, this, true);
						}
						return;
					}
					break;
				case NORMAL:
				default:
			}
			advanceLocation();
			checkDamage(1);
			if (ThreadLocalRandom.current().nextInt(5) == 0) playEarthbendingSound(location);
		} else {
			if (!bPlayer.canBendIgnoreCooldowns(this) || sourceBlock.getLocation().distanceSquared(player.getLocation()) > Math.pow(selectRange + 5, 2)) {
				remove();
			}
		}
	}

	private void collapseWall() {
		if (collapsing) return;
		collapsing = true;
		player.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.5f);
		ParticleEffect.EXPLOSION_NORMAL.display(location, 1, 1, 1, 1, 0.5f);
		checkDamage(2);
		for (final Block block : GeneralMethods.getBlocksAroundPoint(location, 3)) {
			if (block.getY() < location.getBlockY() || !isEarthbendable(block) || isMetal(block)) continue;
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
		ActionBar.sendActionBar(getElement().getColor() + "* Prison Mode *", player);
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
		if (isWater(location.getBlock())) {
			if (mode == EarthLineMode.MAGMA) {
				CoreMethods.playExtinguishEffect(location.clone().add(0, 0.2, 0), 16);
				remove();
			} else if (!allowUnderWater) {
				remove();
			}
		}

		double x = ThreadLocalRandom.current().nextDouble(-0.125, 0.125);
		double z = ThreadLocalRandom.current().nextDouble(-0.125, 0.125);
		new TempArmorStand(this, location.clone().add(x, -2, z), mode == EarthLineMode.MAGMA ? Material.MAGMA_BLOCK : location.getBlock().getRelative(BlockFace.DOWN).getType(), 700);

		location.add(direction.clone().multiply(mode == EarthLineMode.MAGMA ? magmaLineSpeed : earthLineSpeed));
		final Block baseBlock = location.getBlock().getRelative(BlockFace.DOWN);
		if (!isValidBlock(baseBlock)) {
			if (isValidBlock(baseBlock.getRelative(BlockFace.UP))) {
				location.add(0, 1, 0);
			} else if (isValidBlock(baseBlock.getRelative(BlockFace.DOWN))) {
				location.add(0, -1, 0);
			} else {
				if (mode == EarthLineMode.MAGMA) {
					if (breakBlocks) {
						collapseWall();
					} else {
						remove();
					}
					return;
				}
				remove();
				return;
			}
		}
		if (RegionProtection.isRegionProtected(this, location) || location.distanceSquared(sourceBlock.getLocation()) > range * range) {
			remove();
		}
	}

	private void checkDamage(double radius) {
		boolean hasHit = false;
		double appliedDamage;
		if (mode == EarthLineMode.MAGMA) {
			appliedDamage = magmaModifier * damage;
		} else {
			appliedDamage = isMetalbendable(location.getBlock().getRelative(BlockFace.DOWN)) ? getMetalAugment(damage) : damage;
		}
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, radius)) {
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
					new FireDamageTimer(entity, player, this);
				}
				hasHit = true;
			}
		}

		if (hasHit) {
			if (mode == EarthLineMode.NORMAL) {
				if (makeSpikes) {
					raiseSpikes();
				} else {
					remove();
				}
			} else {
				remove();
			}
		}
	}

	public boolean prepare() {
		if (launched) return false;
		Block block = getLavaSourceBlock(selectRange);
		if (block == null || !bPlayer.canLavabend()) {
			block = getEarthSourceBlock(selectRange);
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
			case SAND -> sourceData = Material.SANDSTONE.createBlockData();
			case RED_SAND -> sourceData = Material.RED_SANDSTONE.createBlockData();
			case STONE -> sourceData = Material.COBBLESTONE.createBlockData();
			case LAVA -> {
				sourceData = Material.MAGMA_BLOCK.createBlockData();
				mode = EarthLineMode.MAGMA;
			}
			default -> sourceData = Material.STONE.createBlockData();
		}
		sourceBlock = new TempBlock(block, sourceData);
		location = sourceBlock.getLocation();
		return true;
	}

	private boolean isValidBlock(final Block block) {
		if (!isTransparent(block.getRelative(BlockFace.UP))) return false;
		if (mode == EarthLineMode.NORMAL && (isLava(block) || isLava(block.getRelative(BlockFace.UP)))) return false;
		if (mode == EarthLineMode.MAGMA && isMetal(block)) return false;
		return isEarthbendable(block);
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

	private void shootLine() {
		if (launched) {
			if (makeSpikes) {
				raiseSpikes();
			}
			return;
		}

		if (mode != EarthLineMode.MAGMA && !isEarthbendable(player, sourceBlock.getBlock())) {
			return;
		}
		final Block above = sourceBlock.getBlock().getRelative(BlockFace.UP);
		if (above.isLiquid() || !isTransparent(above)) {
			return;
		}

		final Entity targetedEntity = GeneralMethods.getTargetedEntity(player, range + selectRange, Collections.singletonList(player));
		if (targetedEntity instanceof LivingEntity && targetedEntity.getLocation().distanceSquared(location) <= range * range) {
			endLocation = targetedEntity.getLocation();
		} else {
			endLocation = GeneralMethods.getTargetedLocation(player, range);
		}
		sourceBlock.revertBlock();
		location = sourceBlock.getLocation().clone().add(0.5, 1.25, 0.5);
		direction = CoreMethods.calculateFlatVector(location, endLocation);
		launched = true;
		playEarthbendingSound(sourceBlock.getLocation());
		bPlayer.addCooldown(this);
	}
}
