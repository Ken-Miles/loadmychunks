package org.snug.loadMyChunks;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;

public class HelpCommand {
    public static LoadMyChunksPlugin plugin;
    private static final int COMMANDS_PER_PAGE = 5;

    public static LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("help")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    sendHelpPage(source, 1);  // Default to the first page
                    return 1;
                })
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            int page = IntegerArgumentType.getInteger(context, "page");
                            sendHelpPage(source, page);
                            return 1;
                        })
                ).build();
    }

    private static void sendHelpPage(CommandSourceStack source, int page) {
        List<Command> commands = plugin.getRegisteredCommands();
        int totalCommands = commands.size();
        int totalPages = (int) Math.ceil((double) totalCommands / COMMANDS_PER_PAGE);

        if (page < 1 || page > totalPages) {
            source.getSender().sendMessage(Component.text("Invalid page number!").color(NamedTextColor.RED));
            return;
        }

        source.getSender().sendMessage(Component.text("LoadMyChunks Help - Page " + page + "/" + totalPages).color(NamedTextColor.GOLD));

        // Determine which commands to display on this page
        int startIndex = (page - 1) * COMMANDS_PER_PAGE;
        int endIndex = Math.min(startIndex + COMMANDS_PER_PAGE, totalCommands);

        for (int i = startIndex; i < endIndex; i++) {
            Command command = commands.get(i);
            String usage = command.getUsage();
            String description = command.getDescription();

            // Default to the command label if usage or description is missing
            if (usage == null || usage.isEmpty()) {
                usage = "/" + command.getLabel();
            }

            if (description == null || description.isEmpty()) {
                description = "No description provided.";
            }

            // Create the clickable component for the command
            Component clickableCommand = Component.text(usage)
                    .color(NamedTextColor.YELLOW)
                    .clickEvent(ClickEvent.runCommand("/" + command.getLabel()))
                    .append(Component.text(" - ").color(NamedTextColor.GRAY))
                    .append(Component.text(description).color(NamedTextColor.WHITE));

            // Send the clickable command
            source.getSender().sendMessage(clickableCommand);
        }

        // Show pagination info with clickable links for next/previous pages
        Component previousPage = page > 1
                ? Component.text("[Previous]").color(NamedTextColor.AQUA).clickEvent(ClickEvent.runCommand("/loadmychunks help " + (page - 1)))
                : Component.empty();

        Component nextPage = page < totalPages
                ? Component.text("[Next]").color(NamedTextColor.AQUA).clickEvent(ClickEvent.runCommand("/loadmychunks help " + (page + 1)))
                : Component.empty();

        source.getSender().sendMessage(previousPage.append(Component.text("  ").color(NamedTextColor.GRAY)).append(nextPage));
    }

    // i don't like this but it's the only way to get the command description
    public static void sendCommandDescription(CommandSourceStack source, String command, String description, String params) {
        source.getExecutor().sendMessage(Component.text("Command: /loadmychunks " + command)
                .color(NamedTextColor.YELLOW));
        source.getExecutor().sendMessage(Component.text(description).color(NamedTextColor.GRAY));
        source.getExecutor().sendMessage(Component.text("Parameters: " + params).color(NamedTextColor.AQUA));
    }

    public static void sendHelpMessage(CommandSourceStack source) {
        source.getExecutor().sendMessage(Component.text("LoadMyChunks Help").color(NamedTextColor.GOLD));
        sendCommandDescription(source, "load", "Loads chunks around the player.", "radius: The radius around the player to load chunks.");
        sendCommandDescription(source, "loadchunk", "Loads a specific chunk.", "x: The X coordinate of the chunk. z: The Z coordinate of the chunk.");
        sendCommandDescription(source, "list", "Lists all loaded chunks.", "");
        sendCommandDescription(source, "unload", "Unloads chunks around the player.", "radius: The radius around the player to unload chunks.");
    }
}
