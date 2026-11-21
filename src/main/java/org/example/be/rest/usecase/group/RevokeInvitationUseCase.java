package org.example.be.rest.usecase.group;

public interface RevokeInvitationUseCase {

    void execute(String userId, String groupId, String invitationId);

}
