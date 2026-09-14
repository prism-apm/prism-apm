<template>
  <div>
    <div class="db-type-tabs" @click.capture="handleTabsClick">
      <Tabs :value="systemType" @on-click="handleChangeTab" size="small">
        <TabPane label="Mysql" name="mysql"></TabPane>
        <TabPane label="Postgres" name="postgresql"></TabPane>
        <TabPane label="Redis" name="redis"></TabPane>
        <TabPane label="Mongo" name="mongodb"></TabPane>
        <TabPane label="ES" name="elasticsearch"></TabPane>
        <TabPane label="Kafka" name="kafka"></TabPane>
        <TabPane label="RocketMQ" name="rocketmq"></TabPane>
      </Tabs>
    </div>
    <Row class="map-container">
      <Col span="5" class="bgfff ml10">
      <interface-list-selector v-model="selectedIFace" :data="sortList" :sort-by="sortBy" :loading="listLoading"
        :get-item-title="getNativeTitle" @on-sort-change="handleChangeSort"
        @on-select="handleChangeSelectInterfaceName">
        <template v-slot:header>
          <span v-if="['mysql', 'postgresql', 'redis', 'mongodb', 'elasticsearch'].includes(systemType)">
            {{ $t('database.dbOperations') }}&nbsp;
          </span>
          <Tabs v-else class="small-tab" @on-click="handleChangeMessagingType">
            <TabPane label="Producer" name="4"></TabPane>
            <TabPane label="Consumer" name="5"></TabPane>
          </Tabs>
        </template>
      </interface-list-selector>
      </Col>
      <Col span="18" class="ml25">
      <h2 class="title">
        {{ systemType.toUpperCase() }} {{ $t('database.redMetricsNoCompare') }}
      </h2>
      <div class="echarts-container bgfff" id="db-RED-chart"></div>
      <h2 class="title mt30">{{ $t('transaction.responseTime') }}</h2>
      <div class="echarts-container bgfff" id="db-duration-chart"></div>
      <h2 class="title mt30">{{ $t('transaction.callCount') }}</h2>
      <div class="echarts-container bgfff" id="db-count-chart"></div>
      <h2 class="title mt30">{{ $t('transaction.errorCount') }}</h2>
      <div class="echarts-container bgfff" id="db-error-chart"></div>
      </Col>
    </Row>
  </div>
</template>
<script>
import {reqDatabaseDetail, reqDatabaseList} from '@/api/service/req'
import {calculateDateRangeByDiff} from '@/utils/date-utils'
import {
  DEFAULT_INTERFACE_ORDER_KEY,
  DEFAULT_MACHINE_ID,
  DEFAULT_SPAN_KIND,
  DEFAULT_SYSTEM_TYPE
} from "@/constants/default-vals";
import InterfaceSortedList from "@/entity/view/InterfaceSortedList";
import {hideLoading, setOptionWithResize, showLoading} from "@/utils/echarts-utils";
import {
  SYSTEM_TYPE_KAFKA,
  SYSTEM_TYPE_ES,
  SYSTEM_TYPE_MONGO,
  SYSTEM_TYPE_MYSQL,
  SYSTEM_TYPE_POSTGRES,
  SYSTEM_TYPE_REDIS,
  SYSTEM_TYPE_ROCKETMQ
} from "@/constants/system-type";
import InterfaceListSelector from "@/components/interface_list_selector.vue";
import {isTimeCompareMode, needRequestCompareData, pickMachineIdForApiParams} from "@/helper/request-helper";
import TransactionInfoResponse from "@/entity/response/service_detail/TransactionInfoResponse";
import {RedMapDataCalculator} from "@/entity/view/RedMapDataCalculator";
import ServiceDetailInfoResponse from "@/entity/response/service_detail/ServiceDetailResponse";
import {getComparableEchartsOption, getREDEchartsOptionObject} from "@/helper/echarts-option-helper";
import {handleEmptyResponse, handleErrorResponse, isEmptyResponse} from "@/utils/response-utils";
import EchartsRedCompareOption from "@/entity/dto/EchartsRedCompareOption";
import {eventBus} from "@/events/event-bus";
import {mapState, mapWritableState} from "pinia";
import {useAppStore} from "@/store/store";

