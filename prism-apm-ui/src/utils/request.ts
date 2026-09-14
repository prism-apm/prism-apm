import axios, {AxiosRequestConfig, AxiosResponse} from 'axios';
import qs from 'qs';
import i18n from '@/locale';
import {ERROR_TIMEOUT} from "@/constants/error-code";
import Vue from "vue";
import {debounce} from 'lodash';
import {useAppStore} from "@/store/store";

// --- 1. Define Map and Helper Functions ---
const pendingMap = new Map<string, any>();

/**
 * Generate a unique key for each request
 * @param {AxiosRequestConfig} config
 */
const getPendingKey = (config: AxiosRequestConfig): string => {
    // 【Core Function】：If concurrentId is provided, use it directly as the Key
    // This means: as long as two requests have the same concurrentId, the latter one will cancel the former one, regardless of whether the URL is the same
    if (config.params && config.params.concurrentId) {
        console.debug('use concurrentId =>', config.params ? config.params.concurrentId : '', config.params)
        return config.params.concurrentId;
    }

    // Default logic: Generate a fingerprint based on URL, Method, and Params (used to prevent duplicate clicks)
    let {url, method, params, data} = config;
    if (typeof data === 'object' && data !== null) {
        data = JSON.stringify(data);
    }
    return [url, method, JSON.stringify(params), JSON.stringify(data)].join('&');
}

/**
 * Add request to pendingMap
 * @param {AxiosRequestConfig} config
 */
const addPending = (config: AxiosRequestConfig) => {
    const pendingKey = getPendingKey(config);
    console.debug('add pending =>', pendingKey, config.params ? config.params.concurrentId : '', config.params)
    // Use axios's CancelToken
    config.cancelToken = config.cancelToken || new axios.CancelToken((cancel) => {
        if (!pendingMap.has(pendingKey)) {
            pendingMap.set(pendingKey, cancel);
        }
    });
}

/**
 * Remove request (cancel request)
 * @param {AxiosRequestConfig} config
 */
const removePending = (config: AxiosRequestConfig) => {
    const pendingKey = getPendingKey(config);
    console.debug('found pending =>', pendingMap.has(pendingKey), config.params ? config.params.concurrentId : '', config.params)
    if (pendingMap.has(pendingKey)) {
        const cancel = pendingMap.get(pendingKey);
        // Execute cancel operation
        cancel(pendingKey);
        // Remove from Map
        pendingMap.delete(pendingKey);
    }
}
// ------------------------------

const service = axios.create({
    headers: {
        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
    },
    timeout: 60000,
    // withCredentials: true
});

const showPermissionMessage = debounce((message: string) => {
    Vue.prototype.$Message.error({
        content: message
    });
}, 3000, {
    leading: true,
    trailing: false
});

// --- Request Interceptor ---
service.interceptors.request.use((config: AxiosRequestConfig) => {
    // 1. Before sending, check and cancel "similar" old requests
    removePending(config);
    // 2. Add the current request to the queue
    addPending(config);

    // Get the Token previously stored in localStorage
    const token = localStorage.getItem('apm_token');
    if (token && config.headers) {
        // Backend JwtAuthenticationFilter is waiting for a header starting with "Bearer "
        config.headers['Authorization'] = 'Bearer ' + token;
    }

    if (config.method && config.headers) {
        if ((config.method.toLocaleLowerCase() === 'post' && config.headers['Content-Type'] !== 'application/json') ||
            config.method.toLocaleLowerCase() === 'put') {
            config.data = qs.stringify(config.data);
        }
    }

    config.params = config.params || {}
    config.params.timezone = useAppStore().timezone;
    return config
}, (error: any) => {
    console.debug('request error interceptor catch error=>', error)
    return Promise.reject(error)
})

// --- Response Interceptor ---
service.interceptors.response.use(
    (response: AxiosResponse) => {
        // Request successful, remove from queue
        removePending(response.config);

        let success = response.data.success || response.data.status === 200;
        if (!success) {
            const errorMsg = response.data.err || 'Request Failed.';

            Vue.prototype.$Message.error(errorMsg);

            // ================= Enhanced Log Start =================
            // Use groupCollapsed to fold the error, click to expand and view details
            const requestUrl = response.config.url;
            console.groupCollapsed(`%c🚨 API Error: ${requestUrl}`, 'color: #f56c6c; font-weight: bold;');

            console.log('%cRequest URL:', 'color: #409EFF', response.config.baseURL ? response.config.baseURL + requestUrl : requestUrl);
            console.log('%cRequest Method:', 'color: #409EFF', response.config.method?.toUpperCase());

            // Print GET parameters (params)
            if (response.config.params) {
                console.log('%cRequest Parameters (Params):', 'color: #e6a23c', response.config.params);
            }

            // Print POST/PUT data (data)
            // Note: axios's config.data is usually a JSON string, try to parse it for better readability
            if (response.config.data) {
                try {
                    const parsedData = typeof response.config.data === 'string'
                        ? JSON.parse(response.config.data)
                        : response.config.data;
                    console.log('%cRequest Body:', 'color: #e6a23c', parsedData);
                } catch (e) {
                    console.log('%cRequest Body:', 'color: #e6a23c', response.config.data);
                }
            }

            console.log('%cResponse Result:', 'color: #f56c6c', response.data);

            console.groupEnd();
            // ================= Enhanced Log End =================

            return Promise.resolve({status: response.data.status || -1, err: errorMsg})
        }
        return Promise.resolve(response.data)
    },
    (error: any) => {
        // 4. Request failed (or was canceled), also need to remove from queue
        if (error.config) {
            removePending(error.config);
        }

        //handle timeout
        if (error.code === 'ECONNABORTED' && error.message.indexOf('timeout') !== -1) {
            Vue.prototype.$Message.error((i18n as any).t('request.timeout'))
            return Promise.resolve({status: ERROR_TIMEOUT, err: (i18n as any).t('request.timeout')})
        }

        //handle cancel
        if (axios.isCancel(error)) {
            console.debug('request canceled:', error.message);
            return new Promise(() => {
            });
        }

        //401
        if (error.response && error.response.status === 401) {
            showPermissionMessage((i18n as any).t('request.unauthorized'));
            localStorage.removeItem('apm_token');

            const currentPath = window.location.hash || window.location.pathname
            if (currentPath && currentPath !== '#/login' && currentPath !== '/login') {
                localStorage.setItem('redirectPath', currentPath)
            }

            window.location.href = '/#/login';
            return Promise.resolve({status: 401, err: (i18n as any).t('request.unauthorized')})
        }
        //403
        else if (error.response && error.response.status === 403) {
            showPermissionMessage((i18n as any).t('request.forbidden'));
            return Promise.resolve({status: 403, err: (i18n as any).t('request.forbidden')})
        } else {
            return Promise.reject(error)
        }

    })

export default service
