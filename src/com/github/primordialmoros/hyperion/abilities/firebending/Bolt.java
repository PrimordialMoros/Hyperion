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

package com.github.primordialmoros.hyperion.abilities.firebending;

import com.github.primordialmoros.hyperion.Hyperion;
import com.github.primordialmoros.hyperion.methods.CoreMethods;
import com.github.primordialmoros.hyperion.util.MaterialCheck;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.LightningAbility;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Collections;

public class Bolt extends LightningAbility implements AddonAbility {
	private Location location;

	private double damage;
	private long cooldown;
	private int range;
	private long warmup;

	private long strikeTime;
	private boolean charged;
	private boolean struck;

	public Bolt(final Player player) {
		super(player);

		if (!bPlayer.canBend(this) || hasAbility(player, Bolt.class) || isWater(player.getEyeLocation().getBlock())) {
			return;
		}

		damage = Hyperion.getPlugin().getConfig().getDouble("Abilities.Fire.Bolt.Damage");
		cooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Fire.Bolt.Cooldown");
		range = Hyperion.getPlugin().getConfig().getInt("Abilities.Fire.Bolt.Range");
		warmup = Hyperion.getPlugin().getConfig().getLong("Abilities.Fire.Bolt.Warmup");

		charged = false;
		struck = false;
		strikeTime = System.currentTimeMillis();

		range = (int) getDayFactor(range, player.getWorld());
		damage = getDayFactor(damage, player.getWorld());

		location = player.getLocation();

		start();
	}

	@Override
	public void progress() {
		if (struck) {
			if (System.currentTimeMillis() > strikeTime + 500) {
				remove();
			}
			return;
		} else {
			if (!bPlayer.canBendIgnoreCooldowns(this) || isWater(player.getEyeLocation().getBlock())) {
				remove();
				return;
			}
		}

		if (!charged) {
			if (!player.isSneaking()) {
				remove();
				return;
			}
			if (System.currentTimeMillis() > getStartTime() + warmup) {
				charged = true;
			}
		} else {
			if (!struck) {
				if (player.isSneaking()) {
					Location smokeLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().normalize().multiply(1.2));
					ParticleEffect.SMOKE_NORMAL.display(smokeLoc.add(0, 0.3, 0), 2, 0.05, 0.05, 0.05);
				} else {
					strike();
				}
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
			targetLocation = player.getTargetBlock(MaterialCheck.getIgnoreMaterialSet(), range).getLocation();
		}

		if (GeneralMethods.isRegionProtectedFromBuild(this, targetLocation)) {
			remove();
			return;
		}
		location = targetLocation;
		player.getWorld().strikeLightningEffect(location);
		player.getWorld().spawn(location, LightningStrike.class, entity -> {
			entity.setCustomName("Bolt");
			entity.setCustomNameVisible(false);
			entity.setMetadata(CoreMethods.BOLT_KEY, new FixedMetadataValue(Hyperion.getPlugin(), new BoltInfo(this, damage, targetLocation.clone())));
		});
		struck = true;
		strikeTime = System.currentTimeMillis();
		bPlayer.addCooldown(this);
	}

	public static void dealDamage(BoltInfo info) {
		Location strikeLocation = info.getLocation();
		boolean enhanced = isWater(strikeLocation.getBlock());

		for (final Entity e : GeneralMethods.getEntitiesAroundPoint(strikeLocation, 5)) {
			if (e instanceof LivingEntity && !(e instanceof ArmorStand)) {
				if ((e instanceof Player && Commands.invincible.contains((e).getName()))) {
					continue;
				}
				final double distance = e.getLocation().distance(strikeLocation);
				if (distance > 5) continue;

				double calculatedDamage = info.getDamage() - (distance / 2);
				info.getAbility().dealDamage((LivingEntity) e, enhanced ? calculatedDamage * 2 : calculatedDamage);
				AirAbility.breakBreathbendingHold(e);
			}
		}
		info.getAbility().remove();
	}

	public void dealDamage(LivingEntity ent, double dmg) {
		DamageHandler.damageEntity(ent, dmg, this);
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

	public static class BoltInfo {
		private final Bolt ability;
		private final double damage;
		private final Location location;

		public BoltInfo(Bolt boltAbility, double boltDamage, Location boltLocation) {
			ability = boltAbility;
			damage = boltDamage;
			location = boltLocation;
		}

		public double getDamage() {
			return damage;
		}

		public Location getLocation() {
			return location;
		}

		public Bolt getAbility() {
			return ability;
		}
	}
}