const CHART_IDS = Object.freeze({
  RED: 'db-RED-chart',
  DURATION: 'db-duration-chart',
  COUNT: 'db-count-chart',
  ERROR: 'db-error-chart',
});

export default {
  data() {
    return {
      sortBy: DEFAULT_INTERFACE_ORDER_KEY,
      selectedIFace: null,
      systemType: DEFAULT_SYSTEM_TYPE,
      spanKind: DEFAULT_SPAN_KIND,
      compareDateValue: {},
      /**
       * @type {Array<TransactionInfoResponse>}
       */
      list: [],
      /**
       * @type {import('@/entity/view/InterfaceSortedList').InterfaceSortedItem[]}
       */
      sortList: [],
      metricSorter: new InterfaceSortedList(),

      mainChartData: new RedMapDataCalculator(),
      compareChartData: new RedMapDataCalculator(),

      dbListIsBack: true,
      listLoading: false
    }
  },
  components: {InterfaceListSelector},
  computed: {
    ...mapState(useAppStore, {
      freshTime: 'freshTime',
      timeValueList: 'timeValueList',
      serviceId: 'serviceName',
      machineId: 'machineId',
      machineIds: "machineIds",
      compareMode: 'compareMode',
      compareDate: 'compareDate',
    }),
    tooltipName() {
      if (this.systemType === SYSTEM_TYPE_MYSQL || this.systemType === SYSTEM_TYPE_POSTGRES || this.systemType === SYSTEM_TYPE_MONGO) {
        return "Sql";
      } else if (this.systemType === SYSTEM_TYPE_REDIS || this.systemType === SYSTEM_TYPE_ES) {
        return "Operation";
      } else if (this.systemType === SYSTEM_TYPE_KAFKA || this.systemType === SYSTEM_TYPE_ROCKETMQ) {
        return "Topic";
      } else {
        return "Name";
      }
    }
  },
  watch: {
    compareMode: {
      handler() {
        this.handleCompareControlChange();
      }
    }
  },
  mounted() {
    useAppStore().showCompareControl = true;
    useAppStore().forceSingleMachineMode = false;
    eventBus.on('machine-change', this.handleMachineChange);
    eventBus.on('compare-confirm', this.handleCompareDateConfirm);
    if (this.serviceId) {
      this.requestDbOverViewList();
    }
  },
  beforeDestroy() {
    eventBus.off('machine-change', this.handleMachineChange);
    eventBus.off('compare-confirm', this.handleCompareDateConfirm);
  },
  methods: {
//Event Handler part==================================
    handleMachineChange(value) {
      let cause = value.cause;
      if (cause === 'component') {
        this.requestDatabaseDetail();
      } else {
        this.requestDbOverViewList();
      }
    },
    handleChangeMessagingType(name) {
      this.spanKind = name
      this.requestDbOverViewList()
    },
    handleCompareDateConfirm() {
      this.requestDatabaseDetail()
    },
    async handleCompareControlChange() {
      if (needRequestCompareData(this.compareMode, this.compareDate, this.machineIds)) {
        this.$nextTick(() => {
          this.requestDatabaseDetail()
        })
      }
    },
    handleTabsClick(event) {
      if (!this.dbListIsBack) {
        event.stopPropagation();
        event.preventDefault();
        this.$Message.info(this.$t('database.tooFrequent'));
      }
    },
    handleChangeTab(value) {
      if (!this.dbListIsBack) {
        return;
      }
      this.dbListIsBack = false;
      this.systemType = value;
      this.mainChartData.clear();
      this.compareChartData.clear();
      if ([SYSTEM_TYPE_REDIS, SYSTEM_TYPE_MONGO, SYSTEM_TYPE_MYSQL, SYSTEM_TYPE_POSTGRES, SYSTEM_TYPE_ES].includes(value)) {
        this.spanKind = DEFAULT_SPAN_KIND;
      }
      this.requestDbOverViewList();
    },
    async handleChangeSort(sortKey, firstIn = false) {
      if (this.list.length === 0) {
        this.sortList = []
        return
      }

      this.sortBy = sortKey;
      this.sortList = this.metricSorter.sort(this.list, sortKey);

      if (firstIn && this.sortList.length > 0) {
        this.selectedIFace = this.sortList[0];
      }
    },
    handleChangeSelectInterfaceName(item) {
      this.selectedIFace = item;
      this.requestDatabaseDetail();
    },
    getNativeTitle(item) {
      return `【 Interface 】\n${item.ifaceCaller}\n--------------------------------\n【 ${this.tooltipName} 】\n${item.ifaceName}`;
    },
    //Event Handler End==================================

    //Data request part==================================
    async requestDbOverViewList() {
      this.listLoading = true;
      let params = {
        startDate: this.freshTime[0],
        endDate: this.freshTime[1],
        serviceName: this.serviceId,
        systemType: this.systemType,
        spanKind: this.spanKind,
        concurrentId: 'requestDbOverViewList'
      }
      if (this.machineId !== DEFAULT_MACHINE_ID) {
        params.machineId = this.machineId
      }
      try {
        let res = await reqDatabaseList(params)
        if (isEmptyResponse(res)) {
          this.$Message.info(this.$t('database.noData'));
          handleEmptyResponse([this.list, this.sortList], Object.values(CHART_IDS));
          return;
        }
        this.list = res.data.map(item => new TransactionInfoResponse(item));
        await this.handleChangeSort(this.sortBy, true);
        await this.requestDatabaseDetail();
      } catch (err) {
        handleErrorResponse(err, this.$t('database.fetchListError'));
      } finally {
        this.dbListIsBack = true
        this.listLoading = false;
      }
    },
    async requestDatabaseDetail() {
      let params = {
        startDate: this.freshTime[0],
        endDate: this.freshTime[1],
        serviceName: this.serviceId,
        ifaceName: this.selectedIFace.ifaceName,
        ifaceCaller: this.selectedIFace.ifaceCaller,
        ifaceNameHashKey: this.selectedIFace.ifaceNameHashKey,
        ifaceCallerHashKey: this.selectedIFace.ifaceCallerHashKey,
        machineId: pickMachineIdForApiParams(this.compareMode, this.compareDate, false, this.machineId, this.machineIds),
        systemType: this.systemType
      }
      try {
        if (this.systemType === 'kafka' || this.systemType === 'rocketmq') {
          params.spanKind = this.spanKind
        }
        showLoading(Object.values(CHART_IDS))
        let res = await reqDatabaseDetail(params)
        if (isEmptyResponse(res)) {
          this.mainChartData.clear();
          this.initEcharts();
          return;
        }

        const entityList = res.data.map(item => new ServiceDetailInfoResponse(item));
        this.mainChartData.load(entityList);

        if (needRequestCompareData(this.compareMode, this.compareDate, this.machineIds)) {
          await this.requestDatabaseDetail4Compare()
        } else {
          this.$nextTick(() => {
            this.initEcharts()
          })
        }
      } catch (err) {
        handleErrorResponse(err, this.$t('database.fetchChartError'));
      }
    },
    async requestDatabaseDetail4Compare() {
      let params = {
        serviceName: this.serviceId,
        ifaceName: this.selectedIFace.ifaceName,
        ifaceCaller: this.selectedIFace.ifaceCaller,
        ifaceNameHashKey: this.selectedIFace.ifaceNameHashKey,
        ifaceCallerHashKey: this.selectedIFace.ifaceCallerHashKey,
        systemType: this.systemType
      };
      if (isTimeCompareMode(this.compareMode)) {
        this.compareDateValue = calculateDateRangeByDiff(this.freshTime[0], this.freshTime[1], this.compareDate);
        params = {
          ...params,
          startDate: this.compareDateValue.startDate,
          endDate: this.compareDateValue.endDate,
          machineId: pickMachineIdForApiParams(this.compareMode, this.compareDate, false, this.machineId, this.machineIds),
        }
      } else {
        params = {
          ...params,
          startDate: this.freshTime[0],
          endDate: this.freshTime[1],
          machineId: pickMachineIdForApiParams(this.compareMode, this.compareDate, true, this.machineId, this.machineIds),
        }
      }
      try {
        if (this.systemType === 'kafka' || this.systemType === 'rocketmq') {
          params.spanKind = this.spanKind
        }
        let res = await reqDatabaseDetail(params)
        if (isEmptyResponse(res)) {
          this.compareChartData.clear();
          this.initEcharts();
          this.$Message.error(this.$t('transaction.noCompareData'))
          return;
        }
        const entityList = res.data.map(item => new ServiceDetailInfoResponse(item));
        this.compareChartData.load(entityList);
        this.initEcharts(true)
      } catch (err) {
        handleErrorResponse(err, this.$t('database.fetchCompareError'));
      }
    },
    //Data request End=========================================

    //Echarts part====================
    initEcharts(isCompare) {
      let option = getREDEchartsOptionObject(this.timeValueList, this.mainChartData.durations, this.mainChartData.counts, this.mainChartData.errs);
      setOptionWithResize(CHART_IDS.RED, option);
      let compareOptions = null;
      if (isCompare) {
        compareOptions = new EchartsRedCompareOption(this.compareMode, this.compareDateValue, this.freshTime, this.machineIds);
      }
      let options = getComparableEchartsOption(compareOptions, this.timeValueList, this.mainChartData, this.compareChartData);
      setOptionWithResize(CHART_IDS.DURATION, options.duration);
      setOptionWithResize(CHART_IDS.COUNT, options.count);
      setOptionWithResize(CHART_IDS.ERROR, options.error);
      hideLoading(CHART_IDS.RED);
      hideLoading(CHART_IDS.DURATION)
      hideLoading(CHART_IDS.COUNT)
      hideLoading(CHART_IDS.ERROR)
    }
    // ================
    //Echarts End========================
  }
}
</script>
<style lang="less" scoped>
.table-width {
  margin: 20px;
  background: #fff;
}

