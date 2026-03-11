package dev.larrox.locatorBarRemover.util;

import dev.larrox.locatorBarRemover.LocatorBarRemover;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;

import java.util.List;

public class Gamerules {

    private final LocatorBarRemover plugin;

    public Gamerules(LocatorBarRemover plugin) {
        this.plugin = plugin;
    }

    public void apply() {
        GameRule<Boolean> gamerule = GameRule.LOCATOR_BAR;

        boolean value = plugin.getConfig().getBoolean("enable-locator-bar", false);
        List<String> ignoredWorlds = plugin.getConfig().getStringList("ignored-worlds");
        String consolePrefix = plugin.getConfig().getString("console-prefix", "[LocatorBarRemover] ");
        String consoleSuffix = plugin.getConfig().getString("console-suffix", "");

        for (World world : Bukkit.getWorlds()) {
            if (ignoredWorlds.contains(world.getName())) {
                continue;
            }

            Boolean currentValue = world.getGameRuleValue(gamerule);
            if (currentValue != null && currentValue == value) {
                plugin.getLogger().info(consolePrefix + "LocatorBar already set to " + value
                        + " in world: " + world.getName() + ", skipping." + consoleSuffix);
                continue;
            }

            world.setGameRule(gamerule, value);
            plugin.getLogger().info(consolePrefix + "Set gamerule LocatorBar=" + value
                    + " in world: " + world.getName() + consoleSuffix);
        }
    }
}
