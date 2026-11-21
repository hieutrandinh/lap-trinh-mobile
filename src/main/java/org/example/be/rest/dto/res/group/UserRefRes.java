package org.example.be.rest.dto.res.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRefRes {

    String userId;
    String displayName;

}
