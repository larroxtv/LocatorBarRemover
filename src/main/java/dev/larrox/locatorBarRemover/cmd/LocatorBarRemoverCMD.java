package dev.larrox.locatorBarRemover.cmd;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import dev.larrox.locatorBarRemover.LocatorBarRemover;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.concurrent.CompletionException;

public class LocatorBarRemoverCMD implements CommandExecutor, TabCompleter {

    private static final String RELOAD_PERMISSION = "locatorbarremover.reload";
    private static final List<String> SUBCOMMANDS = List.of("reload", "run", "all");

    private final LocatorBarRemover plugin;
    private final CommandDispatcher<CommandSender> dispatcher;

    public LocatorBarRemoverCMD(LocatorBarRemover plugin) {
        this.plugin = plugin;
        this.dispatcher = createDispatcher();
    }

    private CommandDispatcher<CommandSender> createDispatcher() {
        CommandDispatcher<CommandSender> commandDispatcher = new CommandDispatcher<>();

        commandDispatcher.register(subcommand("reload", sender -> {
            plugin.reloadConfig();
            sender.sendMessage(withPrefix(message("reload-success", "Configuration reloaded successfully.")));
        }));

        commandDispatcher.register(subcommand("run", sender -> {
            plugin.applyGamerulesSafely();
            sender.sendMessage(withPrefix(message("applied", "LocatorBarRemover gamerules applied.")));
        }));

        commandDispatcher.register(subcommand("all", sender -> {
            plugin.reloadConfig();
            plugin.applyGamerulesSafely();
            sender.sendMessage(withPrefix(message("reload-applied", "LocatorBarRemover config reloaded and gamerules applied.")));
        }));

        return commandDispatcher;
    }

    private LiteralArgumentBuilder<CommandSender> subcommand(String name, SenderAction action) {
        return LiteralArgumentBuilder.<CommandSender>literal(name)
                .requires(this::hasPermission)
                .executes(context -> {
                    action.execute(context.getSource());
                    return 1;
                });
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!hasPermission(sender)) {
            sender.sendMessage(withPrefix(message("no-permission", "You don't have permission to use this command.")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(withPrefix(message("usage", "Usage: /%label% <reload|run|all>").replace("%label%", label)));
            return true;
        }

        try {
            dispatcher.execute(String.join(" ", args), sender);
        } catch (CommandSyntaxException exception) {
            sender.sendMessage(withPrefix(message("invalid-subcommand", "Invalid subcommand. Available: " + String.join(", ", SUBCOMMANDS))));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command, @NonNull String alias, String @NonNull [] args) {
        if (!hasPermission(sender)) {
            return List.of();
        }

        String input = String.join(" ", args);
        ParseResults<CommandSender> parseResults = dispatcher.parse(input, sender);

        try {
            Suggestions suggestions = dispatcher.getCompletionSuggestions(parseResults).join();
            return suggestions.getList().stream().map(Suggestion::getText).toList();
        } catch (CompletionException exception) {
            return List.of();
        }
    }

    private boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(RELOAD_PERMISSION);
    }

    private String message(String key, String fallback) {
        return plugin.getConfig().getString("messages." + key, fallback);
    }

    private String withPrefix(String value) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        return prefix + value;
    }

    @FunctionalInterface
    private interface SenderAction {
        void execute(CommandSender sender);
    }
}
