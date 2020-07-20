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
import com.projectkorra.projectkorra.ability.LavaAbility;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LavaDisk extends LavaAbility implements AddonAbility {
	private static List<String> meltables = new ArrayList<>();
	private Location location;
	private Vector direction;
	private long time;
	private boolean isTraveling;
	private int recallCount;
	private int angle;
	private double damage;
	private long cooldown;
	private long duration;
	private long regen;
	private boolean lavaOnly;
	private boolean passHit;
	private boolean damageBlocks;
	private boolean lavaTrail;
	private int recall;

	public LavaDisk(Player player) {
		super(player);

		if (hasAbility(player, LavaDisk.class) || !bPlayer.canBend(this)) {
			return;
		}

		damage = Hyperion.getPlugin().getConfig().getDouble("Abilities.Earth.LavaDisk.Damage");
		cooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.LavaDisk.Cooldown");
		duration = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.LavaDisk.Duration");
		regen = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.LavaDisk.Regen");
		recall = Hyperion.getPlugin().getConfig().getInt("Abilities.Earth.LavaDisk.RecallLimit") - 1;
		lavaOnly = Hyperion.getPlugin().getConfig().getBoolean("Abilities.Earth.LavaDisk.LavaSourceOnly");
		passHit = Hyperion.getPlugin().getConfig().getBoolean("Abilities.Earth.LavaDisk.PassThroughEntities");
		damageBlocks = Hyperion.getPlugin().getConfig().getBoolean("Abilities.Earth.LavaDisk.BlockDamage");
		lavaTrail = Hyperion.getPlugin().getConfig().getBoolean("Abilities.Earth.LavaDisk.LavaTrail");
		meltables = Hyperion.getPlugin().getConfig().getStringList("Abilities.Earth.LavaDisk.AdditionalMeltableBlocks");

		time = System.currentTimeMillis();
		isTraveling = false;

		if (prepare()) {
			start();
		}
	}

	@Override
	public void progress() {
		if (!bPlayer.canBendIgnoreCooldowns(this) || isWater(location.getBlock())) {
			remove();
			return;
		}

		if (player.isSneaking() && !isTraveling) {
			location = player.getEyeLocation();
			Vector dV = location.getDirection().normalize();
			location.add(new Vector(dV.getX(), dV.getY(), dV.getZ()).multiply(3));
			while (!isLocationSafe()) {
				location.subtract(new Vector(dV.getX(), dV.getY(), dV.getZ()).multiply(0.1));
				if (location.distanceSquared(player.getEyeLocation()) > 3 * 3) {
					break;
				}
			}
			displayLavaDisk(false);
			time = System.currentTimeMillis();
			location.setPitch(0);
			direction = location.getDirection().normalize();
		} else if (player.isSneaking() && isTraveling && isLocationSafe()) {
			if (recallCount <= recall) {
				returnToSender();
				advanceLocation();
				displayLavaDisk(true);
			}
		} else if (System.currentTimeMillis() < time + duration && isLocationSafe()) {
			isTraveling = true;
			alterPitch();
			advanceLocation();
			displayLavaDisk(true);
		} else {
			remove();
		}
	}

	private void advanceLocation() {
		location = location.add(direction.clone().multiply(0.8));
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 2)) {
			if (entity instanceof LivingEntity && entity.getEntityId() != player.getEntityId() && !(entity instanceof ArmorStand)) {
				if (entity instanceof Player && Commands.invincible.contains(entity.getName())) {
					continue;
				}
				DamageHandler.damageEntity(entity, damage, this);
				ParticleEffect.LAVA.display(entity.getLocation(), 10, ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat(), 0.1F);
				if (!passHit) {
					remove();
					return;
				}
			}
		}
	}

	private void alterPitch() {
		Location loc = player.getLocation().clone();
		if (loc.getPitch() < -20) {
			loc.setPitch(-20);
		}
		if (loc.getPitch() > 20) {
			loc.setPitch(20);
		}
		direction = loc.getDirection().normalize();
	}

	private void damageBlock(Location l) {
		if (GeneralMethods.isRegionProtectedFromBuild(this, l) && TempBlock.isTempBlock(l.getBlock())) {
			return;
		}
		if (meltables.contains(l.getBlock().getType().name()) || isEarthbendable(l.getBlock())) {
			if (lavaTrail) {
				new TempBlock(l.getBlock(), Material.LAVA.createBlockData(d -> ((Levelled) d).setLevel(4)), regen);
			} else {
				new TempBlock(l.getBlock(), Material.AIR.createBlockData(), regen);
			}
			ParticleEffect.LAVA.display(location, 1, ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat(), 0.2F);
		}
	}

	private void displayLavaDisk(boolean largeLava) {
		if (largeLava) {
			ParticleEffect.LAVA.display(location, 6, ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat(), 0.1F);
		} else {
			ParticleEffect.LAVA.display(location, 1, ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat(), 0.1F);
		}
		angle++;
		for (Location loc : CoreMethods.getCirclePoints(location, 20, 1, angle)) {
			GeneralMethods.displayColoredParticle("#C45D00", loc);
			if (largeLava && damageBlocks) {
				damageBlock(loc);
			}
		}
		for (Location loc : CoreMethods.getCirclePoints(location, 10, 0.5, angle)) {
			ParticleEffect.FLAME.display(loc, 1, 0.0F, 0.0F, 0.0F, 0.01F);
			ParticleEffect.SMOKE_NORMAL.display(loc, 1, 0.0F, 0.0F, 0.0F, 0.03F);
			if (largeLava && damageBlocks) {
				damageBlock(loc);
			}
		}
	}

	private boolean isLocationSafe() {
		if (location == null || location.getY() < 2 || location.getY() > 255) {
			return false;
		}
		Block block = location.getBlock();
		return isTransparent(block);
	}

	private boolean prepare() {
		Block block = getLavaSourceBlock(4);
		if (lavaOnly && block == null) {
			return false;
		}
		if (block == null) {
			block = getEarthSourceBlock(4);
		}
		if (block != null && !isWater(block.getRelative(BlockFace.UP))) {
			new TempBlock(block, Material.LAVA.createBlockData(d -> ((Levelled) d).setLevel(4)), regen);
			location = block.getLocation();
			return true;
		}
		return false;
	}

	private void returnToSender() {
		Location loc = player.getEyeLocation();
		Vector dV = loc.getDirection().normalize();
		loc.add(new Vector(dV.getX(), dV.getY(), dV.getZ()).multiply(3));

		Vector vector = loc.toVector().subtract(location.toVector());
		direction = loc.setDirection(vector).getDirection().normalize();

		if (location.distanceSquared(loc) < 0.5 * 0.5) {
			isTraveling = false;
			recallCount += 1;
		}
	}

	@Override
	public boolean isEnabled() {
		return Hyperion.getPlugin().getConfig().getBoolean("Abilities.Earth.LavaDisk.Enabled");
	}

	@Override
	public String getName() {
		return "LavaDisk";
	}

	@Override
	public String getDescription() {
		return Hyperion.getPlugin().getConfig().getString("Abilities.Earth.LavaDisk.Description");
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
		return isTraveling;
	}

	@Override
	public double getCollisionRadius() {
		return 1.6;
	}

	@Override
	public void load() {
	}

	@Override
	public void stop() {
	}

	@Override
	public void remove() {
		bPlayer.addCooldown(this);
		super.remove();
	}
}
