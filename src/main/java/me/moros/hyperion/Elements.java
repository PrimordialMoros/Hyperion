package me.moros.hyperion;


import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.Element.ElementType;
import com.projectkorra.projectkorra.Element.SubElement;
import com.projectkorra.projectkorra.ProjectKorra;
import me.moros.hyperion.util.HexColor;
import net.md_5.bungee.api.ChatColor;


public class Elements {

    public static final SubElement RAINBOWFIRE;

    public Elements() {
    }

    static {
        RAINBOWFIRE = new SubElement("RainbowFire", Element.FIRE, ElementType.BENDING, ProjectKorra.plugin) {
            @Override
            public ChatColor getColor() {
                return ChatColor.of("#FF7F00");
            }
        };
    }
}
