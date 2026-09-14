<template>
  <div class="layout">
    <Sider ref="sidebar" hide-trigger collapsible :collapsed-width="78" class="side-bar" v-model="isCollapsed"
      :style="{ position: 'fixed', height: '100vh', left: 0, zIndex: 10 }">
      <h1 class="apm-logo" @click="toHomePage" style="cursor: pointer">
        <span v-if="!isCollapsed">{{ $t('layout.apmCenter') }}</span>
        <span v-else>APM</span>
      </h1>

      <side-menu :collapsed="isCollapsed" />

    </Sider>
    <Layout :style="{ marginLeft: isCollapsed ? '80px' : '200px', zIndex: 9 }">
      <Header :style="{ padding: '0 20px' }" class="layout-header-bar">
        <Tooltip :content="tooltipText" placement="right">
          <Icon @click.native="collapse" class="collapsed-sidebar-icon" type="md-swap" size="20"></Icon>
        </Tooltip>
        <div class="header-right">
          <div class="right-operator">
            <date-select />
          </div>
          <div class="lang-selector">
            <Select v-model="currentLocale" prefix="md-globe" size="small" @on-change="switchLanguage"
                    style="width: 100px">
              <Option value="zh-CN">{{ $t('layout.langZh') }}</Option>
              <Option value="en-US">{{ $t('layout.langEn') }}</Option>
            </Select>
          </div>
          <div class="share">
            <Button class="share-btn" @click="share">
              <Icon type="md-share" style="margin-right: 5px; font-size: 14px; position: relative; top: -1px;" />
              {{ $t('layout.share') }}
            </Button>
          </div>
          <div class="logout">
            <Button class="logout-btn" @click="logout">
              <Icon type="md-log-out" style="margin-right: 5px; font-size: 14px; position: relative; top: -1px;" />
              {{ $t('layout.logout') }}
            </Button>
          </div>
        </div>
      </Header>
      <Content class="content-class">
        <div class="header" v-if="showSelector">
          <Row>
            <Col span="4" class="w262">
              <Tooltip class="full-width-tooltip" :delay="500" placement="top" :disabled="serviceSelectOpen">
                <div slot="content" style="white-space: normal; max-width: 300px; word-break: break-word;">
                  {{ $t('layout.serviceSelectTooltip') }}
                </div>

                <service-select v-model="serviceId" @on-open-change="serviceSelectOpen = $event"></service-select>
              </Tooltip>
            </Col>

            <Col span="6" class="w338">
              <Tooltip class="full-width-tooltip" :delay="500" placement="top" :disabled="machineSelectOpen">
                <div slot="content" style="white-space: normal; max-width: 300px; word-break: break-word;">
                  {{ $t('layout.hostSelectTooltip') }}
                </div>

                <machine-select v-model="selectedMachine" :is-single-mode="isSingleModeComputed"
                                :query-params="machineApiParams" @change="handleMachineChange"
                                @on-open-change="machineSelectOpen = $event">
                </machine-select>
              </Tooltip>
            </Col>
            <Col span="6" class="diff" v-show="showCompareControl">
              <compare-control v-model="compareMode" :date.sync="compareDate" :reference-time="freshTime[0]"
                               @confirm="handleCompareDateConfirm"></compare-control>
            </Col>
          </Row>
        </div>
        <router-view v-if="isGlobalTimeReady" />
        <div v-else class="global-loading">
          <Spin size="large" :fix="true">
            <Icon type="ios-loading" size=18 class="spin-icon-load"></Icon>
            <div>{{ $t('layout.initializingTime') }}</div>
          </Spin>
        </div>
      </Content>
    </Layout>
  </div>
</template>
<script>
import dateSelect from '@/components/date_select.vue'
import {reqLogout} from "@/api/common/req";
import SideMenu from "@/components/side_menu.vue";
import {copyText} from "@/utils/copy-utils";
import MachineSelect from "@/components/machine_select.vue";
import ServiceSelect from "@/components/service_select.vue";
import CompareControl from "@/components/compare_control.vue";
import {isTimeCompareMode} from "@/helper/request-helper";
import {eventBus} from "@/events/event-bus";
import {useAppStore} from "@/store/store";
import {mapState, mapWritableState} from "pinia";

