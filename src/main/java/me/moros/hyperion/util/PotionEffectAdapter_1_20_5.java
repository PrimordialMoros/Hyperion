package me.moros.hyperion.util;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.Objects;

public class PotionEffectAdapter_1_20_5 implements PotionEffectAdapter {

    @Override
    public PotionType getHarmingPotionType() {
        return PotionType.valueOf("HARMING");
    }

    @Override
    public PotionEffect getSlownessEffect(int duration, int strength) {
        return new PotionEffect(Objects.requireNonNull(PotionEffectType.getByName("SLOWNESS")), duration / 50, strength - 1);
    }

    @Override
    public PotionEffect getResistanceEffect(int duration, int strength) {
        return new PotionEffect(Objects.requireNonNull(PotionEffectType.getByName("RESISTANCE")), duration / 50, strength - 1);
    }

    @Override
    public PotionEffect getNauseaEffect(int duration) {
        return new PotionEffect(Objects.requireNonNull(PotionEffectType.getByName("NAUSEA")), duration / 50, 1);
    }

    @Override
    public void applyJumpBoost(Player player, int duration, int strength) {
        if (player.hasPotionEffect(Objects.requireNonNull(PotionEffectType.getByName("JUMP_BOOST")))) {
            player.removePotionEffect(Objects.requireNonNull(PotionEffectType.getByName("JUMP_BOOST")));
        }
        player.addPotionEffect(new PotionEffect(Objects.requireNonNull(PotionEffectType.getByName("JUMP_BOOST")), duration / 50, strength - 1));
    }

    @Override
    public boolean hasWaterPotion(Inventory inventory) {
        if (inventory.contains(Material.POTION)) {
            ItemStack item = inventory.getItem(inventory.first(Material.POTION));
            if (item == null) return false;
            PotionMeta meta = (PotionMeta) item.getItemMeta();
            if (meta == null) return false;

            PotionType potionType = PotionMetaUtil.getPotionType(meta);
            return potionType == PotionType.WATER;
        }
        return false;
    }
}