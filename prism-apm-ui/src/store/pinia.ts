import Vue from 'vue'
import { createPinia, PiniaVuePlugin } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'

// IMPORTANT: Pinia's Vue plugin must be installed BEFORE vue-router's.
//
// Both register a `beforeCreate` mixin, executed in installation order. The
// router's beforeCreate triggers the initial navigation synchronously, which
// runs the `beforeEach` guard in routers/index.ts. That guard calls
// useAppStore(), which relies on an active Pinia (set by Pinia's own
// beforeCreate via setActivePinia). If the router plugin is installed first,
// its beforeCreate runs before Pinia's, so getActivePinia() is undefined and
// useAppStore() throws `Cannot read properties of undefined (reading '_s')`.
//
// Because ES imports are hoisted, this side effect must live in a module that
// is imported before `import router from '@/routers'` in app.ts.
Vue.use(PiniaVuePlugin)
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

export default pinia
