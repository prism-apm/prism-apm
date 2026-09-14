package org.prism.apm.ui.server.entity.param.common;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PageParam extends SortParam{

    private int pageNum = 1;
    private int pageOffset = 0;
    private int pageSize = 10;

    public void convertPageNumToOffset() {
        this.pageOffset = (this.pageNum - 1) * this.pageSize;
    }
}
