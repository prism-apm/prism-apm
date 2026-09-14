import * as url from './url'
import request from "@/utils/request";

export function getTopologyData(query: any) {
    return request({
        url: url.TOPOLOGY_GET,
        method: 'get',
        params: query
    });
}

export function getTopologyWarningRules() {
    return request({
        url: url.TOPOLOGY_WARNING_LIST,
        method: 'get',
    });
}

export function switchTopologyWarningStatus(ruleId: number | string, enabled: boolean) {
    return request({
        url: url.SWITCH_TOPOLOGY_WARNING_STATUS,
        method: 'post',
        data: {
            "id": ruleId,
            "isEnable": enabled
        },
        headers: {
            'Content-Type': 'application/json'
        }
    });
}

export function deleteTopologyWarningRule(ruleId: number | string) {
    return request({
        url: url.DELETE_TOPOLOGY_WARNING_RULE,
        method: 'post',
        data: {
            "idInteger": ruleId
        },
        headers: {
            'Content-Type': 'application/json'
        }
    });
}

export function addTopologyWarningRule(ruleData: any) {
    return request({
        url: url.ADD_TOPOLOGY_WARNING_RULE,
        method: 'post',
        data: ruleData,
        headers: {
            'Content-Type': 'application/json'
        }
    });
}

export function updateTopologyWarningRule(ruleData: any) {
    return request({
        url: url.UPDATE_TOPOLOGY_WARNING_RULE,
        method: 'post',
        data: ruleData,
        headers: {
            'Content-Type': 'application/json'
        }
    });
}
