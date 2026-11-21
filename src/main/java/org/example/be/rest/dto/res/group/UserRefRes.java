package org.example.be.rest.dto.res.group;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserRefRes {

    String userId;
    String displayName;

}
