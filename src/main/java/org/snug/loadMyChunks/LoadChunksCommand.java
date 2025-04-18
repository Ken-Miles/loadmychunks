package org.snug.loadMyChunks;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static net.kyori.adventure.text.Component.text;

public class LoadChunksCommand {

    public static LoadMyChunksPlugin plugin = null;


    public static LiteralCommandNode<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("loadmyfarms")
                .requires(source -> source.getSender().hasPermission("loadmyfarms.use"));

        // /loadmyfarms load
        root.then(Commands.literal("load")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    loadChunksAroundPlayer(source);
                    source.getExecutor().sendMessage("Chunks loaded!");
                    return 1;
                })
        );

        // /loadmyfarms list
        root.then(Commands.literal("list")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    listChunks(source, 1); // default to page 1
                    return 1;
                })
        );

        // /loadmyfarms unload
        root.then(Commands.literal("unload")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    unloadChunksAroundPlayer(source);
                    source.getExecutor().sendMessage("Chunks unloaded!");
                    return 1;
                })
        );

        return root.build();
    }

    private int getTotalPages() {
        int entries = this.plugin.getTicketedChunks().size();
        int entriesPerPage = 10;
        return Math.max(1, (entries + entriesPerPage - 1) / entriesPerPage); // ceiling division
    }

    // i need this in loadchunkscommands
//    public CompletableFuture<Suggestion> suggestPages(CommandContext<Object> context, SuggestionsBuilder builder) {
//        int totalPages = getTotalPages();
//        for (int i = 1; i <= totalPages; i++) {
//            builder.suggest(Integer.toString(i));
//        }
//        return CompletableFuture.completedFuture(builder);
//    }

    private static void loadChunksAroundPlayer(CommandSourceStack stack) {
        Entity executor = stack.getExecutor();
        Player player = executor instanceof Player ? (Player) executor : null;

        if (player == null) {
            stack.getSender().sendMessage(text("Only players can run this command!"));
            return;
        }

        Location location = player.getLocation();
        Chunk centerChunk = location.getChunk();
        int centerX = centerChunk.getX();
        int centerZ = centerChunk.getZ();
        World world = centerChunk.getWorld();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Chunk chunk = world.getChunkAt(centerX + dx, centerZ + dz);

                if (!plugin.isChunkLoaded(chunk)) {
                    chunk.addPluginChunkTicket(plugin);
                    plugin.addTicketedChunk(chunk, executor.getUniqueId(), player != null);
                    plugin.getLogger().info("Loading chunk at (" + chunk.getX() + ", " + chunk.getZ() + ") in " + world.getName());
                    stack.getExecutor().sendMessage(text(
                            "Loaded chunk at (" + chunk.getX() + ", " + chunk.getZ() + ") in " + world.getName()
                    ));
                } else {
                    plugin.getLogger().info("Chunk (" + chunk.getX() + ", " + chunk.getZ() + ") is already loaded.");
                    stack.getExecutor().sendMessage(text(
                            "Chunk (" + chunk.getX() + ", " + chunk.getZ() + ") is already loaded."
                    ));
                }
            }
        }
    }

    private static void unloadChunksAroundPlayer(CommandSourceStack stack) {
        Entity executor = stack.getExecutor();
        Player player = executor instanceof Player ? (Player) executor : null;

        if (player == null) {
            stack.getSender().sendMessage(text("Only players can run this command!"));
            return;
        }

        Location location = player.getLocation();
        Chunk centerChunk = location.getChunk();
        int centerX = centerChunk.getX();
        int centerZ = centerChunk.getZ();
        World world = centerChunk.getWorld();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Chunk chunk = world.getChunkAt(centerX + dx, centerZ + dz);

                if (plugin.isChunkLoaded(chunk)) {
                    chunk.removePluginChunkTicket(plugin);
                    plugin.removeTicketedChunk(chunk);
                    plugin.getLogger().info("Unloading chunk at (" + chunk.getX() + ", " + chunk.getZ() + ") in " + world.getName());
                    stack.getSender().sendMessage(text(
                            "Unloaded chunk at (" + chunk.getX() + ", " + chunk.getZ() + ") in " + world.getName()
                    ));
                } else {
                    plugin.getLogger().info("Chunk (" + chunk.getX() + ", " + chunk.getZ() + ") is not loaded.");
                    stack.getSender().sendMessage(text(
                            "Chunk (" + chunk.getX() + ", " + chunk.getZ() + ") is not loaded."
                    ));
                }
            }
        }
    }

    private static void listChunks(CommandSourceStack stack, int pageNumber) {
        Set<TicketedChunk> chunks = plugin.getTicketedChunks();

        int entriesPerPage = 5;
        int totalChunks = chunks.size();
        int totalPages = (int) Math.ceil((double) totalChunks / entriesPerPage);

        if (totalChunks == 0) {
            stack.getSender().sendMessage(text("No chunks are currently loaded."));
            return;
        }

        if (pageNumber < 1 || pageNumber > totalPages) {
            stack.getSender().sendMessage(text("Invalid page. Must be between 1 and " + totalPages + "."));
            return;
        }

        stack.getSender().sendMessage(text("Loaded Chunks (Page " + pageNumber + "/" + totalPages + "):"));

        List<TicketedChunk> chunkList = new ArrayList<>(chunks);

        int start = (pageNumber - 1) * entriesPerPage;
        int end = Math.min(start + entriesPerPage, totalChunks);

        for (int i = start; i < end; i++) {
            TicketedChunk ticket = chunkList.get(i);
            Chunk chunk = ticket.getChunk();
            long now = System.currentTimeMillis();
            long diff = now - ticket.getTimestamp();
            String ago = formatDuration(diff);

            String msg = String.format(
                    "World: %s Chunk: (%d, %d) Player: %s Ticketed: %s ago",
                    chunk.getWorld().getName(), chunk.getX(), chunk.getZ(), ticket.getPlayerUUID(), ago
            );
            stack.getExecutor().sendMessage(text(msg));
        }
    }


    private static String formatDuration(long millis) {
        long seconds = millis / 1000;
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h";
        long days = hours / 24;
        return days + "d";
    }
}
