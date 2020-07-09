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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TempArmorStand {
	private static final Map<ArmorStand, TempArmorStand> instances = new ConcurrentHashMap<>();
	private final ArmorStand armorStand;
	private final CoreAbility ability;
	private final long expirationTime;

	public TempArmorStand(CoreAbility abilityInstance, Location location, Material material) {
		this(abilityInstance, location, material, 0);
	}

	public TempArmorStand(CoreAbility abilityInstance, Location location, Material material, long delay) {
		Location spawnLocation = location.clone();
		armorStand = spawnLocation.getWorld().spawn(spawnLocation, ArmorStand.class, entity -> {
			entity.setInvulnerable(true);
			entity.setVisible(false);
			entity.setGravity(false);
			entity.getEquipment().setHelmet(new ItemStack(material));
			entity.setMetadata(CoreMethods.NO_INTERACTION_KEY, new FixedMetadataValue(Hyperion.getPlugin(), ""));
		});

		expirationTime = System.currentTimeMillis() + delay;
		ability = abilityInstance;
		instances.put(armorStand, this);
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
		Iterator<TempArmorStand> iterator = instances.values().iterator();
		while (iterator.hasNext()) {
			TempArmorStand tas = iterator.next();
			if (System.currentTimeMillis() > tas.getExpirationTime()) {
				tas.getArmorStand().remove();
				iterator.remove();
			}
		}
	}

	public void remove() {
		instances.remove(armorStand);
		armorStand.remove();
	}

	public static void removeAll() {
		instances.keySet().forEach(Entity::remove);
		instances.clear();
	}
}
