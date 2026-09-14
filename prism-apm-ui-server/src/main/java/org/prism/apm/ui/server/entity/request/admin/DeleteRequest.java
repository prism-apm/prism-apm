package org.prism.apm.ui.server.entity.request.admin;

import lombok.Data;

/**
 * Delete request parameters
 */
@Data
public class DeleteRequest {

    /**
     * ID of the record to delete
     */
    private Long id;
} 