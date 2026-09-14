import Vue from 'vue'
import Router from 'vue-router'
import { constantRouterMap } from './router'
import {useAppStore} from "@/store/store";

Vue.use(Router)

let Routers = new Router({
    scrollBehavior: () => ({ x: 0, y: 0 }),
    routes: constantRouterMap
})

//Route guard
Routers.beforeEach((to: any, _from: any, next: any) => {

    const query = to.query

    if (query.serviceName) {
        if (query.serviceName !== useAppStore().serviceName) {
            useAppStore().serviceName = query.serviceName;
        }
    }

    if (query.startTime && query.endTime) {
        const newTimeRange = [query.startTime, query.endTime];
        const oldTimeRange = useAppStore().freshTime || [];

        if (oldTimeRange[0] !== newTimeRange[0] || oldTimeRange[1] !== newTimeRange[1]) {
            useAppStore().freshTime = newTimeRange;
        }
    }
    if (to.path === '/login') {
        next()
        return
    }

    next()
})

export default Routers
