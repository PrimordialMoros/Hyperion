package me.moros.hyperion.util;

import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Firework;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BendingFallingBlock {
	private static final Map<FallingBlock, BendingFallingBlock> instances = new ConcurrentHashMap<>();
	private static final Queue<BendingFallingBlock> bfbQueue = new PriorityQueue<>(Comparator.comparingLong(BendingFallingBlock::getExpirationTime));
	private final FallingBlock fallingBlock;
	private final CoreAbility ability;
	private final long expirationTime;

	public BendingFallingBlock(Location location, BlockData data, Vector velocity, CoreAbility abilityInstance, boolean gravity) {
		this(location, data, velocity, abilityInstance, gravity, 30000);
	}

	public BendingFallingBlock(Location location, BlockData data, Vector velocity, CoreAbility abilityInstance, boolean gravity, long delay) {
		fallingBlock = Objects.requireNonNull(location.getWorld()).spawnFallingBlock(location, data);
		fallingBlock.setVelocity(velocity);
		fallingBlock.setGravity(gravity);
		fallingBlock.setDropItem(false);

		expirationTime = System.currentTimeMillis() + delay;
		ability = abilityInstance;
		instances.put(fallingBlock, this);
		bfbQueue.add(this);
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

	public static Set<BendingFallingBlock> getFromAbility(CoreAbility ability) {
		return instances.values().stream().filter(bfb -> bfb.getAbility().equals(ability)).collect(Collectors.toSet());
	}

	public static void manage() {
		final long currentTime = System.currentTimeMillis();
		while (!bfbQueue.isEmpty()) {
			final BendingFallingBlock bfb = bfbQueue.peek();
			if (currentTime > bfb.getExpirationTime()) {
				bfbQueue.poll();
				bfb.remove(); // Remove expired falling blocks
			} else {
				return;
			}
		}

		Iterator<BendingFallingBlock> iterator = instances.values().iterator();
		while (iterator.hasNext()) {
			BendingFallingBlock bfb = iterator.next();
			if (currentTime > bfb.getExpirationTime()) {
				// Safely remove the falling block if it exists
				if (bfb.getFallingBlock() != null && bfb.getFallingBlock().isValid()) {
					bfb.getFallingBlock().remove();
				}
				iterator.remove(); // Clean up the map entry after removal
			}
		}
	}

	public void remove() {
		// Ensure the falling block is valid before trying to remove it
		if (fallingBlock != null && fallingBlock.isValid()) {
			instances.remove(fallingBlock);
			fallingBlock.remove();
		} else {
			// Log or handle the case where the falling block is already removed or invalid
			System.out.println("Falling block is null or invalid, cannot remove.");
		}
	}

	public static void removeAll() {
		bfbQueue.clear();
		// Ensure entities are valid before removal
		instances.keySet().forEach(fb -> {
			if (fb != null && fb.isValid()) {
				fb.remove();
			}
		});
		instances.clear();
	}
}
