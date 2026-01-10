package me.moros.hyperion.util;

import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.util.ParticleEffect;

import com.projectkorra.projectkorra.util.TempBlock;
import me.moros.hyperion.Hyperion;
import me.moros.hyperion.methods.CoreMethods;

import org.bukkit.Location;
import org.bukkit.Material;

import org.bukkit.entity.ArmorStand;

import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;


import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TempArmorStand {

	private static final Plugin PLUGIN = Hyperion.getPlugin();
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
		this.headMaterial = material;
		this.ability = abilityInstance;
		this.expirationTime = System.currentTimeMillis() + delay;
		this.particles = showRemoveParticles;

		this.armorStand = location.getWorld().spawn(location, ArmorStand.class, entity -> {
			entity.setInvulnerable(true);
			entity.setVisible(false);
			entity.setGravity(false);
			entity.getEquipment().setHelmet(new ItemStack(headMaterial));
			entity.setMetadata(CoreMethods.NO_INTERACTION_KEY, new FixedMetadataValue(PLUGIN, ""));
		});

		instances.put(armorStand, this);
		tasQueue.add(this);
		showParticles(true);
	}

	public void showParticles(boolean show) {
		if (!show) return;

		Location loc = armorStand.getEyeLocation().add(0, 0.2, 0);
		ParticleEffect.BLOCK_CRACK.display(loc, 4, 0.25, 0.125, 0.25, 0, headMaterial.createBlockData());
		ParticleEffect.BLOCK_DUST.display(loc, 6, 0.25, 0.125, 0.25, 0, headMaterial.createBlockData());
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

	public boolean isExpired() {
		return System.currentTimeMillis() > expirationTime;
	}

	public static boolean isTempArmorStand(ArmorStand as) {
		return instances.containsKey(as);
	}

	public static TempArmorStand get(ArmorStand as) {
		return instances.get(as);
	}

	public static Set<TempArmorStand> getFromAbility(CoreAbility ability) {
		return instances.values().stream()
				.filter(tas -> tas.getAbility().equals(ability))
				.collect(Collectors.toSet());
	}

	public static void manage() {
		final long currentTime = System.currentTimeMillis();

		while (!tasQueue.isEmpty()) {
			final TempArmorStand tas = tasQueue.peek();

			if (tas == null || currentTime < tas.getExpirationTime()) {
				return;
			}

			tasQueue.poll();

			if (tas.armorStand == null || !tas.armorStand.isValid()) {
				instances.remove(tas.armorStand);
				continue;
			}


			Location loc = tas.armorStand.getLocation();
			ThreadUtil.ensureLocation(loc, tas::remove);
		}
	}

	public void remove() {
		if (armorStand != null && armorStand.isValid()) {
			showParticles(particles);
			armorStand.remove();
		}
		instances.remove(armorStand);
	}

	public static void removeAll() {
		tasQueue.clear();
		instances.keySet().forEach(entity -> {
			if (entity != null && entity.isValid()) {
				entity.remove();
			}
		});
		instances.clear();
	}
}
