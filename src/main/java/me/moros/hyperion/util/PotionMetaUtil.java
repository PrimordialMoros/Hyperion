package me.moros.hyperion.util;

import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.lang.reflect.Method;

public class PotionMetaUtil {

    public static PotionType getPotionType(PotionMeta meta) {
        try {
            Method getBasePotionType = PotionMeta.class.getMethod("getBasePotionType");
            return (PotionType) getBasePotionType.invoke(meta);
        } catch (NoSuchMethodException e) {
            try {
                Method getBasePotionData = PotionMeta.class.getMethod("getBasePotionData");
                Object basePotionData = getBasePotionData.invoke(meta);

                Method getType = basePotionData.getClass().getMethod("getType");
                return (PotionType) getType.invoke(basePotionData);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}