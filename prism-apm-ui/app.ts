import Vue from 'vue'
// Pinia must be set up before the router is imported: installing PiniaVuePlugin
// first ensures its beforeCreate (setActivePinia) runs before the router's
// beforeCreate triggers the initial navigation. See src/store/pinia.ts.
import pinia from '@/store/pinia'
import VueRouter from 'vue-router'
import router from '@/routers'
import iView from 'iview'
import 'iview/dist/styles/iview.css'
import '@/style/global.less'
import i18n from '@/locale'

// Configure iView to use vue-i18n for internationalization
Vue.use(iView, {
    i18n: (key: string, value: any) => (i18n as any).t(key, value)
});

Vue.prototype.$Message.config({
    top: 40,
    duration: 10,
});

Vue.use(VueRouter);

// Vue global configuration
Vue.config.productionTip = false
Vue.config.devtools = true;

Vue.config.errorHandler = function (err: any, vm: any, info: string) {
    console.error('====【Global Vue Component Exception】====')
    console.error('Error Message:', err.message)
    console.error('Error Stack:', err.stack)
    console.error('Error Location:', info)
    console.error('Faulty Component:', vm?.$options?.name)
};

window.onerror = function (message: any, source: any, line: any, column: any, error: any) {
    console.error('====【Global Native JS Synchronous Exception】====')
    console.error({message, source, line, column, stack: error?.stack})
    return true // Prevent browser from repeating error logs
};

window.addEventListener('unhandledrejection', (event: PromiseRejectionEvent) => {
    event.preventDefault()
    const err = event.reason
    console.error('====【Global Promise Asynchronous Exception】====')
    console.error('Error Message:', err?.message || err)
    console.error('Error Stack:', err?.stack)
});

window.addEventListener('error', (event: ErrorEvent) => {
    const target = event.target as HTMLElement;
    let resourceUrl = '';

    if (target instanceof HTMLImageElement || target instanceof HTMLScriptElement) {
        resourceUrl = target.src;
    } else if (target instanceof HTMLLinkElement) {
        resourceUrl = target.href;
    }

    if (resourceUrl) {
        console.error('====【Static Resource Loading Exception】====');
        console.error('Resource URL:', resourceUrl);
        event.preventDefault();
    }
}, true);

new Vue({
    el: '#app',
    router,
    pinia,
    i18n,
    render: (h) => h('div', {attrs: {id: 'app'}}, [h('router-view')]),
    mounted() {
        // console.log('root init finish')
    }
});