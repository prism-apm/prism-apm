import request from '@/utils/request'
import * as url from './url'

export function reqAppOverviewDetail(query: any) {
    return request({
        url: url.OVERVIEW_DETAIL,
        method: 'get',
        params: query
    })
}
export function reqServiceDetail(query: any) {
    return request({
        url: url.SERVICE_DETAIL,
        method: 'get',
        params: query
    })
}
export function reqServiceTransactionList(query: any) {
    return request({
        url: url.SERVICE_TRANSACTION,
        method: 'get',
        params: query
    })
}
export function reqSingleIfaceDetail(query: any) {
    return request({
        url: url.IFACE_DETAIL,
        method: 'get',
        params: query
    })
}
export function reqServiceWhoCallMeListGroupByIface(query: any) {
    return request({
        url: url.IFACE_FROM_IFACE,
        method: 'get',
        params: query,
    })
}
export function reqServiceWhoCallMeGroupByServiceList(query: any) {
    return request({
        url: url.IFACE_FROM_SERVICE,
        method: 'get',
        params: query,
    })
}
export function reqServiceAnalyseMap(query: any) {
    return request({
        url: url.IFACE_ANALYSE_MAP,
        method: 'get',
        params: query
    })
}
export function reqServiceAnalyseGather(query: any) {
    return request({
        url: url.IFACE_GATHER,
        method: 'get',
        params: query
    })
}
export function reqDatabaseList(query: any) {
    return request({
        url: url.DB_LIST,
        method: 'get',
        params: query
    })
}
export function reqDatabaseDetail(query: any) {
    return request({
        url: url.DB_DETAIL,
        method: 'get',
        params: query
    })
}
