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

package me.moros.hyperion.abilities.earthbending;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.LavaAbility;
import com.projectkorra.projectkorra.ability.MultiAbility;
import com.projectkorra.projectkorra.ability.util.MultiAbilityManager;
import com.projectkorra.projectkorra.ability.util.MultiAbilityManager.MultiAbilityInfoSub;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import me.moros.hyperion.Hyperion;
import me.moros.hyperion.methods.CoreMethods;
import me.moros.hyperion.util.FastMath;
import me.moros.hyperion.util.MaterialCheck;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class LavaDisk extends LavaAbility implements AddonAbility, MultiAbility {
	private static final String[] colors = {"2F1600", "5E2C00", "8C4200", "B05300", "C45D00", "F05A00", "F0A000", "F0BE00"};

	private enum LavaDiskMode {
		FOLLOW, ADVANCE, RETURN, ROTATE, SHATTER
	}

	private static Set<String> materials;
	private Location location;
	private LavaDiskMode mode;

	@Attribute(Attribute.DAMAGE)
	private double maxDamage;
	private double minDamage;
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.RANGE)
	private int range;
	@Attribute("RegenDelay")
	private long regenDelay;
	private boolean passHit;

	private double distance;
	private int angle;
	private int rotationAngle;

	private int ticks = 0;

	public LavaDisk(Player player) {
		super(player);

		if (hasAbility(player, LavaDisk.class) || !bPlayer.canBend(this)) {
			return;
		}

		maxDamage = Hyperion.getPlugin().getConfig().getDouble("Abilities.Earth.LavaDisk.MaxDamage");
		minDamage = Hyperion.getPlugin().getConfig().getDouble("Abilities.Earth.LavaDisk.MinDamage");
		cooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.LavaDisk.Cooldown");
		range = Hyperion.getPlugin().getConfig().getInt("Abilities.Earth.LavaDisk.Range");
		regenDelay = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.LavaDisk.RegenDelay");
		passHit = Hyperion.getPlugin().getConfig().getBoolean("Abilities.Earth.LavaDisk.PassThroughEntities");
		materials = new HashSet<>(Hyperion.getPlugin().getConfig().getStringList("Abilities.Earth.LavaDisk.AdditionalMeltableBlocks"));

		angle = 0;
		rotationAngle = 0;
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
			case 1 -> {
				mode = LavaDiskMode.ADVANCE;
				distance = location.distance(player.getEyeLocation());
				dir.multiply(range + 5);
			}
			case 2 -> {
				mode = LavaDiskMode.RETURN;
				distance = location.distance(player.getEyeLocation());
				dir.multiply(2.5);
			}
			case 3 -> {
				mode = LavaDiskMode.ROTATE;
				angle = player.isSneaking() ? angle + 4 : angle - 4;
				angle = angle % 360;
				dir.multiply(distance);
			}
			case 4 -> {
				mode = LavaDiskMode.SHATTER;
				remove();
				return;
			}
			default -> {
				mode = LavaDiskMode.FOLLOW;
				dir.multiply(distance);
			}
		}

		final Location targetLocation = player.getEyeLocation().add(dir);
		final Vector direction = GeneralMethods.getDirection(location, targetLocation);
		int times = (mode == LavaDiskMode.ADVANCE || mode == LavaDiskMode.RETURN) ? 3 : 2;
		if (times == 3 && player.isSneaking()) times = 1;
		for (int i = 0; i < times; i++) {
			if (location.distanceSquared(targetLocation) < 0.5 * 0.5) break;
			location.add(direction.clone().normalize().multiply(0.4));
		}

		double distanceModifier = (distance < 5) ? 1 : ((distance >= range) ? 0 : 1 - (distance / range));
		int deltaSpeed = Math.max(2, NumberConversions.ceil(16 * distanceModifier));
		rotationAngle += (deltaSpeed % 2 == 0) ? deltaSpeed : ++deltaSpeed;
		if (rotationAngle >= 360) rotationAngle = 0;
		displayLavaDisk();
		if (ticks % 4 == 0) {
			double damage;
			if (mode == LavaDiskMode.ADVANCE || mode == LavaDiskMode.RETURN) {
				damage = maxDamage;
			} else {
				damage = Math.max(minDamage, maxDamage * distanceModifier);
			}
			checkDamage(damage);
		}
		ticks++;
	}

	private void checkDamage(double damage) {
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 1.4)) {
			if (entity instanceof LivingEntity && entity.getEntityId() != player.getEntityId() && !(entity instanceof ArmorStand)) {
				if (entity instanceof Player && Commands.invincible.contains(entity.getName())) {
					continue;
				}
				DamageHandler.damageEntity(entity, damage, this);
				ParticleEffect.LAVA.display(entity.getLocation(), 4, 0.5, 0.5, 0.5, 0.1);
				if (!passHit) {
					remove();
					return;
				}
			}
		}
	}

	private boolean damageBlock(Block block) {
		if (isMetal(block) || block.isLiquid() || TempBlock.isTempBlock(block) || RegionProtection.isRegionProtected(this, block.getLocation()))
			return false;
		if (MaterialCheck.isLeaf(block) || isPlant(block) || materials.contains(block.getType().name()) || isEarthbendable(block)) {
			new TempBlock(block, Material.AIR.createBlockData(), regenDelay);
			ParticleEffect.LAVA.display(block.getLocation(), 1, 0.5, 0.5, 0.5, 0.2);
			if (ThreadLocalRandom.current().nextInt(5) == 0) {
				location.getWorld().playSound(location, Sound.BLOCK_GRINDSTONE_USE, 0.3f, 0.3f);
				location.getWorld().playSound(location, Sound.BLOCK_FIRE_AMBIENT, 0.3f, 1.5f);
			}
			return true;
		}
		return false;
	}

	private void displayLavaDisk() {
		damageBlock(location.getBlock());
		int angle2 = (int) player.getLocation().getYaw() + 90;
		double cos = FastMath.cos(angle);
		double sin = FastMath.sin(angle);
		double cos2 = FastMath.cos(-angle2);
		double sin2 = FastMath.sin(-angle2);
		int offset = 0;
		int index = 0;
		float size = 0.8f;
		for (double pos = 0.1; pos <= 0.8; pos += 0.1) {
			for (int j = 0; j <= 288; j += 72) {
				final Vector temp = new Vector(pos * FastMath.cos(rotationAngle + j + offset), 0, pos * FastMath.sin(rotationAngle + j + offset));
				if (angle != 0) CoreMethods.rotateAroundAxisX(temp, cos, sin);
				if (angle2 != 0) CoreMethods.rotateAroundAxisY(temp, cos2, sin2);
				CoreMethods.displayColoredParticle(colors[index], location.clone().add(temp), 1, 0, 0, 0, size);
				if (pos > 0.5) damageBlock(location.clone().add(temp).getBlock());
			}
			offset += 4;
			index = Math.max(0, Math.min(colors.length - 1, ++index));
			size -= 0.05;
		}
	}

	private boolean isLocationSafe() {
		if (location == null)
			return false;
		if (isWater(location.getBlock())) {
			for (int i = 0; i < 10; i++) {
				ParticleEffect.CLOUD.display(location, 2, ThreadLocalRandom.current().nextDouble(), ThreadLocalRandom.current().nextDouble(), ThreadLocalRandom.current().nextDouble());
			}
			location.getWorld().playSound(location, Sound.BLOCK_LAVA_EXTINGUISH, 1, 1);
			return false;
		}
		return isTransparent(location.getBlock()) || damageBlock(location.getBlock());
	}

	private boolean prepare() {
		Block source = getLavaSourceBlock(5);
		if (source == null) source = getEarthSourceBlock(5);
		if (source == null || isMetal(source)) return false;

		for (int i = 1; i < 3; i++) {
			Block temp = source.getRelative(BlockFace.UP, i);
			if (isPlant(temp)) temp.breakNaturally();
			if (temp.isLiquid() || !isTransparent(temp)) return false;
		}
		if (!isLava(source)) new TempBlock(source, Material.AIR.createBlockData(), regenDelay);
		location = source.getLocation().add(0.5, 0.5, 0.5);
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
		return 1.4;
	}

	@Override
	public void load() {
	}

	@Override
	public void stop() {
	}

	@Override
	public void remove() {
		ParticleEffect.BLOCK_CRACK.display(location, 20, 0.1, 0.1, 0.1, Material.MAGMA_BLOCK.createBlockData());
		location.getWorld().playSound(location, Sound.BLOCK_STONE_BREAK, 1, 1.5f);
		ParticleEffect.LAVA.display(location, 2);
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
