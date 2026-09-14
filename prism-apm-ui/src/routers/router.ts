import Layout from '@/components/layout.vue'

export const constantRouterMap: any[] = [
    // Login page route
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/modules/login/login.vue'),
        meta: { title: 'router.login', noCache: true, hideInMenu: true }
    },
    //===================================================================================
    // Admin routes
    {
        path: '/admin',
        redirect: '/admin/dashboard',
    },
    {
        path: '/admin',
        name: 'AdminDashboard',
        component: Layout,
        meta: { title: 'router.adminCenter', noCache: true, hideInMenu: true, icon: 'ios-lock', app: 'server-overview' },
        children: [
            {
                path: 'dashboard',
                name: 'router.adminCenter',
                component: () => import('@/modules/admin/dashboard.vue'),
            }
        ]
    },
    {
        path: '/admin/permission',
        name: 'AdminPermission',
        component: () => import('@/modules/admin/permission-manage.vue'),
        meta: { title: 'router.permissionManage', noCache: true, hideInMenu: true }
    },
    {
        path: '/admin/assign',
        name: 'AdminAssign',
        component: () => import('@/modules/admin/assign-manage.vue'),
        meta: { title: 'router.assignManage', noCache: true, hideInMenu: true }
    },
    {
        path: '/admin/project',
        name: 'AdminProject',
        component: () => import('@/modules/admin/project.vue'),
        meta: { title: 'router.projectManage', noCache: true, hideInMenu: true }
    },
    //====================================================================================
    // Main function routes
    {
        path: '/',
        redirect: '/login',
    },
    {
        path: '/overview',
        name: 'overview',
        meta: {
            icon: 'ios-apps',
            app: 'server-overview'
        },
        component: Layout,
        children: [
            {
                path: 'application',
                name: 'router.serviceOverview',
                component: () => import('@/modules/overview/overview.vue')
            }
        ]
    },
    {
        path: '/overview',
        name: 'serviceTransaction',
        meta: {
            icon: 'ios-pulse',
            app: 'server-detail'
        },
        component: Layout,
        children: [
            {
                path: 'transaction',
                name: 'router.interfaceDetail',
                component: () => import('@/modules/service_detail/transaction.vue')
            }
        ]
    },
    {
        path: '/overview',
        name: 'serviceDatabase',
        meta: {
            icon: 'logo-buffer',
            app: 'server-detail'
        },
        component: Layout,
        children: [
            {
                path: 'db',
                name: 'router.database',
                component: () => import('@/modules/service_detail/db.vue')
            }
        ]
    },
    {
        path: '/overview',
        name: 'serviceDetail',
        meta: {
            icon: 'md-analytics',
            app: 'server-detail'
        },
        component: Layout,
        children: [
            {
                path: 'keyAction',
                name: 'router.serviceDetail',
                component: () => import('@/modules/service_detail/service.vue')
            }
        ]
    },
    {
        path: '/topology',
        name: 'topology_list',
        meta: {
            icon: 'ios-git-network',
            app: 'server-overview'
        },
        component: Layout,
        children: [
            {
                path: 'list',
                name: 'router.topology',
                component: () => import('@/modules/topology/new-index.vue')
            }
        ]
    }
]