export default {
  data() {
    return {
      isCollapsed: false,
      serverApp: 'Server',
      modelShow: false,
      inputValue: '',
      currentLocale: localStorage.getItem('locale') || 'zh-CN',
      serviceSelectOpen: false,
      machineSelectOpen: false
    }
  },
  components: {
    SideMenu,
    dateSelect,
    MachineSelect,
    ServiceSelect,
    CompareControl
  },
  computed: {
    ...mapWritableState(useAppStore, {
      serviceId: 'serviceName',
      machineId: 'machineId',
      machineIds: "machineIds",
      compareMode: 'compareMode',
      compareDate: 'compareDate',
    }),
    ...mapState(useAppStore, ['freshTime', 'timezone']),
    isGlobalTimeReady() {
      const freshTime = useAppStore().freshTime;
      return Array.isArray(freshTime) && freshTime.length === 2 && freshTime[0] && freshTime[1];
    },
    tooltipText() {
      return this.isCollapsed ? this.$t('layout.clickExpand') : this.$t('layout.clickCollapse')
    },
    selectedMachine: {
      get() {
        return this.isSingleModeComputed ? this.machineId : this.machineIds;
      },
      set(newValue) {
        if (this.isSingleModeComputed) {
          this.machineId = newValue;
        } else {
          this.machineIds = newValue;
        }
      }
    },
    isSingleModeComputed() {
      return useAppStore().forceSingleMachineMode || isTimeCompareMode(this.compareMode);
    },
    showCompareControl() {
      return useAppStore().showCompareControl;
    },
    machineApiParams() {
      return {
        serviceName: this.serviceId,
        startDate: this.freshTime[0],
        endDate: this.freshTime[1],
        timezone: this.timezone,
      };
    },
    showSelector() {
      let show_names = ['router.interfaceDetail', 'router.database', 'router.serviceDetail'];
      let name = this.$route.name;
      return show_names.includes(name);
    },
  },
  methods: {
    handleMachineChange(val) {
      eventBus.emit('machine-change', val);
    },
    handleCompareDateConfirm(val) {
      eventBus.emit('compare-confirm', val);
    },
    collapse() {
      this.$refs.sidebar.toggleCollapse()
      useAppStore().sidebarCollapsed = this.isCollapsed;
    },
    toHomePage() {
      if (this.$route.path === '/overview/application') {
        return;
      }
      useAppStore().app = 'server-overview';
      this.$router.push({ path: '/overview/application' })
    },
    share() {
      const path = this.$route.path
      const serviceName = useAppStore().serviceName;
      const freshTime = useAppStore().freshTime;

      const query = {
        ...this.$route.query,
        serviceName: serviceName
      }

      if (freshTime && freshTime.length === 2) {
        query.startTime = freshTime[0]
        query.endTime = freshTime[1]
      }
      const routeData = this.$router.resolve({
        path: path,
        query: query
      })

      const fullUrl = window.location.origin + window.location.pathname + routeData.href
      copyText(this.$Message, fullUrl)
    },
    logout() {
      reqLogout();
      useAppStore().$reset();
      this.$router.push({ path: '/login' })
      localStorage.removeItem('apm_token');
    },
    switchLanguage(locale) {
      this.$i18n.locale = locale
      localStorage.setItem('locale', locale)
      this.$Message.info(this.$t('layout.langChangeHint'))
    }
  }
}
</script>
<style scoped lang='less'>
.global-loading {
  position: relative;
  height: 100%;
  width: 100%;
  min-height: 500px;
  background: #fff;
}

.spin-icon-load {
  animation: ani-demo-spin 1s linear infinite;
}

.layout {
  border: 1px solid #d7dde4;
  border-top: none;
  background: #f5f7f9;
  position: relative;
  border-radius: 4px;
  overflow: hidden;
  min-width: 1530px;

  .ivu-layout-sider {
    background: #28282e;
  }

  .ivu-layout-header {
    height: 45px;
    line-height: 45px;

    .ivu-icon-md-menu {
      cursor: pointer;
    }
  }

  .apm-logo {
    height: 45px;
    line-height: 45px;
    padding: 0 12px;
    color: #fff;
  }
}

.header {
  padding: 10px 10px 0 20px;
  background: #eee;
  font-size: 14px;

  .diff {
    padding: 4px 0 0 10px;

    .ivu-icon {
      margin: 0 2px;
    }
  }
}

.content-class {
  margin: 2px;
  background: #eee;
  min-height: calc(100vh - 46px);
}

.layout-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  box-shadow: 0 1px 1px rgba(0, 0, 0, .1);
  padding: 0 20px;
}

.header-right {
  display: flex;
  align-items: center;
  height: 100%;
}

.right-operator {
  float: none !important;
  margin-right: 2px;
  display: flex;
  align-items: center;
}

.lang-selector {
  float: none !important;
  margin-right: 8px;
  display: flex;
  align-items: center;
  height: 29px;

  /deep/ .ivu-select {
    height: 29px !important;
  }

  /deep/ .ivu-select-single .ivu-select-selection {
    height: 29px !important;
  }

  /deep/ .ivu-select-selection {
    height: 29px !important;
    border: 1px solid #c7d0d9 !important;
    border-radius: 2px;
    padding-left: 8px;
  }

  /deep/ .ivu-select-selected-value {
    height: 29px !important;
    line-height: 29px !important;
    font-size: 13px;
    padding-left: 0 !important;
  }

  /deep/ .ivu-select-placeholder {
    height: 29px !important;
    line-height: 29px !important;
  }

  /deep/ .ivu-select-arrow {
    top: 50%;
    transform: translateY(-50%);
    margin-top: 0 !important;
  }
}

.share {
  float: none !important;
  margin-right: 2px !important;
  display: flex;
  align-items: center;
}

.logout {
  float: none !important;
  margin: 0 !important;
  display: flex;
  align-items: center;
}

.share-btn {
  height: 29px !important;
  line-height: 27px !important;

  border: 1px solid #c7d0d9 !important;
  border-radius: 2px;

  background: #fff !important;
  color: #5b6270;

  padding: 0 15px;
  font-size: 13px;
  transition: all 0.3s ease;
}

.logout-btn {
  height: 29px !important;
  line-height: 27px !important;

  border: 1px solid #c7d0d9 !important;
  border-radius: 2px;

  background: #fff !important;
  color: #5b6270;

  padding: 0 15px;
  font-size: 13px;
  transition: all 0.3s ease;
}

.logout-btn:hover {
  color: #ff4d4f;
  border-color: #ff4d4f !important;
  background: #fff !important;
  box-shadow: 0 2px 6px rgba(255, 77, 79, 0.2);
}

.collapsed-sidebar-icon {
  margin: 0 20px 0 0;
  cursor: pointer;
}

.full-width-tooltip {
  width: 100%;
  display: block;

  /deep/ .ivu-tooltip-rel {
    width: 100%;
    display: block;
  }
}
</style>