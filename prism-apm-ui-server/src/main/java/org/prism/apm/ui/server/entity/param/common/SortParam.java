package org.prism.apm.ui.server.entity.param.common;

import org.prism.apm.ui.server.constant.OrderBy;
import lombok.Data;

@Data
public class SortParam {

    private String sortBy;
    private String sortOrder;

    public void convertToDatabaseSort(String sortBy, String defaultSortBy, String sortOrder) {
        if (sortBy == null) {
            this.sortBy = defaultSortBy;
        } else {
            this.sortBy = sortBy;
        }
        if (sortOrder == null || OrderBy.Directions.FRONT_END_DEFAULT.equals(sortOrder)) {
            this.sortOrder = OrderBy.Directions.DESC;
        } else {
            this.sortOrder = sortOrder;
        }
    }

}
