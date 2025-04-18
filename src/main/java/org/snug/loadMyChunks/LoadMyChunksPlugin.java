package org.snug.loadMyChunks;

import com.destroystokyo.paper.event.brigadier.CommandRegisteredEvent;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class LoadMyChunksPlugin extends JavaPlugin implements Listener {
    private final Set<TicketedChunk> ticketedChunks = new HashSet<TicketedChunk>();

    public void addTicketedChunk(Chunk chunk, UUID playerUUID, boolean is_player) {
        ticketedChunks.add(new TicketedChunk(chunk, playerUUID, is_player));
    }

    public void removeTicketedChunk(Chunk chunk) {
        ticketedChunks.removeIf(tc -> tc.getChunk().equals(chunk));
    }

    public Set<TicketedChunk> getTicketedChunks() {
        return ticketedChunks;
    }

    public Boolean isChunkLoaded(TicketedChunk chunk) {
        return ticketedChunks.contains(chunk);
    }

    public Boolean isChunkLoaded(Chunk chunk) {
        return ticketedChunks.stream().anyMatch(tc -> tc.getChunk().equals(chunk));
    }

    private static final List<Command> registeredCommands = new ArrayList<>();

    @EventHandler
    public void onCommandRegistration(CommandRegisteredEvent event) {
        registeredCommands.add(event.getCommand());
        // spammy
        // getLogger().info("Registered command: " + event.getCommand());
    }

    public List<Command> getRegisteredCommands() {
        return registeredCommands;
    }

    @Override
    public void onEnable() {

        getLogger().info("Loading LoadMyFarms");

        // register events to make the help command work
        getServer().getPluginManager().registerEvents(this, this);


        LoadChunksCommand.plugin = this;

        LifecycleEventManager<Plugin> manager = this.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();

            // Register the command we defined earlier
            commands.register(
                    LoadChunksCommand.build(), // This is the command built previously
                    "loadmychunks"
            );
        });

        File file = new File(getDataFolder(), "chunks.yml");
        if (file.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            if (config.isList("chunks")) {
                List<Map<?, ?>> chunkList = config.getMapList("chunks");
                for (Map<?, ?> chunkData : chunkList) {
                    String worldName = (String) chunkData.get("world");
                    int x = (int) chunkData.get("x");
                    int z = (int) chunkData.get("z");
                    long timestamp = (long) chunkData.get("timestamp");
                    UUID playerUUID = UUID.fromString((String) chunkData.get("player"));
                    boolean is_player = (boolean) chunkData.get("is_player");

                    World world = getServer().getWorld(worldName);
                    if (world == null) continue;

                    Chunk chunk = world.getChunkAt(x, z);
                    chunk.addPluginChunkTicket(this);
                    ticketedChunks.add(new TicketedChunk(chunk, playerUUID, is_player, timestamp));
                    getLogger().info("Loaded chunk at (" + x + ", " + z + ") in " + worldName);
                }
            } else {
                getLogger().warning("chunks.yml exists but no chunks found!");
            }
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Saving LoadMyFarms");

        File file = new File(getDataFolder(), "chunks.yml");
        YamlConfiguration config = new YamlConfiguration();
        List<Map<String, Object>> chunkList = new ArrayList<>();

        for (TicketedChunk ticket : ticketedChunks) {
            Map<String, Object> chunkData = new LinkedHashMap<>();
            chunkData.put("world", ticket.getWorldName());
            chunkData.put("x", ticket.getX());
            chunkData.put("z", ticket.getZ());
            chunkData.put("timestamp", ticket.getTimestamp());
            chunkData.put("player", ticket.getPlayerUUID().map(UUID::toString).orElse(null));
            chunkData.put("is_player", ticket.getIsPlayer());
            chunkList.add(chunkData);
            getLogger().info("Saving chunk at (" + ticket.getX() + ", " + ticket.getZ() + ") in " + ticket.getWorldName());
        }

        config.set("chunks", chunkList);

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
