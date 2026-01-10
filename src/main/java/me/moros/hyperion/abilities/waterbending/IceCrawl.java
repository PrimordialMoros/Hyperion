package me.moros.hyperion.abilities.waterbending;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.IceAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.MovementHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import com.projectkorra.projectkorra.util.TempPotionEffect;
import com.projectkorra.projectkorra.waterbending.ice.PhaseChange;
import me.moros.hyperion.Hyperion;
import me.moros.hyperion.methods.CoreMethods;
import me.moros.hyperion.util.BendingFallingBlock;
import me.moros.hyperion.util.TempArmorStand;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class IceCrawl extends IceAbility implements AddonAbility {

	private Location location;
	private Location endLocation;
	private LivingEntity target;
	private Vector direction;
	private Block sourceBlock;

	@Attribute(Attribute.DAMAGE)
	private double damage;
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.RANGE)
	private int range;
	@Attribute(Attribute.SELECT_RANGE)
	private int selectRange;
	@Attribute(Attribute.DURATION)
	private long duration;
	@Attribute("RegenDelay")
	private long iceDuration;

	private boolean launched;
	private boolean locked;

	private final List<TempArmorStand> armorStands = new ArrayList<>();
	private final List<TempBlock> tempBlocks = new ArrayList<>();

	public IceCrawl(Player player) {
		super(player);

		if (!bPlayer.canBend(this)) return;
		if (hasAbility(player, IceCrawl.class)) {
			getAbility(player, IceCrawl.class).prepare();
			return;
		}

		damage = Hyperion.getPlugin().getConfig().getDouble("Abilities.Water.IceCrawl.Damage");
		cooldown = Hyperion.getPlugin().getConfig().getLong("Abilities.Water.IceCrawl.Cooldown");
		range = Hyperion.getPlugin().getConfig().getInt("Abilities.Water.IceCrawl.Range");
		selectRange = Hyperion.getPlugin().getConfig().getInt("Abilities.Water.IceCrawl.SelectRange");
		iceDuration = Hyperion.getPlugin().getConfig().getLong("Abilities.Water.IceCrawl.IceDuration");
		duration = Hyperion.getPlugin().getConfig().getLong("Abilities.Water.IceCrawl.FreezeDuration");

		range = (int) getNightFactor(range, player.getWorld());
		selectRange = (int) getNightFactor(selectRange, player.getWorld());
		duration = (long) getNightFactor(duration, player.getWorld());

		launched = false;

		if (prepare()) {
			start();
		}
	}

	@Override
	public void progress() {
		if (launched) {
			if (!bPlayer.canBendIgnoreBindsCooldowns(this)) {
				remove();
				return;
			}
			if (locked) {
				if (target == null || !target.isValid()
						|| (target instanceof Player && !((Player) target).isOnline())
						|| !target.getWorld().equals(location.getWorld())) {
					locked = false;
				} else if (target.getLocation().distanceSquared(endLocation) < 25) {
					endLocation = target.getLocation().clone();
					direction = CoreMethods.calculateFlatVector(sourceBlock.getLocation(), endLocation);
				} else {
					locked = false;
				}
			}
			advanceLocation();
			checkDamage();
			if (ThreadLocalRandom.current().nextInt(5) == 0) playIcebendingSound(location);
			cleanupArmorStands();
		} else {
			if (!bPlayer.canBendIgnoreCooldowns(this)
					|| sourceBlock.getLocation().distanceSquared(player.getLocation()) > Math.pow(selectRange + 5, 2)) {
				remove();
				return;
			}
			if (isWater(sourceBlock) || isIce(sourceBlock)) {
				playFocusWaterEffect(sourceBlock);
			} else {
				remove();
			}
		}
	}

	private void advanceLocation() {
		if (isLava(location.getBlock())) {
			CoreMethods.playExtinguishEffect(location.clone().add(0, 0.2, 0), 8);
			remove();
			return;
		}

		Block down = location.getBlock().getRelative(BlockFace.DOWN);
		if (isWater(down)) {
			TempBlock tb = new TempBlock(down, Material.ICE.createBlockData(), iceDuration);
			PhaseChange.getFrozenBlocksMap().put(tb, player);
			tempBlocks.add(tb);  // Track the temporary ice block

			// Debugging info
			System.out.println("TempBlock added: " + tb.getBlock().getLocation());
		}

		double x = ThreadLocalRandom.current().nextDouble(-0.125, 0.125);
		double z = ThreadLocalRandom.current().nextDouble(-0.125, 0.125);
		TempArmorStand stand = new TempArmorStand(this, location.clone().add(x, -2, z), Material.PACKED_ICE, 1400);
		armorStands.add(stand);

		location.add(direction.clone().multiply(0.7));

		Block base = location.getBlock().getRelative(BlockFace.DOWN);
		if (!isValidBlock(base)) {
			if (isValidBlock(base.getRelative(BlockFace.UP))) {
				location.add(0, 1, 0);
			} else if (isValidBlock(base.getRelative(BlockFace.DOWN))) {
				location.add(0, -1, 0);
			} else {
				remove();
				return;
			}
		} else if (endLocation.getBlockY() != location.getBlockY()) {
			if (endLocation.getBlockY() > location.getBlockY()
					&& isValidBlock(base.getRelative(BlockFace.UP))) {
				location.add(0, 1, 0);
			} else if (endLocation.getBlockY() < location.getBlockY()
					&& isValidBlock(base.getRelative(BlockFace.DOWN))) {
				location.add(0, -1, 0);
			}
		}

		if (RegionProtection.isRegionProtected(this, location)
				|| location.distanceSquared(sourceBlock.getLocation()) > range * range) {
			remove();
		}
	}

	private void cleanupArmorStands() {
		Iterator<TempArmorStand> it = armorStands.iterator();
		while (it.hasNext()) {
			TempArmorStand stand = it.next();
			if (stand.isExpired()) {
				stand.remove();
				it.remove();
			}
		}
	}

	private boolean isValidBlock(Block block) {
		if (!isTransparent(block.getRelative(BlockFace.UP))) return false;
		return isWater(block) || isIce(block) || GeneralMethods.isSolid(block);
	}

	private void checkDamage() {
		boolean hit = false;
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 0.8)) {
			if (entity instanceof LivingEntity && entity.getEntityId() != player.getEntityId()
					&& !(entity instanceof ArmorStand)) {

				if (entity instanceof Player && Commands.invincible.contains(entity.getName())) {
					continue;
				}

				DamageHandler.damageEntity(entity, getNightFactor(damage, player.getWorld()), this);
				if (entity.isValid()) {
					MovementHandler mh = new MovementHandler((LivingEntity) entity,
							CoreAbility.getAbility(IceCrawl.class));
					mh.stopWithDuration(duration / 50,
							Element.ICE.getColor() + "* Frozen *");
					new BendingFallingBlock(entity.getLocation().clone().add(0, -0.2, 0),
							Material.PACKED_ICE.createBlockData(),
							new Vector(), this, false, duration);
					new TempPotionEffect((LivingEntity) entity, Hyperion.plugin.getPotionEffectAdapter().getSlownessEffect(NumberConversions.round(duration), 5));
				}

				hit = true;
			}
		}

		if (hit) remove();
	}

	public boolean prepare() {
		if (launched) return false;
		Block block = getIceSourceBlock(player, selectRange);
		if (block == null) {
			block = getWaterSourceBlock(player, selectRange, false);
		}
		if (block == null
				|| (!isWater(block) && !isIce(block))
				|| !isTransparent(block.getRelative(BlockFace.UP))) {
			if (isStarted()) remove();
			return false;
		}
		sourceBlock = block;
		playFocusWaterEffect(sourceBlock);
		location = sourceBlock.getLocation();
		return true;
	}

	@Override
	public void remove() {
		super.remove();

		// Cleanup Armor Stands
		for (TempArmorStand stand : armorStands) {
			stand.remove();
		}
		armorStands.clear();

		// Revert all temporary ice blocks
		for (TempBlock tb : tempBlocks) {
			tb.revertBlock();  // This should remove the ice block
		}
		tempBlocks.clear();

		// Additional debugging log
		System.out.println("IceCrawl ability removed, ice blocks reverted.");
	}

	@Override public boolean isEnabled() {
		return Hyperion.getPlugin().getConfig()
				.getBoolean("Abilities.Water.IceCrawl.Enabled");
	}

	@Override public String getName() { return "IceCrawl"; }
	@Override public String getDescription() {
		return Hyperion.getPlugin().getConfig()
				.getString("Abilities.Water.IceCrawl.Description");
	}
	@Override public String getAuthor() { return Hyperion.getAuthor(); }
	@Override public String getVersion() { return Hyperion.getVersion(); }
	@Override public boolean isHarmlessAbility() { return false; }
	@Override public boolean isSneakAbility() { return true; }
	@Override public long getCooldown() { return cooldown; }
	@Override public Location getLocation() { return location; }
	@Override public boolean isCollidable() { return launched; }
	@Override public double getCollisionRadius() { return 0.8; }
	@Override public void load() {}
	@Override public void stop() {}

	public static void shootLine(Player player) {
		if (hasAbility(player, IceCrawl.class)) {
			getAbility(player, IceCrawl.class).shootLine();
		}
	}

	private void shootLine() {
		if (launched) return;
		Entity ent = GeneralMethods.getTargetedEntity(player, range + selectRange,
				Collections.singletonList(player));
		if (ent instanceof LivingEntity
				&& ent.getLocation().distanceSquared(location) <= range * range) {
			locked = true;
			target = (LivingEntity) ent;
			endLocation = target.getLocation().clone();
		} else {
			endLocation = GeneralMethods.getTargetedLocation(player, range, Material.WATER);
		}
		location = sourceBlock.getLocation().clone().add(0.5, 1.25, 0.5);
		direction = CoreMethods.calculateFlatVector(location, endLocation);
		launched = true;
		playIcebendingSound(sourceBlock.getLocation());
		bPlayer.addCooldown(this);
	}
}
