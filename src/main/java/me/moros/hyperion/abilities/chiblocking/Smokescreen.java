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

package me.moros.hyperion.abilities.chiblocking;

import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import me.moros.hyperion.Hyperion;
import me.moros.hyperion.methods.CoreMethods;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.NumberConversions;

public class Smokescreen extends ChiAbility implements AddonAbility {
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.DURATION)
	private int cloudTicks;
	@Attribute(Attribute.DURATION)
	private int blindnessTicks;
	@Attribute(Attribute.RADIUS)
	private double radius;

	public Smokescreen(Player player) {
		super(player);

		if (!bPlayer.canBend(this)) {
			return;
		}

		cooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Chi.Smokescreen.Cooldown");
		cloudTicks = NumberConversions.ceil(Hyperion.getPlugin().getConfig().getLong("Abilities.Chi.Smokescreen.CloudDuration") / 50.0);
		blindnessTicks = Hyperion.getPlugin().getConfig().getInt("Abilities.Chi.Smokescreen.BlindnessTicks");
		radius = Hyperion.getPlugin().getConfig().getDouble("Abilities.Chi.Smokescreen.Radius");

		bPlayer.addCooldown(this);
		start();
	}

	public static void createCloud(Location center, SmokescreenData data) {
		if (center.getWorld() == null) return;
		AreaEffectCloud cloud = center.getWorld().spawn(center, AreaEffectCloud.class);
		cloud.addCustomEffect(new PotionEffect(PotionEffectType.BLINDNESS, data.blindnessDuration, 2), false);
		cloud.setSource(data.source);
		cloud.setDuration(data.cloudDuration);
		cloud.setRadius(data.radius);
		cloud.setColor(Color.BLACK);
		cloud.setWaitTime(0);
		cloud.setReapplicationDelay(10);
	}

	@Override
	public void progress() {
		Snowball projectile = player.launchProjectile(Snowball.class);
		SmokescreenData data = new SmokescreenData(player, cloudTicks, blindnessTicks, (float) radius);
		projectile.setMetadata(CoreMethods.SMOKESCREEN_KEY, new FixedMetadataValue(Hyperion.getPlugin(), data));
		remove();
	}

	@Override
	public boolean isEnabled() {
		return Hyperion.getPlugin().getConfig().getBoolean("Abilities.Chi.Smokescreen.Enabled");
	}

	@Override
	public String getName() {
		return "Smokescreen";
	}

	@Override
	public String getDescription() {
		return Hyperion.getPlugin().getConfig().getString("Abilities.Chi.Smokescreen.Description");
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
		return false;
	}

	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
		return player.getLocation();
	}

	@Override
	public void load() {
	}

	@Override
	public void stop() {
	}

	@Override
	public void remove() {
		player.setNoDamageTicks(0);
		bPlayer.addCooldown(this);
		super.remove();
	}

	public static class SmokescreenData {
		private final Player source;
		private final int cloudDuration;
		private final int blindnessDuration;
		private final float radius;

		private SmokescreenData(Player source, int cloudDuration, int blindnessDuration, float radius) {
			this.source = source;
			this.cloudDuration = cloudDuration;
			this.blindnessDuration = blindnessDuration;
			this.radius = radius;
		}
	}
}
