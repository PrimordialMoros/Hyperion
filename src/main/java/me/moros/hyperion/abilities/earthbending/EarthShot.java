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

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import me.moros.hyperion.Hyperion;
import me.moros.hyperion.methods.CoreMethods;
import me.moros.hyperion.util.BendingFallingBlock;
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

import java.util.concurrent.ThreadLocalRandom;

public class EarthShot extends EarthAbility implements AddonAbility {
	private BendingFallingBlock projectile;
	private TempBlock source;
	private TempBlock readySource;
	private Vector lastVelocity;
	private Location location;
	private Location origin;

	@Attribute(Attribute.DAMAGE)
	private double damage;
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.RANGE)
	private int range;
	@Attribute(Attribute.SELECT_RANGE)
	private int selectRange;
	private boolean magmaShot;
	@Attribute("MagmaModifier")
	private double magmaModifier;
	@Attribute(Attribute.CHARGE_DURATION)
	private long chargeTime;

	private boolean ready;
	private boolean launched;
	private boolean convertedMagma;
	private boolean allowQuickLaunch;
	private long magmaStartTime;

	public EarthShot(Player player) {
		super(player);

		if (hasAbility(player, EarthShot.class) || !bPlayer.canBend(this)) {
			return;
		}

		damage = Hyperion.getPlugin().getConfig().getDouble("Abilities.Earth.EarthShot.Damage");
		cooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.EarthShot.Cooldown");
		range = Hyperion.getPlugin().getConfig().getInt("Abilities.Earth.EarthShot.Range");
		selectRange = Hyperion.getPlugin().getConfig().getInt("Abilities.Earth.EarthShot.SelectRange");
		allowQuickLaunch = Hyperion.getPlugin().getConfig().getBoolean("Abilities.Earth.EarthShot.AllowQuickLaunch");
		magmaShot = Hyperion.getPlugin().getConfig().getBoolean("Abilities.Earth.EarthShot.MagmaShot.AllowConvert");
		magmaModifier = Hyperion.getPlugin().getConfig().getDouble("Abilities.Earth.EarthShot.MagmaShot.DamageModifier");
		chargeTime = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.EarthShot.MagmaShot.ChargeTime");

		ready = false;
		launched = false;
		convertedMagma = false;
		magmaStartTime = 0;
		origin = player.getLocation().clone();

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
			if (projectile == null || !projectile.getFallingBlock().isValid() || projectile.getFallingBlock().getLocation().distanceSquared(origin) > range * range) {
				remove();
				return;
			}
			location = projectile.getFallingBlock().getLocation();
			Vector velocity = projectile.getFallingBlock().getVelocity().clone();
			if (lastVelocity.angle(velocity) > Math.PI / 4 || velocity.length() < 1.5) {
				remove();
				return;
			}
			if (player.isSneaking()) {
				Vector dir = player.getEyeLocation().getDirection().clone().normalize().multiply(0.2);
				velocity.add(dir.setY(0));
			}
			projectile.getFallingBlock().setVelocity(velocity.normalize().multiply(1.8));
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
					return;
				}
				if (readySource.getBlock().equals(player.getTargetBlock(getTransparentMaterialSet(), selectRange * 2)) && player.isSneaking()) {
					if (magmaStartTime == 0) {
						magmaStartTime = System.currentTimeMillis();
						if (chargeTime > 0) playLavabendingSound(readySource.getLocation());
					}
					playParticles(readySource.getLocation().add(0.5, 0.5, 0.5));
					if (chargeTime <= 0 || System.currentTimeMillis() > magmaStartTime + chargeTime) {
						convertedMagma = true;
						readySource.setType(Material.MAGMA_BLOCK);
					}
				} else {
					if (magmaStartTime != 0 && ThreadLocalRandom.current().nextInt(6) == 0) {
						remove();
						return;
					}
					magmaStartTime = 0;
				}
			}
		}
	}

	public boolean prepare() {
		if (launched) return false;
		Block block = getLavaSourceBlock(selectRange);
		if (block == null) {
			Block temp = player.getTargetBlock(getTransparentMaterialSet(), selectRange);
			if (temp.getType() == Material.MAGMA_BLOCK) {
				block = temp;
			}
		}
		if (block == null || !bPlayer.canLavabend()) {
			block = getEarthSourceBlock(selectRange);
			if (block == null || (isMetal(block) && !bPlayer.canMetalbend())) return false;
		}
		if (block.getLocation().getBlockY() > origin.getBlockY()) {
			origin = block.getLocation();
		}
		for (int i = 1; i < 4; i++) {
			Block temp = block.getRelative(BlockFace.UP, i);
			if (isPlant(temp)) temp.breakNaturally();
			if (temp.isLiquid() || !isTransparent(temp)) return false;
		}

		final BlockData data;
		if (isLava(block) || block.getType() == Material.MAGMA_BLOCK) {
			data = Material.MAGMA_BLOCK.createBlockData();
			magmaShot = true;
			convertedMagma = true;
			playEarthbendingSound(block.getLocation());
		} else {
			data = switch (block.getType()) {
				case SAND -> Material.SANDSTONE.createBlockData();
				case RED_SAND -> Material.RED_SANDSTONE.createBlockData();
				case GRAVEL -> Material.STONE.createBlockData();
				default -> block.getBlockData();
			};
			if (isMetal(block)) {
				playMetalbendingSound(block.getLocation());
				magmaShot = false;
			} else {
				playEarthbendingSound(block.getLocation());
			}
		}
		projectile = new BendingFallingBlock(block.getLocation().add(0.5, 0, 0.5), data, new Vector(0, 0.65, 0), this, false);
		if (!isLava(block)) source = new TempBlock(block, Material.AIR);
		location = block.getLocation();
		return true;
	}

	public void checkBlast(boolean hit) {
		double dmg = damage;
		if (convertedMagma) {
			dmg = damage * magmaModifier;
		} else if (isMetal(projectile.getFallingBlock().getBlockData().getMaterial())) {
			dmg = getMetalAugment(damage);
		}
		Location tempLocation = projectile.getFallingBlock().getLocation().clone().add(0, 0.5, 0);
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(tempLocation, 1.5)) {
			if (entity instanceof LivingEntity && entity.getEntityId() != player.getEntityId() && !(entity instanceof ArmorStand)) {
				if (entity instanceof Player && Commands.invincible.contains((entity).getName())) {
					continue;
				}
				DamageHandler.damageEntity(entity, dmg, this);
				((LivingEntity) entity).setNoDamageTicks(0);
				Vector vector = player.getEyeLocation().getDirection();
				entity.setVelocity(vector.normalize().multiply(0.4));
				hit = true;
			}

		}
		if (hit) {
			remove();
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
		return launched;
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
		if (projectile.getFallingBlock() != null) {
			if (launched) {
				final Location tempLocation = projectile.getFallingBlock().getLocation().clone();
				ParticleEffect.BLOCK_CRACK.display(tempLocation, 6, 1, 1, 1, 0, projectile.getFallingBlock().getBlockData());
				ParticleEffect.BLOCK_DUST.display(tempLocation, 4, 1, 1, 1, 0, projectile.getFallingBlock().getBlockData());
				if (convertedMagma) {
					ParticleEffect.SMOKE_LARGE.display(tempLocation, 16, 1, 1, 1, 0.05);
					ParticleEffect.FIREWORKS_SPARK.display(tempLocation, 8, 1, 1, 1, 0.05);
					tempLocation.getWorld().playSound(tempLocation, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0);
				} else {
					if (isMetal(projectile.getFallingBlock().getBlockData().getMaterial())) {
						playMetalbendingSound(tempLocation);
					} else {
						playEarthbendingSound(tempLocation);
					}
				}
			}
			projectile.remove();
		}
		if (source != null) source.revertBlock();
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
		if (launched) {
			return;
		}
		boolean prematureLaunch = false;
		if (!ready) {
			if (!allowQuickLaunch) {
				return;
			}
			prematureLaunch = true;
		}
		if (prematureLaunch) {
			location = projectile.getFallingBlock().getLocation();
			Vector direction = GeneralMethods.getDirection(location, GeneralMethods.getTargetedLocation(player, range)).normalize();
			projectile.getFallingBlock().setGravity(true);
			projectile.getFallingBlock().setVelocity(direction.multiply(1.8).add(new Vector(0, 0.2, 0)));
		} else {
			Vector direction = GeneralMethods.getDirection(readySource.getLocation(), GeneralMethods.getTargetedLocation(player, range, readySource.getBlock().getType())).normalize();
			projectile = new BendingFallingBlock(readySource.getLocation().add(0.5, 0, 0.5), readySource.getBlock().getBlockData(), direction.multiply(1.8).add(new Vector(0, 0.2, 0)), this, true);
			location = projectile.getFallingBlock().getLocation();
			readySource.revertBlock();
			getPreventEarthbendingBlocks().remove(readySource.getBlock());
		}
		lastVelocity = projectile.getFallingBlock().getVelocity().clone();
		if (source != null) source.revertBlock();
		bPlayer.addCooldown(this, cooldown);
		launched = true;
	}

	public void playParticles(Location loc) {
		ParticleEffect.LAVA.display(loc, 2, 0.5, 0.5, 0.5);
		ParticleEffect.SMOKE_NORMAL.display(loc, 2, 0.5, 0.5, 0.5);
		for (int i = 0; i < 8; i++) {
			GeneralMethods.displayColoredParticle("#FFA400", CoreMethods.withGaussianOffset(loc, 1));
			GeneralMethods.displayColoredParticle("#FF8C00", CoreMethods.withGaussianOffset(loc, 1));
		}
	}
}
