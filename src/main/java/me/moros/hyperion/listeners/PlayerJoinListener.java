package me.moros.hyperion.listeners;

import me.moros.hyperion.Hyperion;
import me.moros.hyperion.util.ThreadUtil;
import me.moros.hyperion.util.UpdateChecker;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import static me.moros.hyperion.Hyperion.plugin;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.isOp()) return;

        UpdateChecker checker = Hyperion.getUpdateChecker();
        if (checker == null) {
            plugin.adventure().player(player).sendMessage(
                    Component.text("[Hyperion] ", TextColor.color(0xFFA500))
                            .append(Component.text("Update checker not initialized.", NamedTextColor.RED))
            );
            return;
        }

        if (!checker.hasChecked()) {
            plugin.adventure().player(player).sendMessage(
                    Component.text("[Hyperion] ", TextColor.color(0xFFA500))
                            .append(
                                    Component.text("Checking for updates...", NamedTextColor.GRAY)
                            )
            );
            return;
        }

        if (checker.isUpdateAvailable()) {
            String current = checker.getCurrentVersion() != null ? checker.getCurrentVersion() : "unknown";
            String latest = checker.getLatestVersion() != null ? checker.getLatestVersion() : "unknown";
            var audience = Hyperion.plugin.adventure().player(player);

            audience.sendMessage(
                    Component.text("[Hyperion] ", TextColor.fromHexString("#FFA500"))
                            .append(
                                    Component.text("A new version is available!",
                                                    TextColor.fromHexString("#FFFF00"))
                                            .decorate(TextDecoration.UNDERLINED)
                            )
            );

            audience.sendMessage(
                    Component.text("You're running: ", NamedTextColor.GRAY)
                            .append(Component.text(current, NamedTextColor.RED))
            );

            audience.sendMessage(
                    Component.text("Latest version: ", NamedTextColor.GRAY)
                            .append(Component.text(latest, NamedTextColor.GREEN))
            );

            audience.sendMessage(
                    Component.text("Download: ", NamedTextColor.GRAY)
                            .append(
                                    Component.text("https://github.com/Hihelloy-main/Hyperion",
                                                    NamedTextColor.BLUE)
                                            .decorate(TextDecoration.UNDERLINED)
                                            .clickEvent(ClickEvent.openUrl("https://github.com/Hihelloy-main/Hyperion"))
                            )
            );

        } else {
            String latest = checker.getLatestVersion() != null ? checker.getLatestVersion() : "unknown";
            var audience = Hyperion.plugin.adventure().player(player);

            audience.sendMessage(
                    Component.text("[Hyperion] ", TextColor.fromHexString("#FFA500"))
                            .append(
                                    Component.text(
                                            "No updates available. You're on the latest version.",
                                            NamedTextColor.GREEN
                                    )
                            )
            );

            audience.sendMessage(
                    Component.text("Latest GitHub version: ", NamedTextColor.GRAY)
                            .append(Component.text(latest, NamedTextColor.GREEN))
            );
        }

    }
}
