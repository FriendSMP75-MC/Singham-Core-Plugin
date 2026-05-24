package com.friendsmp.singhamcore.repository;

import com.friendsmp.singhamcore.database.AsyncDatabaseExecutor;
import com.friendsmp.singhamcore.database.DatabaseManager;
import com.friendsmp.singhamcore.models.Note;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SQLNoteRepository implements NoteRepository {

    private final DatabaseManager databaseManager;

    public SQLNoteRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public CompletableFuture<Void> addNote(Note note) {
        if (!databaseManager.isAvailable()) return CompletableFuture.completedFuture(null);
        return AsyncDatabaseExecutor.runAsync(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement stmt = connection.prepareStatement("INSERT INTO notes (player_uuid, author_uuid, author_name, note_text, created_at) VALUES (?, ?, ?, ?, ?);") ) {
                stmt.setObject(1, note.getPlayerUuid());
                stmt.setObject(2, note.getAuthorUuid());
                stmt.setString(3, note.getAuthorName());
                stmt.setString(4, note.getText());
                stmt.setLong(5, note.getCreatedAt().toEpochMilli());
                stmt.executeUpdate();
            } catch (SQLException ex) {
                databaseManager.getPlugin().getLogger().severe("Failed to add note: " + ex.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Void> removeNote(long noteId) {
        if (!databaseManager.isAvailable()) return CompletableFuture.completedFuture(null);
        return AsyncDatabaseExecutor.runAsync(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement stmt = connection.prepareStatement("DELETE FROM notes WHERE id = ?;")) {
                stmt.setLong(1, noteId);
                stmt.executeUpdate();
            } catch (SQLException ex) {
                databaseManager.getPlugin().getLogger().severe("Failed to remove note: " + ex.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<List<Note>> listNotes(UUID playerUuid) {
        if (!databaseManager.isAvailable()) return CompletableFuture.completedFuture(new ArrayList<>());
        return AsyncDatabaseExecutor.supplyAsync(() -> {
            List<Note> list = new ArrayList<>();
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement stmt = connection.prepareStatement("SELECT id, player_uuid, author_uuid, author_name, note_text, created_at FROM notes WHERE player_uuid = ? ORDER BY created_at DESC;")) {
                stmt.setObject(1, playerUuid);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    list.add(new Note(rs.getLong("id"), rs.getObject("player_uuid", UUID.class), rs.getObject("author_uuid", UUID.class), rs.getString("author_name"), rs.getString("note_text"), Instant.ofEpochMilli(rs.getLong("created_at"))));
                }
            } catch (SQLException ex) {
                databaseManager.getPlugin().getLogger().severe("Failed to list notes: " + ex.getMessage());
            }
            return list;
        });
    }
}
