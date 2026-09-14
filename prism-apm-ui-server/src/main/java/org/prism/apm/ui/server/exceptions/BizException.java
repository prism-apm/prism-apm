package org.prism.apm.ui.server.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class BizException extends RuntimeException {

    private String message;

    public BizException(String message) {
        super();
        this.message = message;
    }
}
