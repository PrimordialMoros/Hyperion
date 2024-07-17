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

package me.moros.hyperion.util;

public class FastMath {
	static final double[] sin = new double[360];

	static {
		for (int i = 0; i < sin.length; i++) {
			sin[i] = Math.sin(Math.toRadians(i));
		}
	}

	public static double sin(int angle) {
		double result = sin[Math.abs(angle) % 360];
		return angle >= 0 ? result : -result;
	}

	public static double cos(int angle) {
		return sin(angle + 90);
	}
}
