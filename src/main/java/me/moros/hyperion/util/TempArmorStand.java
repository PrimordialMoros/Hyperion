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

package me.moros.hyperion.util;

import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.util.ParticleEffect;
import me.moros.hyperion.Hyperion;
import me.moros.hyperion.methods.CoreMethods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TempArmorStand {
	private static final Map<ArmorStand, TempArmorStand> instances = new ConcurrentHashMap<>();
	private static final Queue<TempArmorStand> tasQueue = new PriorityQueue<>(Comparator.comparingLong(TempArmorStand::getExpirationTime));

	private final ArmorStand armorStand;
	private final CoreAbility ability;
	private final Material headMaterial;
	private final long expirationTime;
	private final boolean particles;

	public TempArmorStand(CoreAbility abilityInstance, Location location, Material material, long delay) {
		this(abilityInstance, location, material, delay, false);
	}

	public TempArmorStand(CoreAbility abilityInstance, Location location, Material material, long delay, boolean showRemoveParticles) {
		headMaterial = material;
		armorStand = location.getWorld().spawn(location, ArmorStand.class, entity -> {
			entity.setInvulnerable(true);
			entity.setVisible(false);
			entity.setGravity(false);
			entity.getEquipment().setHelmet(new ItemStack(headMaterial));
			entity.setMetadata(CoreMethods.NO_INTERACTION_KEY, new FixedMetadataValue(Hyperion.getPlugin(), ""));
		});
		expirationTime = System.currentTimeMillis() + delay;
		ability = abilityInstance;
		instances.put(armorStand, this);
		tasQueue.add(this);
		particles = showRemoveParticles;
		showParticles(true);
	}

	public void showParticles(boolean show) {
		if (show) {
			ParticleEffect.BLOCK_CRACK.display(armorStand.getEyeLocation().add(0, 0.2, 0), 4, 0.25, 0.125, 0.25, 0, headMaterial.createBlockData());
			ParticleEffect.BLOCK_DUST.display(armorStand.getEyeLocation().add(0, 0.2, 0), 6, 0.25, 0.125, 0.25, 0, headMaterial.createBlockData());
		}
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

	public static Set<TempArmorStand> getFromAbility(CoreAbility ability) {
		return instances.values().stream().filter(tas -> tas.getAbility().equals(ability)).collect(Collectors.toSet());
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
		showParticles(particles);
		armorStand.remove();
	}

	public static void removeAll() {
		tasQueue.clear();
		instances.keySet().forEach(Entity::remove);
		instances.clear();
	}
}
