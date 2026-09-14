import { defineStore } from 'pinia'
import { constantRouterMap } from '@/routers/router'
import { DEFAULT_COMPARE_MODE, DEFAULT_MACHINE_ID } from "@/constants/default-vals"

const hasChild = (item: any): boolean => (item.children && item.children.length !== 0);
const forEach = (arr: any[], fn: (item: any, index: number, array: any[]) => void): void => {
    if (!arr.length || !fn) return
    let i = -1
    let len = arr.length
    while (++i < len) {
        fn(arr[i], i, arr)
    }
}
const getMenuByRouter = (list: any[], path?: string): any[] => {
    let res: any[] = []
    forEach(list, item => {
        if (item.path === '/') return
        let obj: any = {
            icon: (item.meta && item.meta.icon) || '',
            app: (item.meta && item.meta.app) || '',
            name: item.name,
            path: path ? (path + '/' + item.path) : item.path,
            hideInMenu: (item.meta && item.meta.hideInMenu) || false,
        }
        if (hasChild(item)) {
            obj.children = getMenuByRouter(item.children, item.path)
        }
        res.push(obj)
    })
    return res
};

// ================= define Pinia Store =================
// noinspection JSUnusedGlobalSymbols
export const useAppStore = defineStore('app', {
    state: () => ({
        freshTime: [] as any[],
        timezone: '',
        timeValueList: [] as any[],
        app: 'server-overview',
        serviceName: '',
        activeRoutePath: '',
        machineId: DEFAULT_MACHINE_ID,
        machineIds:[DEFAULT_MACHINE_ID],
        compareMode: DEFAULT_COMPARE_MODE,
        compareDate: '',
        showCompareControl: true,
        forceSingleMachineMode: false,
        sidebarCollapsed: false,
    }),

    getters: {
        menuList: (): any[] => getMenuByRouter(constantRouterMap, undefined),
    },

    actions: {
    },

    persist: {
        storage: window.sessionStorage,
    }
})