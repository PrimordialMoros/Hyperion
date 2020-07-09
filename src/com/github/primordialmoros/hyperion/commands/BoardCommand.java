/*
 *   Copyright 2016, 2017, 2020 Moros <https://github.com/PrimordialMoros>
 *
 * 	  This file is part of Hyperion.
 *
 *    Hyperion is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    Hyperion is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with Hyperion.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.primordialmoros.hyperion.commands;

import com.github.primordialmoros.hyperion.board.BendingBoardManager;
import com.projectkorra.projectkorra.command.PKCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class BoardCommand extends PKCommand {
	public BoardCommand() {
		super("board", "/bending board", "Toggle bending board visibility", new String[]{ "bendingboard", "board", "bb" });
	}

	@Override
	public void execute(CommandSender sender, List<String> args) {
		if (!isPlayer(sender) || !hasPermission(sender) || !correctLength(sender, args.size(), 0, 0)) return;
		BendingBoardManager.toggleScoreboard((Player) sender);
	}
}
