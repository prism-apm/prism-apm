import request from '@/utils/request'
import adminUrl from './url'

export function addRole(data: any) {
    return request({
        url: adminUrl.ROLE_ADD,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: data
    })
}

export function modifyRole(data: any) {
    return request({
        url: adminUrl.ROLE_MODIFY,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: data
    })
}

export function deleteRole(id: number | string) {
    return request({
        url: adminUrl.ROLE_DELETE,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: { id }
    })
}

export function listRoles(pageParam: any) {
    return request({
        url: adminUrl.ROLE_LIST,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: pageParam
    })
}

export function addPermission(data: any) {
    return request({
        url: adminUrl.PERMISSION_ADD,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: data
    })
}

export function modifyPermission(data: any) {
    return request({
        url: adminUrl.PERMISSION_MODIFY,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: data
    })
}

export function deletePermission(id: number | string) {
    return request({
        url: adminUrl.PERMISSION_DELETE,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: { id }
    })
}

export function listPermissions(pageParam: any) {
    return request({
        url: adminUrl.PERMISSION_LIST,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: pageParam
    })
}

export function addUser(data: any) {
    return request({
        url: adminUrl.USER_ADD,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: data
    })
}

export function modifyUser(data: any) {
    return request({
        url: adminUrl.USER_MODIFY,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: data
    })
}

export function deleteUser(id: number | string) {
    return request({
        url: adminUrl.USER_DELETE,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: { id }
    })
}

export function listUsers(pageParam: any) {
    return request({
        url: adminUrl.USER_LIST,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: pageParam
    })
}

export function assignRoleToUser(roleId: number | string, userId: number | string) {
    return request({
        url: adminUrl.ASSIGN_ROLE_USER,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: { roleId, userId }
    })
}

export function removeUserRole(relationId: number | string) {
    return request({
        url: adminUrl.REMOVE_ROLE_USER,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: { relationId }
    })
}

export function listUserRoleRelations(pageParam: any) {
    return request({
        url: adminUrl.LIST_ROLE_USER,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: pageParam
    })
}

export function assignPermissionToRole(roleId: number | string, permissionId: number | string) {
    return request({
        url: adminUrl.ASSIGN_ROLE_PERMISSION,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: { roleId, permissionId }
    })
}

export function removeRolePermission(relationId: number | string) {
    return request({
        url: adminUrl.REMOVE_ROLE_PERMISSION,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: { relationId }
    })
}

export function listRolePermissionRelations(pageParam: any) {
    return request({
        url: adminUrl.LIST_ROLE_PERMISSION,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: pageParam
    })
}

export function assignProjectToProjectGroup(projectGroupId: number | string, projectId: number | string) {
    return request({
        url: adminUrl.ASSIGN_PROJECT_GROUP_PROJECT,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: { projectGroupId, projectId }
    })
}

export function removeProjectGroupProject(relationId: number | string) {
    return request({
        url: adminUrl.REMOVE_PROJECT_GROUP_PROJECT,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: { relationId }
    })
}

export function listProjectGroupProjectRelations(pageParam: any) {
    return request({
        url: adminUrl.LIST_PROJECT_GROUP_PROJECT,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: pageParam
    })
}

export function assignUserToProjectGroup(projectGroupId: number | string, userId: number | string) {
    return request({
        url: adminUrl.ASSIGN_PROJECT_GROUP_USER,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: { projectGroupId, userId }
    })
}

export function removeProjectGroupUser(relationId: number | string) {
    return request({
        url: adminUrl.REMOVE_PROJECT_GROUP_USER,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: { relationId }
    })
}

export function listProjectGroupUserRelations(pageParam: any) {
    return request({
        url: adminUrl.LIST_PROJECT_GROUP_USER,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: pageParam
    })
}

export function addProjectGroup(data: any) {
    return request({
        url: adminUrl.PROJECT_GROUP_ADD,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: data
    })
}

export function modifyProjectGroup(data: any) {
    return request({
        url: adminUrl.PROJECT_GROUP_MODIFY,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: data
    })
}

export function deleteProjectGroup(id: number | string) {
    return request({
        url: adminUrl.PROJECT_GROUP_DELETE,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: { id }
    })
}

export function listProjectGroups(pageParam: any) {
    return request({
        url: adminUrl.PROJECT_GROUP_LIST,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: pageParam
    })
}

export function addProject(data: any) {
    return request({
        url: adminUrl.PROJECT_ADD,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: data
    })
}

export function modifyProject(data: any) {
    return request({
        url: adminUrl.PROJECT_MODIFY,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: data
    })
}

export function deleteProject(id: number | string) {
    return request({
        url: adminUrl.PROJECT_DELETE,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: { id }
    })
}

export function listProjects(pageParam: any) {
    return request({
        url: adminUrl.PROJECT_LIST,
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: pageParam
    })
}
