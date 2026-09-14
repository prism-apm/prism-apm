import request from '@/utils/request'
import * as url from './url'

export function reqServiceList(query: any) {
    return request({
        url: url.SERVICE_LIST,
        method: 'get',
        params: query
    })
}
export function reqServiceObjectList(query: any) {
    return request({
        url: url.SERVICE_OBJECT_LIST,
        method: 'get',
        params: query
    })
}
export function reqMachineList(query: any) {
    return request({
        url: url.MACHINE_LIST,
        method: 'get',
        params: query
    })
}
export function reqTimeAxis(query: any) {
    return request({
        url: url.TIME_AXIS,
        method: 'get',
        params: query
    })
}

export function reqLogin(data: any) {
    return request({
        url: url.LOGIN,
        method: 'post',
        data: data
    })
}

export function reqLogout() {
    return request({
        url: url.LOGOUT,
        method: 'post'
    })
}
