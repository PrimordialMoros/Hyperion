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

package me.moros.hyperion;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

public class PersistentDataLayer {
	public static final String STR_EARTHGUARD_ITEM = "earthguard_armor";
	public static final String STR_LOCKSMITHING_ITEM = "locksmithing_key";

	private static final byte VALUE = (byte) 0x1;

	private final NamespacedKey NSK_EARTHGUARD;
	private final NamespacedKey NSK_LOCKSMITHING;

	protected PersistentDataLayer() {
		NSK_EARTHGUARD = new NamespacedKey(Hyperion.getPlugin(), STR_EARTHGUARD_ITEM);
		NSK_LOCKSMITHING = new NamespacedKey(Hyperion.getPlugin(), STR_LOCKSMITHING_ITEM);
	}

	public boolean hasEarthGuardKey(PersistentDataHolder holder) {
		return hasKey(holder, NSK_EARTHGUARD);
	}

	public boolean hasLocksmithingKey(PersistentDataHolder holder) {
		return hasKey(holder, NSK_LOCKSMITHING);
	}

	public boolean hasKey(PersistentDataHolder holder, NamespacedKey key) {
		return holder != null && holder.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
	}

	public void addEarthGuardKey(PersistentDataHolder holder) {
		addKey(holder, NSK_EARTHGUARD);
	}

	public void addLockSmithingKey(PersistentDataHolder holder) {
		addKey(holder, NSK_LOCKSMITHING);
	}

	public void addKey(PersistentDataHolder holder, NamespacedKey key) {
		if (holder != null && !hasKey(holder, key)) {
			holder.getPersistentDataContainer().set(key, PersistentDataType.BYTE, VALUE);
		}
	}
}
