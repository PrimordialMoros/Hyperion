package me.moros.hyperion;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class PersistentDataLayer {
	public static final String STR_EARTHGUARD_ITEM = "earthguard_armor";

	private static final byte VALUE = (byte) 0x1;

	private final NamespacedKey NSK_EARTHGUARD;

	protected PersistentDataLayer() {
		NSK_EARTHGUARD = new NamespacedKey(Hyperion.getPlugin(), STR_EARTHGUARD_ITEM);
	}

	public boolean hasEarthGuardKey(PersistentDataContainer container) {
		return container != null && container.has(NSK_EARTHGUARD, PersistentDataType.BYTE);
	}

	public void addEarthGuardKey(PersistentDataContainer container) {
		if (container != null && !hasEarthGuardKey(container)) {
			container.set(NSK_EARTHGUARD, PersistentDataType.BYTE, VALUE);
		}
	}
}
