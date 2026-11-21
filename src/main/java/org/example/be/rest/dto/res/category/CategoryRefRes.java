package org.example.be.rest.dto.res.category;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryRefRes {

    String id;
    String name;

}
