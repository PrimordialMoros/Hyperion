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

import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BendingFallingBlock {
	private static final Map<FallingBlock, BendingFallingBlock> instances = new ConcurrentHashMap<>();
	private final FallingBlock fallingBlock;
	private final CoreAbility ability;
	private final long expirationTime;

	public BendingFallingBlock(Location location, BlockData data, Vector velocity, CoreAbility abilityInstance, boolean gravity) {
		this(location, data, velocity, abilityInstance, gravity, 30000);
	}

	public BendingFallingBlock(Location location, BlockData data, Vector velocity, CoreAbility abilityInstance, boolean gravity, long delay) {
		fallingBlock = location.getWorld().spawnFallingBlock(location, data);
		fallingBlock.setVelocity(velocity);
		fallingBlock.setGravity(gravity);
		fallingBlock.setDropItem(false);

		expirationTime = System.currentTimeMillis() + delay;
		ability = abilityInstance;
		instances.put(fallingBlock, this);
	}

	public CoreAbility getAbility() {
		return ability;
	}

	public FallingBlock getFallingBlock() {
		return fallingBlock;
	}

	public long getExpirationTime() {
		return expirationTime;
	}

	public static boolean isBendingFallingBlock(FallingBlock fb) {
		return instances.containsKey(fb);
	}

	public static BendingFallingBlock get(FallingBlock fb) {
		return instances.get(fb);
	}

	public static List<BendingFallingBlock> getFromAbility(CoreAbility ability) {
		return instances.values().stream().filter(bfb -> bfb.getAbility().equals(ability)).collect(Collectors.toList());
	}

	public static void manage() {
		Iterator<BendingFallingBlock> iterator = instances.values().iterator();
		while (iterator.hasNext()) {
			BendingFallingBlock bfb = iterator.next();
			if (System.currentTimeMillis() > bfb.getExpirationTime()) {
				bfb.getFallingBlock().remove();
				iterator.remove();
			}
		}
	}

	public void remove() {
		instances.remove(fallingBlock);
		fallingBlock.remove();
	}

	public static void removeAll() {
		instances.keySet().forEach(Entity::remove);
		instances.clear();
	}
}
