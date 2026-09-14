package org.prism.apm.common.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 
 * @author john
 * @date 2024-10-17 11:31
 *
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceParseConfigEntity {
    private Integer id;

    private String serviceName;

    private Boolean parsed;

    private Boolean enabled;
}