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
import com.projectkorra.projectkorra.ability.MetalAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import me.moros.hyperion.Hyperion;
import me.moros.hyperion.abilities.earthbending.util.Projectile;
import me.moros.hyperion.methods.CoreMethods;
import me.moros.hyperion.util.BendingFallingBlock;
import me.moros.hyperion.util.MaterialCheck;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MainHand;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class MetalCable extends MetalAbility implements AddonAbility {
	private final List<Location> pointLocations = new ArrayList<>();
	private Location location;
	private Location origin;
	private Arrow cable;
	private CableTarget target;

	@Attribute(Attribute.DAMAGE)
	private double damage;
	@Attribute(Attribute.SPEED)
	private double blockSpeed;
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.RANGE)
	private int range;
	@Attribute("RegenDelay")
	private long regenDelay;

	private boolean hasHit;
	private int ticks;

	public MetalCable(Player player) {
		super(player);

		if (hasAbility(player, MetalCable.class)) {
			getAbility(player, MetalCable.class).attemptLaunchTarget();
			return;
		}

		if (!hasRequiredInv() || !bPlayer.canBend(this)) {
			return;
		}

		damage = Hyperion.getPlugin().getConfig().getDouble("Abilities.Earth.MetalCable.Damage");
		blockSpeed = Hyperion.getPlugin().getConfig().getDouble("Abilities.Earth.MetalCable.BlockSpeed");
		cooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.MetalCable.Cooldown");
		range = Hyperion.getPlugin().getConfig().getInt("Abilities.Earth.MetalCable.Range");
		regenDelay = Hyperion.getPlugin().getConfig().getInt("Abilities.Earth.MetalCable.RegenDelay");

		hasHit = false;

		if (launchCable()) {
			start();
			bPlayer.addCooldown(this);
		}
	}

	@Override
	public void progress() {
		if (cable == null || !cable.isValid() || !player.getWorld().equals(cable.getWorld()) || !bPlayer.canBendIgnoreCooldowns(this)) {
			remove();
			return;
		}
		if (origin.distanceSquared(location) > range * range) {
			remove();
			return;
		}
		location = cable.getLocation();
		final double distance = player.getLocation().distance(location);
		if (hasHit) {
			if (target == null || !target.isValid(player)) {
				remove();
				return;
			}

			final Vector direction;
			Entity entityToMove = player;
			Location targetLocation = location;
			if (target.getType() == CableTarget.Type.ENTITY) {
				cable.teleport(target.getEntity().getLocation());
				if (player.isSneaking()) {
					entityToMove = target.getEntity();
					Vector dir = player.getEyeLocation().getDirection().multiply(distance / 2);
					targetLocation = player.getEyeLocation().add(dir);
				}
			}
			direction = GeneralMethods.getDirection(entityToMove.getLocation(), targetLocation).normalize();
			if (distance > 3) {
				entityToMove.setVelocity(direction.multiply(0.8));
			} else {
				if (target.getType() == CableTarget.Type.ENTITY) {
					entityToMove.setVelocity(new Vector());
					if (target.getEntity() instanceof FallingBlock fb) {
						Location tempLocation = fb.getLocation().add(0, 0.5, 0);
						ParticleEffect.BLOCK_CRACK.display(tempLocation, 4, ThreadLocalRandom.current().nextDouble() / 4, ThreadLocalRandom.current().nextDouble() / 8, ThreadLocalRandom.current().nextDouble() / 4, 0, fb.getBlockData());
						ParticleEffect.BLOCK_DUST.display(tempLocation, 6, ThreadLocalRandom.current().nextDouble() / 4, ThreadLocalRandom.current().nextDouble() / 8, ThreadLocalRandom.current().nextDouble() / 4, 0, fb.getBlockData());
						target.getEntity().remove();
					}
					remove();
					return;
				} else {
					if (distance <= 3 && distance > 1.5) {
						entityToMove.setVelocity(direction.multiply(0.35));
					} else {
						player.setVelocity(new Vector(0, 0.5, 0));
						remove();
						return;
					}
				}
			}
		}
		visualizeLine(distance);
	}

	public void attemptLaunchTarget() {
		if (target == null || target.getType() == CableTarget.Type.BLOCK) return;

		final List<Entity> ignore = new ArrayList<>(3);
		ignore.add(cable);
		ignore.add(player);
		ignore.add(target.getEntity());

		final Entity targetedEntity = GeneralMethods.getTargetedEntity(player, range, ignore);
		final Location targetLocation;
		if (targetedEntity instanceof LivingEntity) {
			targetLocation = targetedEntity.getLocation();
		} else {
			targetLocation = player.getTargetBlock(getTransparentMaterialSet(), range).getLocation();
		}

		Vector direction = GeneralMethods.getDirection(location, targetLocation).normalize();
		target.getEntity().setVelocity(direction.multiply(blockSpeed).add(new Vector(0, 0.2, 0)));
		remove();
	}

	private boolean launchCable() {
		final Location target = GeneralMethods.getTargetedLocation(player, range);
		if (target.getBlock().isLiquid()) {
			return false;
		}

		if (player.getMainHand() == MainHand.RIGHT) {
			origin = GeneralMethods.getRightSide(player.getLocation(), 0.3);
		} else {
			origin = GeneralMethods.getLeftSide(player.getLocation(), 0.3);
		}
		origin.add(0, 1, 0);
		final Vector dir = GeneralMethods.getDirection(origin, target).normalize();
		final Arrow arrow = player.getWorld().spawnArrow(origin, dir, 1.8F, 0);
		arrow.setShooter(player);
		arrow.setGravity(false);
		arrow.setInvulnerable(true);
		arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
		arrow.setMetadata(CoreMethods.CABLE_KEY, new FixedMetadataValue(Hyperion.getPlugin(), this));
		cable = arrow;
		location = cable.getLocation();
		playMetalbendingSound(arrow.getLocation());
		return true;
	}

	private void visualizeLine(double distance) {
		ticks++;
		if (ticks % 2 == 0) return;

		final Location temp;
		if (player.getMainHand() == MainHand.RIGHT) {
			temp = GeneralMethods.getRightSide(player.getLocation(), 0.3);
		} else {
			temp = GeneralMethods.getLeftSide(player.getLocation(), 0.3);
		}
		pointLocations.clear();
		pointLocations.addAll(CoreMethods.getLinePoints(temp.add(0, 1, 0), location, NumberConversions.ceil(distance * 2)));
		int counter = 0;
		for (final Location tempLocation : pointLocations) {
			if (tempLocation.getBlock().isLiquid() || !isTransparent(tempLocation.getBlock())) {
				if (++counter > 2) {
					remove();
					return;
				}
			}
			GeneralMethods.displayColoredParticle("#444444", tempLocation);
		}
	}

	@Override
	public boolean isEnabled() {
		return Hyperion.getPlugin().getConfig().getBoolean("Abilities.Earth.MetalCable.Enabled");
	}

	@Override
	public String getName() {
		return "MetalCable";
	}

	@Override
	public String getDescription() {
		return Hyperion.getPlugin().getConfig().getString("Abilities.Earth.MetalCable.Description");
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
		return pointLocations;
	}

	@Override
	public boolean isCollidable() {
		return cable != null;
	}

	@Override
	public double getCollisionRadius() {
		return 0.6;
	}

	@Override
	public void handleCollision(Collision collision) {
		collision.setRemovingSecond(collision.getAbilitySecond() instanceof MetalCable);
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
		if (cable != null) cable.remove();
		super.remove();
	}

	public void setHitBlock(Block block) {
		if (target != null) return;
		if (RegionProtection.isRegionProtected(player, block.getLocation())) {
			remove();
			return;
		}
		if (player.isSneaking() && !MaterialCheck.isUnbreakable(block)) {
			BlockData data = block.getState().getBlockData();
			new TempBlock(block, Material.AIR.createBlockData(), regenDelay);
			final Vector velocity = GeneralMethods.getDirection(location, player.getEyeLocation()).normalize().multiply(0.2);
			final BendingFallingBlock bfb = new BendingFallingBlock(location, data, velocity, this, true);
			new Projectile(this, bfb, damage);
			target = new CableTarget(bfb.getFallingBlock());
		} else {
			target = new CableTarget(block);
		}
		hasHit = true;
	}

	public void setHitEntity(Entity entity) {
		if (target != null) return;
		if (RegionProtection.isRegionProtected(player, entity.getLocation())) {
			remove();
			return;
		}
		target = new CableTarget(entity);
		hasHit = true;
	}

	public boolean hasRequiredInv() {
		Set<String> materials = new HashSet<>(Hyperion.getPlugin().getConfig().getStringList("Abilities.Earth.MetalCable.RequiredItems"));
		if (materials.isEmpty()) return true;
		return materials.stream().map(Material::getMaterial).filter(Objects::nonNull).anyMatch(player.getInventory()::contains);
	}

	public static void attemptDestroy(final Player player) {
		for (Entity targetedEntity : GeneralMethods.getEntitiesAroundPoint(player.getEyeLocation(), 3)) {
			if (targetedEntity instanceof Arrow && targetedEntity.hasMetadata(CoreMethods.CABLE_KEY)) {
				MetalCable ability = (MetalCable) targetedEntity.getMetadata(CoreMethods.CABLE_KEY).get(0).value();
				if (ability != null && !player.equals(ability.getPlayer())) {
					ability.remove();
					return;
				}
			}
		}
	}

	public static class CableTarget {
		private enum Type {ENTITY, BLOCK}

		private final Type type;
		private final Entity entity;
		private final Block block;
		private final Material material;

		public CableTarget(Entity entity) {
			block = null;
			material = null;
			this.entity = entity;
			type = Type.ENTITY;
		}

		public CableTarget(Block block) {
			entity = null;
			this.block = block;
			material = block.getType();
			type = Type.BLOCK;
		}

		public Type getType() {
			return type;
		}

		public Entity getEntity() {
			return entity;
		}

		public Block getBlock() {
			return block;
		}

		public boolean isValid(Player p) {
			if (type == Type.ENTITY) {
				return entity != null && entity.isValid() && entity.getWorld().equals(p.getWorld());
			} else {
				return block.getType() == material;
			}
		}
	}
}
