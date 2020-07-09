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

package com.github.primordialmoros.hyperion.abilities.waterbending;

import com.github.primordialmoros.hyperion.Hyperion;
import com.github.primordialmoros.hyperion.methods.CoreMethods;
import com.github.primordialmoros.hyperion.util.MaterialCheck;
import com.github.primordialmoros.hyperion.util.RegenTempBlock;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.IceAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import com.projectkorra.projectkorra.airbending.AirShield;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempPotionEffect;
import com.projectkorra.projectkorra.waterbending.ice.PhaseChange;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class FrostBreath extends IceAbility implements AddonAbility {
	private final Set<Block> blocks = new HashSet<>();
	private long cooldown;
	private long duration;
	private int frostDuration;
	private int currentRange;
	private int range;
	private boolean slowEnabled;
	private Location location;


	public FrostBreath(Player player) {
		super(player);

		if (!bPlayer.canBend(this)) {
			return;
		}

		cooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Water.FrostBreath.Cooldown");
		range = Hyperion.getPlugin().getConfig().getInt("Abilities.Water.FrostBreath.Range");
		duration = Hyperion.getPlugin().getConfig().getLong("Abilities.Water.FrostBreath.Duration");
		frostDuration = Hyperion.getPlugin().getConfig().getInt("Abilities.Water.FrostBreath.FrostDuration");
		slowEnabled = Hyperion.getPlugin().getConfig().getBoolean("Abilities.Water.FrostBreath.Slowness");

		range = (int) getNightFactor(range, player.getWorld());

		currentRange = 0;
		location = player.getEyeLocation();

		start();
	}

	@Override
	public void progress() {
		if (!bPlayer.canBendIgnoreCooldowns(this) || !player.isSneaking()) {
			remove();
			return;
		}
		if (System.currentTimeMillis() > getStartTime() + duration) {
			remove();
			return;
		}
		blocks.clear();
		createBeam();
	}

	private void createBeam() {
		if (currentRange < range) currentRange++;

		location = player.getEyeLocation();
		final Vector direction = player.getEyeLocation().getDirection();
		double size = 0;
		double offset = 0;
		double affect = 1.5;
		for (int i = 0; i < currentRange; i++) {
			location.add(direction.clone());
			size += 0.005;
			offset += 0.15;
			affect += 0.01;

			if (!isTransparent(location.getBlock())) {
				return;
			}

			blocks.add(location.getBlock());

			for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, affect)) {
				if (entity instanceof LivingEntity && entity.getEntityId() != player.getEntityId() && !(entity instanceof ArmorStand)) {
					if (entity instanceof Player && Commands.invincible.contains(entity.getName())) {
						continue;
					}
					for (Location l2 : createCage(entity.getLocation())) {
						if (!GeneralMethods.isRegionProtectedFromBuild(this, l2) && (!l2.getBlock().getType().isSolid() || l2.getBlock().getType().equals(Material.AIR))) {
							PhaseChange.getFrozenBlocksMap().put(new RegenTempBlock(l2.getBlock(), Material.ICE.createBlockData(), ThreadLocalRandom.current().nextInt(1000) + frostDuration), player);
						}
					}
					if (slowEnabled) {
						PotionEffect effect = new PotionEffect(PotionEffectType.SLOW, 60, 5);
						new TempPotionEffect((LivingEntity) entity, effect);
					}
				}
			}

			ParticleEffect.SNOW_SHOVEL.display(location, 3, ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat(), size);
			ParticleEffect.SPELL_MOB.display(CoreMethods.getRandomOffsetLocation(location, offset), 0, 220, 220, 220, 0.003, new Particle.DustOptions(Color.fromRGB(220, 220, 220), 1));
			ParticleEffect.SPELL_MOB.display(CoreMethods.getRandomOffsetLocation(location, offset), 0, 180, 180, 255, 0.0035, new Particle.DustOptions(Color.fromRGB(180, 180, 255), 1));
		}
	}

	private List<Location> createCage(Location centerBlock) {
		List<Location> selectedBlocks = new ArrayList<>();

		int bX = centerBlock.getBlockX();
		int bY = centerBlock.getBlockY();
		int bZ = centerBlock.getBlockZ();
		for (int x = bX - 1; x <= bX + 1; x++) {
			for (int y = bY - 1; y <= bY + 1; y++) {
				Location l = new Location(centerBlock.getWorld(), x, y, bZ);
				selectedBlocks.add(l);
			}
		}
		for (int y = bY - 1; y <= bY + 2; y++) {
			Location l = new Location(centerBlock.getWorld(), bX, y, bZ);
			selectedBlocks.add(l);
		}
		for (int z = bZ - 1; z <= bZ + 1; z++) {
			for (int y = bY - 1; y <= bY + 1; y++) {
				Location l = new Location(centerBlock.getWorld(), bX, y, z);
				selectedBlocks.add(l);
			}
		}
		for (int x = bX - 1; x <= bX + 1; x++) {
			for (int z = bZ - 1; z <= bZ + 1; z++) {
				Location l = new Location(centerBlock.getWorld(), x, bY, z);
				selectedBlocks.add(l);
			}
		}
		return selectedBlocks;
	}

	@Override
	public boolean isEnabled() {
		return Hyperion.getPlugin().getConfig().getBoolean("Abilities.Water.FrostBreath.Enabled");
	}

	@Override
	public String getName() {
		return "FrostBreath";
	}

	@Override
	public String getDescription() {
		return Hyperion.getPlugin().getConfig().getString("Abilities.Water.FrostBreath.Description");
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
		return blocks.stream().map(Block::getLocation).collect(Collectors.toList());
	}

	@Override
	public boolean isCollidable() {
		return true;
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
				if (MaterialCheck.isAir(testBlock) || isWater(testBlock)) {
					PhaseChange.getFrozenBlocksMap().put(new RegenTempBlock(testBlock, Material.ICE.createBlockData(), ThreadLocalRandom.current().nextInt(1000) + frostDuration), player);
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

	@Override
	public void remove() {
		bPlayer.addCooldown(this);
		super.remove();
	}
}
