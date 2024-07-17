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

package me.moros.hyperion.abilities.airbending;

import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import me.moros.hyperion.Hyperion;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class Evade extends AirAbility implements AddonAbility {
	private Vector direction;

	private long cooldown;

	private double angleStep;

	private int ticks = 0;

	public Evade(Player player) {
		super(player);

		if (!player.isOnGround() || player.getEyeLocation().getBlock().isLiquid() || hasAbility(player, Evade.class) || !bPlayer.canBend(this)) {
			return;
		}

		cooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Air.Evade.Cooldown");
		direction = player.getEyeLocation().getDirection().setY(0).normalize().multiply(-0.2);

		angleStep = Math.PI / 10;
		if (player.getEyeLocation().getDirection().getY() >= 0) angleStep = -angleStep;

		player.setVelocity(player.getVelocity().add(new Vector(0, 0.1, 0)));

		player.setNoDamageTicks(5);
		start();
	}

	@Override
	public void progress() {
		if (!bPlayer.canBendIgnoreCooldowns(this) || ticks > 10) {
			remove();
			return;
		}
		for (int i = 0; i < 2; i++) {
			player.setVelocity(player.getVelocity().add(direction.clone()));
			direction.rotateAroundY(angleStep);
			playAirbendingParticles(player.getLocation().add(0, 1, 0), 2, 0, 0, 0);
		}
		ticks++;
	}

	@Override
	public boolean isEnabled() {
		return Hyperion.getPlugin().getConfig().getBoolean("Abilities.Air.Evade.Enabled");
	}

	@Override
	public String getName() {
		return "Evade";
	}

	@Override
	public String getDescription() {
		return Hyperion.getPlugin().getConfig().getString("Abilities.Air.Evade.Description");
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
		return true;
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
}
