package org.example.be.rest.dto.res.category;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryListRes {

    List<CategoryRes> data;

}