.small-tab .ivu-tabs-nav .ivu-tabs-tab {
  padding: 8px 3px;
}

.db-type-tabs {
  margin: 0 21px;
  background: #eee;
  padding: 10px 10px 10px 0;

  /deep/ .ivu-tabs-bar {
    border-bottom: 1px solid #d8dce5;
    box-shadow: 0 1px 2px 0 rgba(0, 0, 0, .12), 0 0 2px 0 rgba(0, 0, 0, .04);
    margin-bottom: 0;
    background: transparent;
    padding-left: 0;
  }

  /deep/ .ivu-tabs-nav .ivu-tabs-tab {
    width: 106px;
    padding: 0 6px 0 0;
    box-sizing: border-box;
    height: 32px;
    line-height: 30px;
    font-size: 14px;
    font-weight: bold;
    text-align: center;
    color: #00000082;
    background: transparent;
    border: none;
    border-radius: 0;
    transition: none;
    position: relative;
    z-index: 1;
    margin: 0 0 8px;
  }

  /deep/ .ivu-tabs-nav .ivu-tabs-tab::before {
    content: "";
    position: absolute;
    top: 0;
    left: 0;
    width: 100px;
    height: 100%;
    background: #fff;
    z-index: -1;
    transition: all 0.3s ease-in-out;
  }

  /deep/ .ivu-tabs-nav .ivu-tabs-tab:hover {
    color: #fff;
  }

  /deep/ .ivu-tabs-nav .ivu-tabs-tab:hover::before {
    background: #20bba6;
    border-color: #20bba6;
  }

  /deep/ .ivu-tabs-nav .ivu-tabs-tab-active {
    color: #fff;
  }

  /deep/ .ivu-tabs-nav .ivu-tabs-tab-active::before {
    background: #20bba6;
    border-color: #20bba6;
  }

  /deep/ .ivu-tabs-ink-bar {
    background-color: #14ab96;
    height: 3px;
    bottom: 0;
    box-sizing: border-box;
    width: 100px !important;
  }

  /deep/ .ivu-tabs-content {
    height: 0;
  }
}
</style>
