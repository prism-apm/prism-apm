import Vue from 'vue';
import VueI18n from 'vue-i18n';
import zhCN from './zh-CN.json';
import enUS from './en-US.json';
import iviewZhCN from 'iview/dist/locale/zh-CN';
import iviewEnUS from 'iview/dist/locale/en-US';

Vue.use(VueI18n)

const messages = {
    'zh-CN': { ...iviewZhCN, ...zhCN },
    'en-US': { ...iviewEnUS, ...enUS }
}
const locale = localStorage.getItem('locale') || 'zh-CN'

const i18n = new VueI18n({
    locale,
    fallbackLocale: 'zh-CN',
    messages
})

export default i18n
