package org.example.be.rest.usecase.entry;

public interface DeleteEntryUseCase {

    void execute(String userId, String entryId, int version);

}
