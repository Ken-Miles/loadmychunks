package org.snug.loadMyChunks;

import org.bukkit.Chunk;

import java.util.Optional;
import java.util.UUID;

public class TicketedChunk {
    private final Chunk chunk;
    private final String worldName;
    private final int x;
    private final int z;
    private final long timestamp; // milliseconds
    private final UUID executorUUID;
    private final Boolean is_player;
    private final Optional<UUID> playerUUID;

    public TicketedChunk(Chunk chunk, UUID executorUUID, Boolean is_player) {
        this(chunk, executorUUID, is_player, System.currentTimeMillis());
    }

    public TicketedChunk(Chunk chunk, UUID executorUUID, Boolean is_player, Long timestamp) {
        this.chunk = chunk;
        this.worldName = chunk.getWorld().getName();
        this.x = chunk.getX();
        this.z = chunk.getZ();
        this.timestamp = timestamp;
        this.executorUUID = executorUUID;
        this.is_player = is_player;
        if (this.is_player) {
            this.playerUUID = Optional.of(executorUUID);
        }
        else {
            this.playerUUID = Optional.empty();
        }
    }

    public Chunk getChunk() { return chunk; }
    public String getWorldName() { return worldName; }

    public int getX() { return x; }

    public int getZ() { return z; }

    public long getTimestamp() { return timestamp; }

    public UUID getExecutorUUID() { return executorUUID; }
    public Boolean getIsPlayer() { return is_player; }
    public Optional<UUID> getPlayerUUID() { return playerUUID; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TicketedChunk that = (TicketedChunk) o;

        if (x != that.x) return false;
        if (z != that.z) return false;
        return worldName.equals(that.worldName);
    }

    @Override
    public int hashCode() {
        int result = worldName.hashCode();
        result = 31 * result + x;
        result = 31 * result + z;
        return result;
    }

}
