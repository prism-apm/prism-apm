package org.prism.apm.ui.server.service.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.prism.apm.ui.server.constant.route.DynamicTablePlaceholder;
import org.prism.apm.ui.server.entity.dto.route.RouteInfo;
import org.prism.apm.ui.server.entity.dto.security.UserDTO;
import org.prism.apm.ui.server.entity.model.project.Project;
import org.prism.apm.ui.server.entity.param.common.DateParam;
import org.prism.apm.ui.server.mapper.project.ProjectMapper;
import org.prism.apm.ui.server.mapper.project.UserProjectGroupMapper;
import org.prism.apm.ui.server.mapper.service.HostnameMapper;
import org.prism.apm.ui.server.route.TableRoutingService;
import org.prism.apm.ui.server.utils.DateUtils;
import org.prism.apm.ui.server.utils.FillDataHoleUtil;
import org.prism.apm.ui.server.utils.ParamCheckUtils;
import org.prism.apm.ui.server.utils.SecurityUtils;
import org.prism.apm.ui.server.utils.TimeAxisUtils;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.prism.apm.ui.server.utils.FillDataHoleUtil.MIN_DATA_STEP_UNIT;

@Service
public class CommonService {

    private final HostnameMapper hostnameMapper;

    private final UserProjectGroupMapper userProjectGroupMapper;

    private final ProjectMapper projectMapper;

    private final TableRoutingService tableRoutingService;

    public CommonService(HostnameMapper hostnameMapper, UserProjectGroupMapper userProjectGroupMapper, ProjectMapper projectMapper, TableRoutingService tableRoutingService) {
        this.hostnameMapper = hostnameMapper;
        this.userProjectGroupMapper = userProjectGroupMapper;
        this.projectMapper = projectMapper;
        this.tableRoutingService = tableRoutingService;
    }

    /**
     * Get the service name list
     */
    public List<String> getCurrentUserServiceNameList() {
        List<Project> projects = getCurrentUserServiceList();

        return projects.stream()
                .map(Project::getName)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Get the service list
     */
    public List<Project> getCurrentUserServiceList() {
        UserDTO current_user = SecurityUtils.getCurrentUser();
        List<Project> projects;
        if (SecurityUtils.isCurrentUserAdmin(current_user)) {
            projects = projectMapper.selectList(Wrappers.emptyWrapper());
        } else {
            projects = userProjectGroupMapper.selectProjectsByUserId(current_user.getUserId());
        }
        return projects;
    }

    /**
     * Query host names running the service
     */
    public List<String> getMachineList(String serviceName, DateParam dateParam) {
        ParamCheckUtils.validateDateAndServiceName(dateParam, serviceName);
        dateParam.convertToUTC();
        ParamCheckUtils.validateServiceNamePermission(getCurrentUserServiceNameList(), serviceName);
        RouteInfo routeInfo = tableRoutingService.getTableName(dateParam, DynamicTablePlaceholder.TAG_HOSTNAME);
        return hostnameMapper.queryMachineByServiceName(dateParam, serviceName, routeInfo);
    }

    public List<String> getTimeAxis(DateParam dateParam) {
        ZoneId zoneId = ZoneId.of(dateParam.getTimezone());
        long start = DateUtils.convertDatetime2Timestamp(dateParam.getStartDate(), zoneId).getTime();
        long end = DateUtils.convertDatetime2Timestamp(dateParam.getEndDate(), zoneId).getTime() - Duration.ofSeconds(1).toMillis();
        long dataStep = (tableRoutingService.getTableName(dateParam.getStartDate(), dateParam.getEndDate(), DynamicTablePlaceholder.TAG_ENTRY).getDataStep()) * MIN_DATA_STEP_UNIT;
        List<Timestamp> timestamps = FillDataHoleUtil.fillHolesGeneric(start, end, new ArrayList<>(), dataStep, x -> null, x -> Timestamp.from(Instant.ofEpochMilli(x)));
        return TimeAxisUtils.calculateTimeAxis(timestamps, dataStep, zoneId);
    }

}
