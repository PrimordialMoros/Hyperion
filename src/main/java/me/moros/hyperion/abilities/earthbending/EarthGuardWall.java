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

package me.moros.hyperion.abilities.earthbending;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.earthbending.Collapse;
import com.projectkorra.projectkorra.earthbending.RaiseEarth;
import com.projectkorra.projectkorra.util.ParticleEffect;
import me.moros.hyperion.Hyperion;
import me.moros.hyperion.methods.CoreMethods;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class EarthGuardWall extends EarthAbility implements AddonAbility {
	private final Set<Block> pillars = new HashSet<>();

	@Attribute(Attribute.DURATION)
	private long wallDuration;
	@Attribute(Attribute.COOLDOWN)
	private long wallCooldown;

	public EarthGuardWall(Player player) {
		super(player);

		if (bPlayer.isOnCooldown(this) || !bPlayer.canBendIgnoreCooldowns(getAbility(EarthGuard.class))) {
			return;
		}

		wallDuration = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.EarthGuard.WallDuration");
		wallCooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Earth.EarthGuard.WallCooldown");

		if (raiseSmallWall()) {
			start();
			bPlayer.addCooldown(this);
		}
	}

	@Override
	public void progress() {
		if (!bPlayer.canBendIgnoreBindsCooldowns(getAbility(EarthGuard.class))) {
			remove();
			return;
		}

		if (System.currentTimeMillis() > getStartTime() + wallDuration) {
			remove();
		}
	}

	private boolean raiseSmallWall() {
		final Block source = getTargetEarthBlock(4);
		if (source == null || source.getLocation().distanceSquared(player.getLocation()) <= 1.5 * 1.5) {
			return false;
		}

		final Vector direction = player.getEyeLocation().getDirection().clone().setY(0).normalize();
		direction.setX(NumberConversions.round(direction.getX()));
		direction.setZ(NumberConversions.round(direction.getZ()));
		final BlockFace blockFace = CoreMethods.getLeftBlockFace(GeneralMethods.getCardinalDirection(direction));

		Set<Block> testBlocks = new HashSet<>(3);
		testBlocks.add(source);
		testBlocks.add(source.getRelative(blockFace));
		testBlocks.add(source.getRelative(blockFace.getOppositeFace()));

		for (Block testBlock : testBlocks) {
			if (isTransparent(testBlock) && isEarthbendable(testBlock.getRelative(BlockFace.DOWN))) {
				pillars.add(testBlock.getRelative(BlockFace.DOWN));
			} else if (isEarthbendable(testBlock.getRelative(BlockFace.UP)) && isTransparent(testBlock.getRelative(BlockFace.UP, 2))) {
				pillars.add(testBlock.getRelative(BlockFace.UP));
			} else if (isEarthbendable(testBlock)) {
				pillars.add(testBlock);
			}
		}

		for (Block b : pillars) {
			RaiseEarth pillar = new RaiseEarth(player, b.getLocation(), 2);
			pillar.setCooldown(0);
			pillar.setSpeed(10);
		}

		return !pillars.isEmpty();
	}

	@Override
	public boolean isEnabled() {
		return Hyperion.getPlugin().getConfig().getBoolean("Abilities.Earth.EarthGuard.Enabled");
	}

	@Override
	public String getName() {
		return "EarthGuardWall";
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
	public boolean isHiddenAbility() {
		return true;
	}

	@Override
	public boolean isSneakAbility() {
		return true;
	}

	@Override
	public long getCooldown() {
		return wallCooldown;
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
		for (Block pillarOrigin : pillars) {
			Block particleBlock = pillarOrigin.getRelative(BlockFace.UP, 2);
			if (isEarthbendable(particleBlock)) {
				Location particleLocation = particleBlock.getLocation().clone().add(0.5, -0.5, 0.5);
				ParticleEffect.BLOCK_CRACK.display(particleLocation, 8, 0.2F, 0.2F, 0.2F, 1, particleBlock.getBlockData());
			}
			Collapse pillarDown = new Collapse(player, pillarOrigin.getRelative(BlockFace.UP).getLocation());
			pillarDown.setCooldown(0);
			pillarDown.setHeight(2);
			pillarDown.setSpeed(10);
		}
		super.remove();
	}
}
