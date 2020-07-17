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

package com.github.primordialmoros.hyperion.util;

import com.github.primordialmoros.hyperion.Hyperion;
import com.github.primordialmoros.hyperion.methods.CoreMethods;
import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.EulerAngle;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TempArmorStand {
	private static final Map<ArmorStand, TempArmorStand> instances = new ConcurrentHashMap<>();
	private static final Queue<TempArmorStand> tasQueue = new PriorityQueue<>(Comparator.comparingLong(TempArmorStand::getExpirationTime));
	private static final EulerAngle DEFAULT_ANGLE = new EulerAngle(0, 0, 0);

	private final ArmorStand armorStand;
	private final CoreAbility ability;
	private final long expirationTime;

	public TempArmorStand(CoreAbility abilityInstance, Location location, Material material) {
		this(abilityInstance, location, material, 30000, DEFAULT_ANGLE);
	}

	public TempArmorStand(CoreAbility abilityInstance, Location location, Material material, long delay) {
		this(abilityInstance, location, material, delay, DEFAULT_ANGLE);
	}

	public TempArmorStand(CoreAbility abilityInstance, Location location, Material material, long delay, EulerAngle headPose) {
		Location spawnLocation = location.clone();
		armorStand = spawnLocation.getWorld().spawn(spawnLocation, ArmorStand.class, entity -> {
			entity.setHeadPose(headPose);
			entity.setInvulnerable(true);
			entity.setVisible(false);
			entity.setGravity(false);
			entity.getEquipment().setHelmet(new ItemStack(material));
			entity.setMetadata(CoreMethods.NO_INTERACTION_KEY, new FixedMetadataValue(Hyperion.getPlugin(), ""));
		});

		expirationTime = System.currentTimeMillis() + delay;
		ability = abilityInstance;
		instances.put(armorStand, this);
		tasQueue.add(this);
	}

	public CoreAbility getAbility() {
		return ability;
	}

	public ArmorStand getArmorStand() {
		return armorStand;
	}

	public long getExpirationTime() {
		return expirationTime;
	}

	public static boolean isTempArmorStand(ArmorStand as) {
		return instances.containsKey(as);
	}

	public static TempArmorStand get(ArmorStand as) {
		return instances.get(as);
	}

	public static List<TempArmorStand> getFromAbility(CoreAbility ability) {
		return instances.values().stream().filter(tas -> tas.getAbility().equals(ability)).collect(Collectors.toList());
	}

	public static void manage() {
		final long currentTime = System.currentTimeMillis();
		while (!tasQueue.isEmpty()) {
			final TempArmorStand tas = tasQueue.peek();
			if (currentTime > tas.getExpirationTime()) {
				tasQueue.poll();
				tas.remove();
			} else {
				return;
			}
		}
	}

	public void remove() {
		instances.remove(armorStand);
		armorStand.remove();
	}

	public static void removeAll() {
		tasQueue.clear();
		instances.keySet().forEach(Entity::remove);
		instances.clear();
	}
}
