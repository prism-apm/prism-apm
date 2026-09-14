package org.prism.apm.ui.server.entity.db.query;

import org.prism.apm.ui.server.entity.param.service.AnalyseMapQueryParam;
import org.prism.apm.ui.server.entity.param.service.GatherQueryParam;
import org.prism.apm.ui.server.entity.param.service.WhoCallMeQueryParam;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServiceDataParam {

    private String strInterval;

    private String serviceName;
    private String hostName;
    private String targetName;

    private String callerTargetName;
    private String systemType;
    private Short spanKind;

    /**
     * HashKey for target_name (UUID format)
     * Used to directly match the target_name field in the database
     */
    private String targetNameHashKey;

    /**
     * caller_HashKey for target_name (UUID format)
     * Used to directly match the caller_target_name field in the database
     */
    private String callerTargetNameHashKey;

    public ServiceDataParam convertFrom(AnalyseMapQueryParam analyseMapQueryParam){
        this.serviceName = analyseMapQueryParam.getServiceName();
        this.hostName = analyseMapQueryParam.getMachineId();
        this.targetName = analyseMapQueryParam.getIfaceName();
        this.targetNameHashKey = analyseMapQueryParam.getIfaceNameHashKey();
        return this;
    }
    public ServiceDataParam convertFrom(GatherQueryParam analyseMapQueryParam){
        this.serviceName = analyseMapQueryParam.getServiceName();
        this.hostName = analyseMapQueryParam.getMachineId();
        this.targetName = analyseMapQueryParam.getIfaceName();
        this.targetNameHashKey = analyseMapQueryParam.getIfaceNameHashKey();
        return this;
    }

    public ServiceDataParam convertFrom(WhoCallMeQueryParam analyseMapQueryParam){
        this.serviceName = analyseMapQueryParam.getServiceName();
        this.hostName = analyseMapQueryParam.getMachineId();
        this.targetName = analyseMapQueryParam.getIfaceName();
        this.targetNameHashKey = analyseMapQueryParam.getIfaceNameHashKey();
        return this;
    }

}
