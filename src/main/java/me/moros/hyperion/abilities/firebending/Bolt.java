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

package me.moros.hyperion.abilities.firebending;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.LightningAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.DamageHandler;
import me.moros.hyperion.Hyperion;
import me.moros.hyperion.methods.CoreMethods;
import me.moros.hyperion.util.MaterialCheck;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collections;

public class Bolt extends LightningAbility implements AddonAbility {
	private Location location;

	@Attribute(Attribute.DAMAGE)
	private double damage;
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.RANGE)
	private int range;
	@Attribute(Attribute.CHARGE_DURATION)
	private long chargeTime;

	private boolean charged;

	public Bolt(final Player player) {
		super(player);

		if (isWater(player.getEyeLocation().getBlock()) || hasAbility(player, Bolt.class) || !bPlayer.canBend(this)) {
			return;
		}

		damage = Hyperion.getPlugin().getConfig().getDouble("Abilities.Fire.Bolt.Damage");
		cooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Fire.Bolt.Cooldown");
		range = Hyperion.getPlugin().getConfig().getInt("Abilities.Fire.Bolt.Range");
		chargeTime = Hyperion.getPlugin().getConfig().getLong("Abilities.Fire.Bolt.ChargeTime");

		charged = chargeTime <= 0;

		range = (int) getDayFactor(range, player.getWorld());
		damage = getDayFactor(damage, player.getWorld());

		location = player.getLocation();

		start();
	}

	@Override
	public void progress() {
		if (!bPlayer.canBendIgnoreCooldowns(this) || isWater(player.getEyeLocation().getBlock())) {
			remove();
			return;
		}

		if (charged) {
			if (player.isSneaking() && chargeTime != 0) {
				CoreMethods.playFocusParticles(player);
			} else {
				strike();
				remove();
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

	@Override
	public boolean isEnabled() {
		return Hyperion.getPlugin().getConfig().getBoolean("Abilities.Fire.Bolt.Enabled");
	}

	@Override
	public String getName() {
		return "Bolt";
	}

	@Override
	public String getDescription() {
		return Hyperion.getPlugin().getConfig().getString("Abilities.Fire.Bolt.Description");
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
	public void load() {
	}

	@Override
	public void stop() {
	}

	private void strike() {
		final Entity targetedEntity = GeneralMethods.getTargetedEntity(player, range, Collections.singletonList(player));
		final Location targetLocation;
		if (targetedEntity instanceof LivingEntity) {
			targetLocation = targetedEntity.getLocation();
		} else {
			targetLocation = player.getTargetBlock(getTransparentMaterialSet(), range).getLocation();
		}

		if (RegionProtection.isRegionProtected(this, targetLocation)) {
			remove();
			return;
		}
		location = targetLocation;
		player.getWorld().strikeLightningEffect(location);
		player.getWorld().playSound(location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 5, 1.2F);
		bPlayer.addCooldown(this);
		if (!Bolt.isNearbyChannel(location, player)) {
			dealDamage(location);
		}
	}

	public void dealDamage(Location strikeLocation) {
		boolean enhanced = isWater(strikeLocation.getBlock());
		for (final Entity e : GeneralMethods.getEntitiesAroundPoint(strikeLocation, 5)) {
			if (e instanceof Creeper) ((Creeper) e).setPowered(true);
			if (e instanceof LivingEntity livingEntity && !(e instanceof ArmorStand)) {
				if ((e instanceof Player && Commands.invincible.contains((e).getName()))) {
					continue;
				}
				final double distance = e.getLocation().distance(strikeLocation);
				if (distance > 5) continue;

				boolean vulnerable = enhanced || MaterialCheck.hasMetalArmor(livingEntity);
				double baseDamage = vulnerable ? damage * 2 : damage;
				double distanceModifier = enhanced ? distance / 3 : distance / 2;
				DamageHandler.damageEntity(e, (distance < 1.5) ? baseDamage : baseDamage - distanceModifier, this);
				AirAbility.breakBreathbendingHold(e);
			}
		}
	}

	public static boolean isNearbyChannel(Location location, Player source) {
		for (Bolt boltInstance : getAbilities(Bolt.class)) {
			if (!boltInstance.player.getWorld().equals(location.getWorld())) {
				continue;
			}
			if (boltInstance.player.getLocation().distanceSquared(location) < 4 * 4 && boltInstance.getPlayer() != source) {
				boltInstance.charged = true;
				return true;
			}
		}
		return false;
	}
}
