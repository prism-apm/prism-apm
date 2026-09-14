package org.prism.apm.ui.server.entity.response.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IfaceAnalyseMapResponse {

    private String ifaceName;
    private List<IfaceDetailResponse> ifaceDetail;

}
