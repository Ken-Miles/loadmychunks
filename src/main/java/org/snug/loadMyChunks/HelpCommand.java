package org.snug.loadMyChunks;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.plugin.Plugin;

public class HelpCommand {
    public static LoadMyChunksPlugin plugin;

    public static LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("loadmyfarms")
                .requires(source -> source.getSender().hasPermission("loadmyfarms.use"))
                .then(Commands.literal("help")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();

                            String message = "LoadMyFarms Help:";

                            for (Command command : plugin.getRegisteredCommands()) {

                            }
                        })
                ).build();
    }

}
