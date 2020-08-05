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
import com.github.primordialmoros.hyperion.util.MaterialCheck;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.LavaAbility;
import com.projectkorra.projectkorra.ability.MultiAbility;
import com.projectkorra.projectkorra.ability.util.MultiAbilityManager;
import com.projectkorra.projectkorra.ability.util.MultiAbilityManager.MultiAbilityInfoSub;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class LavaDisk extends LavaAbility implements AddonAbility, MultiAbility {
	private enum LavaDiskMode {
		FOLLOW, ADVANCE, RETURN, ROTATE, SHATTER
	}

	private static Set<String> materials;
	private Location location;
	private LavaDiskMode mode;

	private double damage;
	private long cooldown;
	private int range;
	private long regen;
	private boolean passHit;

	private double distance;
	private int angle;

	public LavaDisk(Player player) {
		super(player);

		if (hasAbility(player, LavaDisk.class) || !bPlayer.canBend(this)) {
			return;
		}

		damage = Hyperion.getPlugin().getConfig().getDouble("Abilities.Earth.LavaDisk.Damage");
		cooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.LavaDisk.Cooldown");
		range = Hyperion.getPlugin().getConfig().getInt("Abilities.Earth.LavaDisk.Range");
		regen = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.LavaDisk.Regen");
		passHit = Hyperion.getPlugin().getConfig().getBoolean("Abilities.Earth.LavaDisk.PassThroughEntities");
		materials = new HashSet<>(Hyperion.getPlugin().getConfig().getStringList("Abilities.Earth.LavaDisk.AdditionalMeltableBlocks"));

		angle = 0;
		mode = LavaDiskMode.FOLLOW;

		if (prepare()) {
			MultiAbilityManager.bindMultiAbility(player, "LavaDisk");
			start();
		}
	}

	@Override
	public void progress() {
		if (!bPlayer.canBendIgnoreBindsCooldowns(this) || !isLocationSafe() || location.distanceSquared(player.getEyeLocation()) > range * range) {
			remove();
			return;
		}

		final Vector dir = player.getEyeLocation().getDirection();
		switch (player.getInventory().getHeldItemSlot()) {
			case 1:
				mode = LavaDiskMode.ADVANCE;
				distance = location.distance(player.getEyeLocation());
				dir.multiply(range);
				break;
			case 2:
				mode = LavaDiskMode.RETURN;
				distance = location.distance(player.getEyeLocation());
				dir.multiply(3);
				break;
			case 3:
				mode = LavaDiskMode.ROTATE;
				angle = player.isSneaking() ? angle + 4 : angle - 4;
				angle = angle % 360;
				dir.multiply(distance);
				break;
			case 4:
				mode = LavaDiskMode.SHATTER;
				for (int i = 0; i < 10; i++) {
					ParticleEffect.BLOCK_CRACK.display(location, 2, ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat(), Material.DIRT.createBlockData());
				}
				ParticleEffect.LAVA.display(location, 2, ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat(), 0.2);
				remove();
				return;
			case 0:
			default:
				mode = LavaDiskMode.FOLLOW;
				dir.multiply(distance);
				break;
		}

		final Location targetLocation = player.getEyeLocation().add(dir);
		final Vector direction = GeneralMethods.getDirection(location, targetLocation);
		int times = (mode == LavaDiskMode.ADVANCE || mode == LavaDiskMode.RETURN) ? 3 : 2;
		for (int i = 0; i < times; i++) {
			if (location.distanceSquared(targetLocation) < 0.5 * 0.5) break;
			location.add(direction.clone().normalize().multiply(0.4));
		}
		displayLavaDisk();
		if (getCurrentTick() % 4 == 0) {
			checkDamage();
		}
	}

	private void checkDamage() {
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 2)) {
			if (entity instanceof LivingEntity && entity.getEntityId() != player.getEntityId() && !(entity instanceof ArmorStand)) {
				if (entity instanceof Player && Commands.invincible.contains(entity.getName())) {
					continue;
				}
				DamageHandler.damageEntity(entity, damage, this);
				ParticleEffect.LAVA.display(entity.getLocation(), 4, ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat(), 0.1);
				if (!passHit) {
					remove();
					return;
				}
			}
		}
	}

	private void damageBlock(Block block) {
		if (TempBlock.isTempBlock(block) || GeneralMethods.isRegionProtectedFromBuild(this, block.getLocation())) return;
		if (MaterialCheck.isLeaf(block) || isPlant(block) || materials.contains(block.getType().name()) || isEarthbendable(block)) {
			new TempBlock(block, Material.AIR.createBlockData(), regen);
			ParticleEffect.LAVA.display(location, 1, ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat(), 0.2);
		}
	}

	private void displayLavaDisk() {
		ParticleEffect.LAVA.display(location, 1, ThreadLocalRandom.current().nextFloat() / 8, ThreadLocalRandom.current().nextFloat() / 8, ThreadLocalRandom.current().nextFloat() / 8, 0.01);
		int angle2 = (int) player.getLocation().getYaw() + 90;
		for (Location loc : CoreMethods.getCirclePoints(location, 20, 1, angle, angle2)) {
			GeneralMethods.displayColoredParticle(ThreadLocalRandom.current().nextBoolean() ? "#C45D00" : "#B05300", loc);
			if (!MaterialCheck.isAir(loc.getBlock())) {
				damageBlock(loc.getBlock());
			}
		}
		for (Location loc : CoreMethods.getCirclePoints(location, 10, 0.5, angle, angle2)) {
			GeneralMethods.displayColoredParticle(ThreadLocalRandom.current().nextBoolean() ? "#333333" : "#444444", loc);
			if (!MaterialCheck.isAir(loc.getBlock())) {
				damageBlock(loc.getBlock());
			}
		}
	}

	private boolean isLocationSafe() {
		if (location == null || location.getY() <= 2 || location.getY() >= location.getWorld().getMaxHeight() || isWater(location.getBlock())) return false;
		return isTransparent(location.getBlock());
	}

	private boolean prepare() {
		Block source = getLavaSourceBlock(5);
		if (source == null) source = getEarthSourceBlock(5);
		if (source == null) return false;

		for (int i = 1; i < 3; i++) {
			Block temp = source.getRelative(BlockFace.UP, i);
			if (isPlant(temp)) temp.breakNaturally();
			if (temp.isLiquid() || !isTransparent(temp)) return false;
		}
		new TempBlock(source, Material.AIR.createBlockData(), regen);
		location = source.getLocation();
		distance = location.distance(player.getEyeLocation());
		return true;
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
		return true;
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
		MultiAbilityManager.unbindMultiAbility(this.player);
		super.remove();
	}

	@Override
	public ArrayList<MultiAbilityInfoSub> getMultiAbilities() {
		final ArrayList<MultiAbilityInfoSub> abils = new ArrayList<>();
		abils.add(new MultiAbilityInfoSub("Follow", Element.LAVA));
		abils.add(new MultiAbilityInfoSub("Advance", Element.LAVA));
		abils.add(new MultiAbilityInfoSub("Return", Element.LAVA));
		abils.add(new MultiAbilityInfoSub("Rotate", Element.LAVA));
		abils.add(new MultiAbilityInfoSub("Shatter", Element.LAVA));
		return abils;
	}
}
