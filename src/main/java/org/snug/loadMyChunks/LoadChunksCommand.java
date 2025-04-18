package org.snug.loadMyChunks;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LoadChunksCommand {

    public static LoadMyChunksPlugin plugin = null;


    public static LiteralCommandNode<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("loadmychunks")
                //.then(Commands.literal("lmc") // Add alias for loadmychunks command
                        .requires(source -> source.getSender().hasPermission("loadmychunks.use"))
                        .executes(context -> {
                            HelpCommand.sendHelpMessage(context.getSource());
                            return 1;
                        })
                .requires(source -> source.getSender().hasPermission("loadmychunks.use"))
                .executes(context -> {
                    HelpCommand.sendHelpMessage(context.getSource());
                    return 1;
                }
        //  )
                );

        // /loadmychunks load
        root.then(Commands.literal("load")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            HelpCommand.sendCommandDescription(source, "load", "Loads chunks around you.", "radius: The radius around the player to load chunks. Enter 0 to only load the chunk you're in.");
                            return 1;
                        })
                        .then(Commands.argument("radius", IntegerArgumentType.integer(0)) // minimum radius 1
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();
                                    int radius = IntegerArgumentType.getInteger(context, "radius");
                                    loadChunksAroundPlayer(source, radius);
                                    if (radius >= 1) {
                                        source.getExecutor().sendMessage(Component.text("Chunks loaded in radius: " + radius + " (total " + (radius * 2 + 1) * (radius * 2 + 1) + " chunks)").color(NamedTextColor.GREEN));
                                    }
                                    return 1;
                                })
                        )
        );

        // /loadmychunks loadchunk and /lmc loadchunk
        root.then(Commands.literal("loadchunk")
                //.then(Commands.literal("lc") // Add alias for loadchunk command
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            HelpCommand.sendCommandDescription(source, "loadchunk", "Loads a specific chunk.", "x: The X coordinate of the chunk. z: The Z coordinate of the chunk.");
                            return 1;
                        })
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                        .executes(context -> {
                                            CommandSourceStack source = context.getSource();
                                            int x = IntegerArgumentType.getInteger(context, "x");
                                            int z = IntegerArgumentType.getInteger(context, "z");
                                            loadSpecificChunk(source, x, z);
                                            //source.getExecutor().sendMessage(Component.text("Loaded chunk at (" + x + ", " + z + ")").color(NamedTextColor.GREEN));
                                            return 1;
                                        })
                                )
                        )
                //)
        );

        // /loadmychunks loadchunks and /lmc loadchunks
        root.then(Commands.literal("loadchunks")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            HelpCommand.sendCommandDescription(source, "loadchunks", "Loads chunks around the specified coordinates.", "radius: The radius around the specified coordinates to load chunks.");
                            return 1;
                        })
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                        .then(Commands.argument("radius", IntegerArgumentType.integer(1)) // minimum radius 1
                                                .executes(context -> {
                                                    CommandSourceStack source = context.getSource();
                                                    int x = IntegerArgumentType.getInteger(context, "x");
                                                    int z = IntegerArgumentType.getInteger(context, "z");
                                                    int radius = IntegerArgumentType.getInteger(context, "radius");
                                                    loadChunksAt(source, x, z, radius);
                                                    source.getExecutor().sendMessage(Component.text("Loaded chunks at (" + x + ", " + z + ") with radius: " + radius).color(NamedTextColor.GREEN));
                                                    return 1;
                                                })
                                        )
                                )
                        )
        );

        // /loadmychunks list and /lmc list
        root.then(Commands.literal("list")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            listChunks(source, 1); // default to page 1
                            return 1;
                        })

        );

        // /loadmychunks unload and /lmc unload
        root.then(Commands.literal("unload")
                //.then(Commands.literal("ulc") // Add alias for unload command
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            HelpCommand.sendCommandDescription(source, "unload", "Unloads chunks around you.", "radius: The radius around the player to unload chunks. Enter 0 to only unload the chunk you're in.");
                            return 1;
                        })
                        .then(Commands.argument("radius", IntegerArgumentType.integer(0)) // minimum radius 1
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();
                                    int radius = IntegerArgumentType.getInteger(context, "radius");
                                    unloadChunksAroundPlayer(source, radius);
                                    if (radius >= 1) {
                                        source.getExecutor().sendMessage(Component.text("Chunks unloaded in radius: " + radius + " (total " + (radius * 2 + 1) * (radius * 2 + 1) + " chunks)").color(NamedTextColor.RED));
                                    }
                                    return 1;
                                })
                        )
                //)
        );

        root.then(Commands.literal("unloadchunk")
                //.then(Commands.literal("ulc") // Add alias for unloadchunks command
                        .executes(
                                context -> {
                                    CommandSourceStack source = context.getSource();
                                    HelpCommand.sendCommandDescription(source, "unloadchunk", "Unloads a specific chunk.", "x: The X coordinate of the chunk. z: The Z coordinate of the chunk.");
                                    return 1;
                                })
                        )
                        .then(Commands.argument("x", IntegerArgumentType.integer()))
                        .then(Commands.argument("z", IntegerArgumentType.integer()))

                        .executes(context -> {
                            CommandSourceStack source = context.getSource();

                            int x = IntegerArgumentType.getInteger(context, "x");
                            int z = IntegerArgumentType.getInteger(context, "z");

                            unloadSpecificChunk(source, x, z);
                            //source.getExecutor().sendMessage(Component.text("Unloaded chunk at (" + x + ", " + z + ")").color(NamedTextColor.RED));
                            return 1;
                        }
                //)
        );

        root.then(Commands.literal("unloadchunks")
                .executes(
                        context -> {
                            CommandSourceStack source = context.getSource();
                            HelpCommand.sendCommandDescription(source, "unloadchunks", "Unloads chunks around the specified coordinates.", "radius: The radius around the specified coordinates to unload chunks.");
                            return 1;
                        })
                )
                .then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1)) // minimum radius 1
                                        .executes(context -> {
                                            CommandSourceStack source = context.getSource();
                                            int x = IntegerArgumentType.getInteger(context, "x");
                                            int z = IntegerArgumentType.getInteger(context, "z");
                                            int radius = IntegerArgumentType.getInteger(context, "radius");
                                            unloadChunksAt(source, x, z, radius);
                                            if (radius >= 1) {
                                                source.getExecutor().sendMessage(Component.text("Unloaded chunks at (" + x + ", " + z + ") with radius: " + radius).color(NamedTextColor.RED));
                                            }
                                            return 1;
                                        })
                                )
                        )
                );

        root.then(Commands.literal("checkchunk")
                .then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();
                                    int x = IntegerArgumentType.getInteger(context, "x");
                                    int z = IntegerArgumentType.getInteger(context, "z");
                                    checkIfChunkIsLoaded(source, x, z); // Check specific chunk
                                    return 1;
                                })
                        )
                )
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    checkIfChunkIsLoaded(source); // Check the player's current chunk
                    return 1;
                })
        );


        // also load the help command
        HelpCommand.plugin = plugin;
        root.then(HelpCommand.build());

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

    private static void modifyChunks(CommandSourceStack stack, int centerX, int centerZ, int radius, boolean load) {
        Entity executor = stack.getExecutor();
        Player player = executor instanceof Player ? (Player) executor : null;

        if (player == null) {
            stack.getExecutor().sendMessage(Component.text("Only players can run this command!").color(NamedTextColor.RED));
            return;
        }

        World world = player.getWorld();

        if (radius < 0) {
            stack.getExecutor().sendMessage(Component.text("Radius must be greater than or equal to 0!").color(NamedTextColor.RED));
            return;
        }


        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                Chunk chunk = world.getChunkAt(centerX + dx, centerZ + dz);

                if (load) {
                    chunk.addPluginChunkTicket(plugin);
                    boolean actuallyAdded = plugin.addTicketedChunk(chunk, executor.getUniqueId(), true);
                    if (actuallyAdded) {
                        stack.getExecutor().sendMessage(Component.text("Loaded chunk at (" + chunk.getX() + ", " + chunk.getZ() + ")").color(NamedTextColor.GREEN));
                    } else if (radius == 0) {
                        stack.getExecutor().sendMessage(Component.text("Chunk at (" + chunk.getX() + ", " + chunk.getZ() + ") was already loaded!").color(NamedTextColor.YELLOW));
                    }
                } else {
                    chunk.removePluginChunkTicket(plugin);
                    boolean actuallyRemoved = plugin.removeTicketedChunk(chunk);
                    if (actuallyRemoved) {
                        stack.getExecutor().sendMessage(Component.text("Unloaded chunk at (" + chunk.getX() + ", " + chunk.getZ() + ")").color(NamedTextColor.RED));
                    } else if (radius == 0) {
                        stack.getExecutor().sendMessage(Component.text("Chunk at (" + chunk.getX() + ", " + chunk.getZ() + ") was never loaded!").color(NamedTextColor.YELLOW));
                    }
                }
            }
        }
        }

    private static void loadChunksAroundPlayer(CommandSourceStack stack) {
        loadChunksAroundPlayer(stack, 2);
    }

    private static void loadChunksAroundPlayer(CommandSourceStack stack, int radius) {
        Entity executor = stack.getExecutor();
        Player player = executor instanceof Player ? (Player) executor : null;

        if (player == null) {
            stack.getExecutor().sendMessage("Only players can run this command!");
            return;
        }

        Location location = player.getLocation();
        Chunk centerChunk = location.getChunk();
        LoadChunksCommand.modifyChunks(stack, centerChunk.getX(), centerChunk.getZ(), radius, true);
    }

    private static void unloadChunksAroundPlayer(CommandSourceStack stack) {
        unloadChunksAroundPlayer(stack, 2);
    }

    private static void unloadChunksAroundPlayer(CommandSourceStack stack, int radius) {
        Entity executor = stack.getExecutor();
        Player player = executor instanceof Player ? (Player) executor : null;

        if (player == null) {
            stack.getExecutor().sendMessage("Only players can run this command!");
            return;
        }

        Location location = player.getLocation();
        Chunk centerChunk = location.getChunk();
        modifyChunks(stack, centerChunk.getX(), centerChunk.getZ(), radius, false);
    }

    private static void loadSpecificChunk(CommandSourceStack stack, int x, int z) {
        modifyChunks(stack, x, z, 0, true); // radius 0 = only one chunk
    }

    private static void unloadSpecificChunk(CommandSourceStack stack, int x, int z) {
        modifyChunks(stack, x, z, 0, false); // radius 0 = only one chunk
    }

    private static void loadChunksAt(CommandSourceStack stack, int x, int z, int radius) {
        modifyChunks(stack, x, z, radius, true); // load = true
    }

    private static void unloadChunksAt(CommandSourceStack stack, int x, int z, int radius) {
        modifyChunks(stack, x, z, radius, false); // load = false
    }

    private static void listChunks(CommandSourceStack stack, int pageNumber) {
        Set<TicketedChunk> chunks = plugin.getTicketedChunks();

        int entriesPerPage = 5;
        int totalChunks = chunks.size();
        int totalPages = (int) Math.ceil((double) totalChunks / entriesPerPage);

        if (totalChunks == 0) {
            stack.getSender().sendMessage(Component.text("No chunks are currently loaded.").color(NamedTextColor.RED));
            return;
        }

        if (pageNumber < 1 || pageNumber > totalPages) {
            stack.getSender().sendMessage(Component.text("Invalid page. Must be between 1 and " + totalPages + ".").color(NamedTextColor.RED));
            return;
        }

        stack.getSender().sendMessage(Component.text("Loaded Chunks (Page " + pageNumber + "/" + totalPages + "):").color(NamedTextColor.GOLD));

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
            stack.getExecutor().sendMessage(Component.text(msg).color(NamedTextColor.AQUA));
        }
    }

    private static void checkIfChunkIsLoaded(CommandSourceStack stack) {
        Entity executor = stack.getExecutor();
        Player player = executor instanceof Player ? (Player) executor : null;

        if (player == null) {
            stack.getExecutor().sendMessage("Only players can run this command!");
            return;
        }

        Location location = player.getLocation();
        Chunk currentChunk = location.getChunk();
        checkIfChunkIsLoaded(stack, currentChunk);
    }

    private static void checkIfChunkIsLoaded(CommandSourceStack stack, int x, int z) {
        World world = stack.getExecutor().getWorld();  // Get the world of the executor
        Chunk chunk = world.getChunkAt(x, z);
        checkIfChunkIsLoaded(stack, chunk);
    }

    private static void checkIfChunkIsLoaded(CommandSourceStack stack, Chunk chunk) {
        // Check if the chunk is loaded
        if (plugin.isChunkLoaded(chunk)) {
            stack.getExecutor().sendMessage(Component.text("The chunk (x: " + chunk.getX() + " z: " + chunk.getZ() + ") is loaded.").color(NamedTextColor.GREEN));
        } else {
            stack.getExecutor().sendMessage(Component.text("The chunk (x: " + chunk.getX() + " z: " + chunk.getZ() + ") is not loaded.").color(NamedTextColor.RED));
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
