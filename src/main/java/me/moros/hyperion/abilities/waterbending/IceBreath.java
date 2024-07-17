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

package me.moros.hyperion.abilities.waterbending;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.IceAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import com.projectkorra.projectkorra.airbending.AirShield;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.MovementHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import com.projectkorra.projectkorra.util.TempPotionEffect;
import com.projectkorra.projectkorra.waterbending.ice.PhaseChange;
import me.moros.hyperion.Hyperion;
import me.moros.hyperion.methods.CoreMethods;
import me.moros.hyperion.util.BendingFallingBlock;
import me.moros.hyperion.util.MaterialCheck;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class IceBreath extends IceAbility implements AddonAbility {
	private final Set<Location> line = new LinkedHashSet<>();
	private Location location;

	@Attribute(Attribute.DAMAGE)
	private double damage;
	@Attribute(Attribute.RANGE)
	private int range;
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.CHARGE_DURATION)
	private long chargeTime;
	@Attribute(Attribute.DURATION)
	private long frostDuration;

	private double currentRange;
	private boolean charged;
	private boolean released;

	public IceBreath(Player player) {
		super(player);

		if (!bPlayer.canBend(this)) {
			return;
		}

		damage = Hyperion.getPlugin().getConfig().getDouble("Abilities.Water.IceBreath.Damage");
		range = Hyperion.getPlugin().getConfig().getInt("Abilities.Water.IceBreath.Range");
		cooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Water.IceBreath.Cooldown");
		chargeTime = Hyperion.getPlugin().getConfig().getLong("Abilities.Water.IceBreath.ChargeTime");
		frostDuration = Hyperion.getPlugin().getConfig().getLong("Abilities.Water.IceBreath.FrostDuration");

		charged = chargeTime <= 0;
		released = false;
		currentRange = 0;
		location = player.getEyeLocation();

		start();
	}

	@Override
	public void progress() {
		if (!bPlayer.canBendIgnoreCooldowns(this)) {
			remove();
			return;
		}

		if (charged) {
			if (released) {
				Iterator<Location> it = line.iterator();
				for (int i = 0; i < 4; i++) {
					if (it.hasNext()) {
						location = it.next();
						currentRange += 0.25;
						visualizeBreath(currentRange * 0.4, currentRange * 0.025);
						it.remove();
					} else {
						remove();
						return;
					}
				}
				checkArea(1.5 + currentRange * 0.2);
			} else {
				if (player.isSneaking() && chargeTime != 0) {
					CoreMethods.playFocusParticles(player);
				} else {
					if (calculateBreath()) {
						bPlayer.addCooldown(this);
						released = true;
						freezeArea();
					} else {
						remove();
					}
				}
			}
		} else {
			if (!player.isSneaking()) {
				remove();
				return;
			}
			if (System.currentTimeMillis() > getStartTime() + chargeTime) {
				charged = true;
			}
		}
	}

	private void visualizeBreath(double offset, double particleSize) {
		ParticleEffect.SNOW_SHOVEL.display(location, 5, offset, 1, offset, particleSize);
		ParticleEffect.BLOCK_CRACK.display(location, 4, offset, 1, offset, particleSize, Material.ICE.createBlockData());
		ParticleEffect.SPELL_MOB.display(CoreMethods.withGaussianOffset(location, offset), 0, 220, 220, 220, 0.003, Color.fromRGB(220, 220, 220));
		ParticleEffect.SPELL_MOB.display(CoreMethods.withGaussianOffset(location, offset), 0, 180, 180, 255, 0.0035, Color.fromRGB(180, 180, 255));
	}

	private boolean calculateBreath() {
		range = (int) getNightFactor(range, player.getWorld());
		final Vector direction = player.getEyeLocation().getDirection();
		final Location origin = player.getEyeLocation().clone();
		for (Location loc : CoreMethods.getLinePoints(origin, origin.clone().add(direction.clone().multiply(range)), 4 * range)) {
			if (!line.contains(loc) && !isTransparent(loc.getBlock())) {
				break;
			}
			line.add(loc);
		}
		location = origin.clone();
		return !line.isEmpty();
	}

	private void freezeArea() {
		final Location center = GeneralMethods.getTargetedLocation(player, range);

		final List<BlockFace> faces = new ArrayList<>();
		final Vector toPlayer = GeneralMethods.getDirection(center, player.getEyeLocation());
		final double[] vars = {toPlayer.getX(), toPlayer.getY(), toPlayer.getZ()};
		for (int i = 0; i < 3; i++) {
			if (vars[i] != 0) {
				faces.add(GeneralMethods.getBlockFaceFromValue(i, vars[i]));
			}
		}
		int radius = (int) getNightFactor(2, player.getWorld());
		for (final Location l : GeneralMethods.getCircle(center, radius, 1, false, true, 0)) {
			final Block b = l.getBlock();
			for (final BlockFace face : faces) {
				if (b.getRelative(face).getType().isAir()) {
					if (!isWater(b) || RegionProtection.isRegionProtected(this.player, b.getLocation())) {
						continue;
					}
					new TempBlock(b, Material.ICE.createBlockData(), frostDuration);
					break;
				}
			}
		}
	}

	private void checkArea(double radius) {
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, radius)) {
			if (entity instanceof LivingEntity && entity.getEntityId() != player.getEntityId() && !(entity instanceof ArmorStand)) {
				if (entity instanceof Player && Commands.invincible.contains(entity.getName())) {
					continue;
				}
				DamageHandler.damageEntity(entity, getNightFactor(damage, player.getWorld()), this);
				if (entity.isValid()) {
					final MovementHandler mh = new MovementHandler((LivingEntity) entity, CoreAbility.getAbility(IceCrawl.class));
					mh.stopWithDuration(frostDuration / 50, Element.ICE.getColor() + "* Frozen *");
					new BendingFallingBlock(entity.getLocation().clone().add(0, -0.2, 0), Material.PACKED_ICE.createBlockData(), new Vector(), this, false, frostDuration);
					new TempPotionEffect((LivingEntity) entity, new PotionEffect(PotionEffectType.SLOWNESS, (int) (frostDuration / 50), 3));

				}
			}
		}
	}

	@Override
	public boolean isEnabled() {
		return Hyperion.getPlugin().getConfig().getBoolean("Abilities.Water.IceBreath.Enabled");
	}

	@Override
	public String getName() {
		return "IceBreath";
	}

	@Override
	public String getDescription() {
		return Hyperion.getPlugin().getConfig().getString("Abilities.Water.IceBreath.Description");
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
		return new ArrayList<>(line);
	}

	@Override
	public boolean isCollidable() {
		return released;
	}

	@Override
	public double getCollisionRadius() {
		return 0.5;
	}

	@Override
	public void handleCollision(Collision collision) {
		if (collision.getAbilitySecond() instanceof AirShield) {
			int radius = (int) collision.getAbilitySecond().getCollisionRadius();
			for (Location testLoc : GeneralMethods.getCircle(collision.getLocationSecond(), radius, radius, true, true, 0)) {
				final Block testBlock = testLoc.getBlock();
				if (MaterialCheck.isLeaf(testBlock)) testBlock.breakNaturally();
				if (testBlock.getType().isAir() || isWater(testBlock)) {
					PhaseChange.getFrozenBlocksMap().put(new TempBlock(testBlock, Material.ICE.createBlockData(), ThreadLocalRandom.current().nextInt(1000) + frostDuration), player);
				}
			}
		}
		super.handleCollision(collision);
	}

	@Override
	public void load() {
	}

	@Override
	public void stop() {
	}
}
