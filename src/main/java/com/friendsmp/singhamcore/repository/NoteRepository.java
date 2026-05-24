package com.friendsmp.singhamcore.repository;

import com.friendsmp.singhamcore.models.Note;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;

public interface NoteRepository {
    CompletableFuture<Void> addNote(Note note);
    CompletableFuture<Void> removeNote(long noteId);
    CompletableFuture<List<Note>> listNotes(UUID playerUuid);
}
