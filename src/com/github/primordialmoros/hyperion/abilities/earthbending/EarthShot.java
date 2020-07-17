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
import com.github.primordialmoros.hyperion.util.MaterialCheck;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class EarthShot extends EarthAbility implements AddonAbility {
	private BendingFallingBlock projectile;
	private TempBlock source;
	private TempBlock readySource;
	private Vector lastVelocity;
	private Location location;
	private Location origin;

	private double damage;
	private long cooldown;
	private int range;
	private int selectRange;
	private boolean magmaShot;
	private double magmaModifier;
	private long magmaPrepareTime;

	private boolean ready;
	private boolean thrown;
	private boolean convertedMagma;
	private long magmaStartTime;

	public EarthShot(Player player) {
		super(player);

		if (hasAbility(player, EarthShot.class) || !bPlayer.canBend(this)) {
			return;
		}

		damage = Hyperion.getPlugin().getConfig().getDouble("Abilities.Earth.EarthShot.Damage");
		cooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.EarthShot.Cooldown");
		range = Hyperion.getPlugin().getConfig().getInt("Abilities.Earth.EarthShot.Range");
		selectRange = Hyperion.getPlugin().getConfig().getInt("Abilities.Earth.EarthShot.PrepareRange");
		magmaShot = Hyperion.getPlugin().getConfig().getBoolean("Abilities.Earth.EarthShot.MagmaShot.Enabled");
		magmaModifier = Hyperion.getPlugin().getConfig().getDouble("Abilities.Earth.EarthShot.MagmaShot.DamageModifier");
		magmaPrepareTime = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.EarthShot.MagmaShot.PrepareTime");

		ready = false;
		thrown = false;
		convertedMagma = false;
		magmaStartTime = 0;
		origin = player.getLocation().clone();

		if (prepare()) {
			start();
		}
	}

	@Override
	public void progress() {
		if (thrown) {
			if (!bPlayer.canBendIgnoreBindsCooldowns(this)) {
				remove();
				return;
			}

			if (projectile == null || !projectile.getFallingBlock().isValid() || projectile.getFallingBlock().getLocation().distanceSquared(origin) > range * range) {
				remove();
				return;
			}

			Vector velocity = projectile.getFallingBlock().getVelocity().clone();

			if (lastVelocity.angle(velocity) > Math.PI / 4 || velocity.length() < 1.5) {
				shatter();
				remove();
				return;
			}

			if (player.isSneaking()) {
				Vector dir = player.getEyeLocation().getDirection().clone().normalize().multiply(0.2);
				velocity.add(dir.setY(0));
			}

			projectile.getFallingBlock().setVelocity(velocity.normalize().multiply(1.6));
			lastVelocity = projectile.getFallingBlock().getVelocity().clone();
			checkBlast(false);
		} else {
			if (!bPlayer.canBendIgnoreCooldowns(this)) {
				remove();
				return;
			}
			if (!ready) {
				handleSource();
			} else {
				if (readySource.getLocation().distanceSquared(player.getLocation()) > Math.pow(selectRange + 5, 2)) {
					remove();
					return;
				}

				if (!magmaShot || convertedMagma || !bPlayer.canLavabend()) {
					magmaStartTime = 0;
					return;
				}

				final Block targetBlock = player.getTargetBlock(MaterialCheck.getIgnoreMaterialSet(), selectRange * 2);

				if (targetBlock.equals(readySource.getBlock()) && player.isSneaking()) {
					if (magmaStartTime == 0) {
						magmaStartTime = System.currentTimeMillis();
						playLavabendingSound(targetBlock.getLocation());
					}
					playParticles(readySource.getLocation().add(0.5, 0.5, 0.5));
					if (System.currentTimeMillis() > magmaStartTime + magmaPrepareTime) {
						convertedMagma = true;
						readySource.setType(Material.MAGMA_BLOCK);
					}
				} else {
					if (magmaStartTime != 0 && ThreadLocalRandom.current().nextInt(6) == 0) {
						shatter();
						remove();
						return;
					}
					magmaStartTime = 0;
				}
			}
		}
	}

	public boolean prepare() {
		Block block = getEarthSourceBlock(selectRange + 2);
		if (block == null || thrown) {
			return false;
		}

		for (int i = 1; i < 4; i++) {
			Block temp = block.getRelative(BlockFace.UP, i);
			if (isPlant(temp)) temp.breakNaturally();
			if (temp.isLiquid() || !isTransparent(temp)) {
				return false;
			}
		}

		if (isMetal(block)) {
			playMetalbendingSound(block.getLocation());
		} else {
			playEarthbendingSound(block.getLocation());
		}

		projectile = new BendingFallingBlock(block.getLocation().add(0.5, 0, 0.5), block.getBlockData(), new Vector(0, 0.65, 0), this, false);
		source = new TempBlock(block, Material.AIR);
		location = source.getLocation();
		return true;
	}

	public void checkBlast(boolean hit) {
		Location tempLocation = projectile.getFallingBlock().getLocation().clone().add(0, 0.5, 0);
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(tempLocation, 2)) {
			if (entity instanceof LivingEntity && entity.getEntityId() != player.getEntityId() && !(entity instanceof ArmorStand)) {
				if (entity instanceof Player && Commands.invincible.contains((entity).getName())) {
					continue;
				}
				if (convertedMagma) {
					damage *= magmaModifier;
				} else if (isMetal(projectile.getFallingBlock().getBlockData().getMaterial())) {
					damage = getMetalAugment(damage);
				}
				DamageHandler.damageEntity(entity, damage, this);
				((LivingEntity) entity).setNoDamageTicks(0);
				Vector vector = player.getEyeLocation().getDirection();
				entity.setVelocity(vector.normalize().multiply(0.4));
				hit = true;
			}

		}
		if (hit) {
			shatter();
			remove();
		}
	}

	public void shatter() {
		Location tempLocation = projectile.getFallingBlock().getLocation().clone();
		ParticleEffect.BLOCK_CRACK.display(tempLocation, 6, ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat(), 0, projectile.getFallingBlock().getBlockData());
		ParticleEffect.BLOCK_DUST.display(tempLocation, 4, ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat(), 0, projectile.getFallingBlock().getBlockData());
		if (convertedMagma) {
			ParticleEffect.EXPLOSION_NORMAL.display(tempLocation, 3, ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat(), 0.03);
			ParticleEffect.SMOKE_LARGE.display(tempLocation, 20, ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat(), 0.5F);
			ParticleEffect.FIREWORKS_SPARK.display(tempLocation, 10, ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat(), 0.5F);
			tempLocation.getWorld().playSound(tempLocation, Sound.ENTITY_GENERIC_EXPLODE, 2, 0);
		} else {
			if (isMetal(projectile.getFallingBlock().getBlockData().getMaterial())) {
				playMetalbendingSound(tempLocation);
			} else {
				playEarthbendingSound(tempLocation);
			}
		}
	}

	public void handleSource() {
		if (ready) return;
		if (projectile.getFallingBlock().getLocation().getBlockY() >= origin.getBlockY() + 2) {
			readySource = new TempBlock(projectile.getFallingBlock().getLocation().getBlock(), projectile.getFallingBlock().getBlockData());
			projectile.remove();
			getPreventEarthbendingBlocks().add(readySource.getBlock());
			location = readySource.getLocation();
			origin = readySource.getLocation().clone();
			ready = true;
		}
	}

	@Override
	public boolean isEnabled() {
		return Hyperion.getPlugin().getConfig().getBoolean("Abilities.Earth.EarthShot.Enabled");
	}

	@Override
	public String getName() {
		return "EarthShot";
	}

	@Override
	public String getDescription() {
		return Hyperion.getPlugin().getConfig().getString("Abilities.Earth.EarthShot.Description");
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
		return thrown;
	}

	@Override
	public double getCollisionRadius() {
		return 1.5;
	}

	@Override
	public void load() {
	}

	@Override
	public void stop() {
	}

	@Override
	public void remove() {
		projectile.remove();
		source.revertBlock();
		if (readySource != null) {
			readySource.revertBlock();
			getPreventEarthbendingBlocks().remove(readySource.getBlock());
		}
		super.remove();
	}

	public static void throwProjectile(Player player) {
		if (hasAbility(player, EarthShot.class)) {
			getAbility(player, EarthShot.class).throwProjectile();
		}
	}

	public void throwProjectile() {
		if (thrown || !ready) {
			return;
		}

		Vector direction = GeneralMethods.getDirection(readySource.getLocation(), GeneralMethods.getTargetedLocation(player, range, readySource.getBlock().getType())).normalize();
		projectile = new BendingFallingBlock(readySource.getLocation().add(0.5, 0, 0.5), readySource.getBlock().getBlockData(), direction.multiply(1.8), this, true);
		location = projectile.getFallingBlock().getLocation();
		lastVelocity = projectile.getFallingBlock().getVelocity().clone();
		source.revertBlock();
		readySource.revertBlock();
		getPreventEarthbendingBlocks().remove(readySource.getBlock());

		bPlayer.addCooldown(this, cooldown);
		thrown = true;
	}

	public void playParticles(Location loc) {
		ParticleEffect.LAVA.display(loc, 2, ThreadLocalRandom.current().nextFloat() / 2, ThreadLocalRandom.current().nextFloat() / 2, ThreadLocalRandom.current().nextFloat() / 2);
		ParticleEffect.SMOKE_NORMAL.display(loc, 2, ThreadLocalRandom.current().nextFloat() / 2, ThreadLocalRandom.current().nextFloat() / 2, ThreadLocalRandom.current().nextFloat() / 2);
		for (int i = 0; i < 10; i++) {
			GeneralMethods.displayColoredParticle("#FFA400", CoreMethods.getRandomOffsetLocation(loc, 1));
			GeneralMethods.displayColoredParticle("#FF8C00", CoreMethods.getRandomOffsetLocation(loc, 1));
		}
	}
}
