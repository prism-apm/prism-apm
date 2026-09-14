<template>
  <div>
    <div class="custom-tab-wrapper">
      <Tabs :value="tabId" :key="tabsKey" @on-click="handleChangeTab" size="small">
        <TabPane :label="$t('transaction.tabs.indicatorList')" :name="TAB_IDS.INDICATOR_LIST"></TabPane>
        <TabPane :label="$t('transaction.tabs.internalDetails')" :name="TAB_IDS.INTERNAL_DETAILS"></TabPane>
        <TabPane :label="$t('transaction.tabs.sourceStats')" :name="TAB_IDS.SOURCE_STATISTICS"></TabPane>
        <TabPane :label="$t('transaction.tabs.redComparison')" :name="TAB_IDS.RED_COMPARISON"></TabPane>
      </Tabs>
    </div>
    <!-- TAB_IDS.INDICATOR_LIST -->
    <Table v-if="tabId === TAB_IDS.INDICATOR_LIST" ref="table" :columns="indicatorColumn" :data="indicatorsDataList"
      :loading="indicatorsTableLoading" class="table-width" size="small">
    </Table>

    <!-- TAB_IDS.INTERNAL_DETAILS & TAB_IDS.SOURCE_STATISTICS & TAB_IDS.RED_COMPARISON -->
    <Row v-else class="map-container" style="margin-top:2px">
      <Col class="bgfff ml10" span="5">
      <interface-list-selector v-model="selectedIFace" :data="sortList" :sort-by="sortBy"
        :loading="indicatorsTableLoading" @on-sort-change="handleChangeSort"
        @on-select="handleChangeSelectInterfaceName">
        <template v-slot:header>
          {{ $t('transaction.interfaceList') }}
        </template>
      </interface-list-selector>
      </Col>
      <Col v-show="tabId === TAB_IDS.INTERNAL_DETAILS" class="ml25" span="18">
      <div class="title"><strong>{{ $t('transaction.ifaceREDMetrics') }} &nbsp; </strong>
        <Tooltip placement="top" max-width="200">
          <Icon type="ios-information-circle-outline" />
          <template v-slot:content>
            <p>{{ $t('transaction.redCompoundTip') }}</p>
          </template>
        </Tooltip>
      </div>
      <div id="iface-RED-chart" class="echarts-container bgfff"></div>
      <p></p>
      <div class="title mt30"><strong>{{ $t('transaction.internalCallTime') }} &nbsp; </strong>
        <RadioGroup v-model="analyseMapGroupByKey" @on-change="handleAnalyseMapGroupByKeyChangeHandler" type="button"
          size="small">
          <Radio label="AN">
            <span>{{ $t('transaction.groupByType') }}</span>
          </Radio>
          <Radio label="TN">
            <span>{{ $t('transaction.groupByTarget') }}</span>
          </Radio>
        </RadioGroup>
        <Tooltip placement="top" max-width="200">
          <Icon type="ios-information-circle-outline" />
          <template v-slot:content>
            <p>{{ $t('transaction.groupByTypeTip') }}</p>
            <p>{{ $t('transaction.groupByTargetTip') }}</p>
            <p>{{ $t('transaction.groupByTargetLimitTip') }}</p>
          </template>
        </Tooltip>
        <InputNumber v-if="this.analyseMapGroupByKey === 'TN'" :max="1000" :min="1" v-model="analyseMapLimit"
          size="small" @on-change="initAnalyseMapChart">
        </InputNumber>
      </div>
      <div id="iface-analyse-chart" class="echarts-container bgfff"></div>
      <div class="table-title mt30"><strong>{{ $t('transaction.internalAnalysis') }} &nbsp; </strong>
        <RadioGroup v-model="gatherGroupByKey" @on-change="handleGatherGroupByKeyChangeHandler" type="button"
          size="small">
          <Radio label="AN">
            <span>{{ $t('transaction.groupByType') }}</span>
          </Radio>
          <Radio label="TN">
            <span>{{ $t('transaction.groupByTarget') }}</span>
          </Radio>
        </RadioGroup>
        <Tooltip placement="top" max-width="200">
          <Icon type="ios-information-circle-outline" />
          <template v-slot:content>
            <p>{{ $t('transaction.groupByTypeTip') }}</p>
            <p>{{ $t('transaction.groupByTargetTip') }}</p>
          </template>
        </Tooltip>
      </div>
      <Table ref="table" :columns="gatherColumns" :data="gatherData" :loading="gatherLoading" class="tr-cursor"
        size="small">
      </Table>
      <Page :page-size="gatherPageSize" :page-size-opts="DEFAULT_PAGE_SIZE_OPTIONS" :total="gatherDataTotal"
        class="ml10" :show-sizer="true" :show-total="true" size="small" @on-change="handleAnalysePageChange"
        @on-page-size-change="handleAnalysePageSizeChange" />
      </Col>
      <Col v-show="tabId === TAB_IDS.SOURCE_STATISTICS" class="ml25" span="18">
      <div style="display: flex; gap: 5px">
        <div id="newWhoCallMeIfacePieChart0" class="echarts-container-half-width bgfff"></div>
        <div id="newWhoCallMeIfacePieChart1" class="echarts-container-half-width bgfff"></div>
        <div id="newWhoCallMeIfacePieChart2" class="echarts-container-half-width bgfff"></div>
      </div>
      <p></p>
      <h2 class="table-title mt20">{{ $t('transaction.callSourceByIface') }}</h2>
      <Table ref="table22" :columns="whoCallMeColumns" :data="whoCallMeByIfaceData" :loading="whoCallMeLoading"
        class="tr-cursor" size="small" @on-sort-change="handleWhoCallMeIfaceSortChange">
      </Table>
      <Page :page-size="whoCallMePageSize" :page-size-opts="DEFAULT_PAGE_SIZE_OPTIONS" :total="whoCallMeDataTotal"
        class="ml10" :show-sizer="true" :show-total="true" size="small" @on-change="handleWhoCallMePageChange"
        @on-page-size-change="handleWhoCallMePageSizeChange" />
      <h2 class="table-title mt30">{{ $t('transaction.callSourceByService') }}</h2>
      <Table ref="table21" :columns="whoCallMeByServiceColumns" :data="whoCallMeByServiceData"
        :loading="whoCallMeByServiceLoading" class="tr-cursor" size="small">
      </Table>
      </Col>
      <Col v-show="tabId === TAB_IDS.RED_COMPARISON" class="ml25" span="18">
      <h2 class="title">{{ $t('transaction.responseTime') }}</h2>
      <div id="iface-duration-chart" class="echarts-container bgfff"></div>
      <h2 class="title mt30">{{ $t('transaction.callCount') }}</h2>
      <div id="iface-count-chart" class="echarts-container bgfff"></div>
      <h2 class="title mt30">{{ $t('transaction.errorCount') }}</h2>
      <div id="iface-error-chart" class="echarts-container bgfff"></div>
      </Col>
    </Row>
  </div>
</template>
<script>
import {
  reqServiceAnalyseGather,
  reqServiceAnalyseMap,
  reqServiceTransactionList,
  reqServiceWhoCallMeGroupByServiceList,
  reqServiceWhoCallMeListGroupByIface,
  reqSingleIfaceDetail
} from '@/api/service/req'
import {calculateDateRangeByDiff,} from '@/utils/date-utils'
import {
  DEFAULT_INTERFACE_ORDER_KEY,
  DEFAULT_MACHINE_ID,
  DEFAULT_PAGE_NUM,
  DEFAULT_PAGE_SIZE,
  DEFAULT_PAGE_SIZE_OPTIONS,
  DEFAULT_PAGE_TOTAL
} from "@/constants/default-vals";
import {formatTinyNumber} from '@/utils/number-utils';
import InterfaceSortedList from "@/entity/view/InterfaceSortedList";
import {hideLoading, setOptionWithResize, showLoading} from "@/utils/echarts-utils";
import InterfaceListSelector from "@/components/interface_list_selector.vue";
import {isTimeCompareMode, needRequestCompareData, pickMachineIdForApiParams} from "@/helper/request-helper";
import TransactionInfoResponse from "@/entity/response/service_detail/TransactionInfoResponse";
import TransactionRowModel from "@/entity/view/TransactionRowModel";
import {RedMapDataCalculator} from "@/entity/view/RedMapDataCalculator";
import {IfaceDetailResponse} from "@/entity/response/service_detail/IfaceDetailResponse";
import {IfaceAnalyseMapResponse} from "@/entity/response/service_detail/IfaceAnalyseMapResponse";
import {IfaceGatherInfoResponse} from "@/entity/response/service_detail/IfaceGatherInfoResponse";
import GatherRowModel from "@/entity/view/GatherRowModel";
import {
  getAnalyseMapOption,
  getComparableEchartsOption,
  getREDEchartsOptionObject,
  getWhoCallMePieOption
} from "@/helper/echarts-option-helper";
import {WhoCallMeResponse} from "@/entity/response/service_detail/WhoCallMeResponse";
import WhoCallMeRowModel from "@/entity/view/WhoCallMeRowModel";
import {handleEmptyResponse, handleErrorResponse, isEmptyResponse} from "@/utils/response-utils";
import EchartsRedCompareOption from "@/entity/dto/EchartsRedCompareOption";
import {eventBus} from "@/events/event-bus";
import {useAppStore} from "@/store/store";
import {mapState} from "pinia";

const TAB_IDS = Object.freeze({
  INDICATOR_LIST: '1',
  INTERNAL_DETAILS: '2',
  SOURCE_STATISTICS: '21',
  RED_COMPARISON: '3'
});

const CHART_IDS = Object.freeze({
  RED: 'iface-RED-chart',
  ANALYSE: 'iface-analyse-chart',
  DURATION: 'iface-duration-chart',
  COUNT: 'iface-count-chart',
  ERROR: 'iface-error-chart',
  FROM_PIE_1: 'newWhoCallMeIfacePieChart0',
  FROM_PIE_2: 'newWhoCallMeIfacePieChart1',
  FROM_PIE_3: 'newWhoCallMeIfacePieChart2',
});

export default {
  data() {
    return {
      DEFAULT_PAGE_SIZE_OPTIONS,
      TAB_IDS: TAB_IDS,
      tabId: TAB_IDS.INDICATOR_LIST, // current tab id
      // forces <Tabs> to remount so its internal activeKey re-syncs to `tabId`
      // after a rejected switch (iView sets activeKey optimistically on click)
      tabsKey: 0,

      sortBy: DEFAULT_INTERFACE_ORDER_KEY,//sort by key
      /**
       * @type {import('@/entity/view/InterfaceSortedList').InterfaceSortedItem}
       */
      selectedIFace: null,//selected interface

      indicatorsTableLoading: false,
      indicatorColumn: TransactionRowModel.getColumn(),
      /**
       * @type {Array<TransactionRowModel>}
       */
      indicatorsDataList: [],
      /**
       * @type {Array<TransactionInfoResponse>}
       */
      list: [],
      /**
       * @type {import('@/entity/view/InterfaceSortedList').InterfaceSortedItem[]}
       */
      sortList: [],
      metricSorter: new InterfaceSortedList(),

      analyseMapGroupByKey: 'AN',
      analyseMapLimit: 20,

      gatherColumns: GatherRowModel.getColumn(),
      /**
       * @type {Array<GatherRowModel>}
       */
      gatherData: [],
      gatherDataTotal: DEFAULT_PAGE_TOTAL,
      gatherPageNum: DEFAULT_PAGE_NUM,
      gatherPageSize: DEFAULT_PAGE_SIZE,
      gatherGroupByKey: 'AN',
      gatherLoading: false,

      whoCallMeByServiceLoading: true,
      whoCallMeByServiceColumns: WhoCallMeRowModel.getByServiceColumn(),
      /**
       * @type {Array<WhoCallMeRowModel>}
       */
      whoCallMeByServiceData: [],

      whoCallMeLoading: true,
      whoCallMeColumns: WhoCallMeRowModel.getByIfaceColumn(),
      /**
       * @type {Array<WhoCallMeRowModel>}
       */
      whoCallMeByIfaceData: [],
      whoCallMeDataTotal: DEFAULT_PAGE_TOTAL,
      whoCallMePageNum: DEFAULT_PAGE_NUM,
      whoCallMePageSize: DEFAULT_PAGE_SIZE,

      compareDateValue: {},

      mainChartData: new RedMapDataCalculator(),
      compareChartData: new RedMapDataCalculator(),

      /**
       * @type {Array<IfaceAnalyseMapResponse>}
       */
      analyseMap: [],
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
  },
  watch: {
    tabId: {
      immediate: true,
      handler(val) {
        if (val === TAB_IDS.RED_COMPARISON) {
          useAppStore().showCompareControl = true;
          useAppStore().forceSingleMachineMode = false;
        } else {
          useAppStore().showCompareControl = false;
          useAppStore().forceSingleMachineMode = true;
          // if mode switches back and we were in multiple machine mode, reset to single machine mode
          if (this.machineIds.length === 2) {
            useAppStore().machineId = DEFAULT_MACHINE_ID;
            useAppStore().machineIds = [DEFAULT_MACHINE_ID];
          }
        }
      }
    },
    compareMode: {
      handler() {
        this.handleCompareControlChange();
      }
    }
  },
  mounted() {
    eventBus.on('machine-change', this.handleMachineChange);
    eventBus.on('compare-confirm', this.handleCompareDateConfirm);
    if (this.serviceId) {
      this.requestTransactionList();
    }
  },
  beforeDestroy() {
    useAppStore().showCompareControl = true;
    useAppStore().forceSingleMachineMode = false;
    eventBus.off('machine-change', this.handleMachineChange);
    eventBus.off('compare-confirm', this.handleCompareDateConfirm);
  },
  methods: {
    formatTinyNumber,
    //Event Handler part==================================
    handleMachineChange(value) {
      let cause = value.cause;
      if (this.tabId === TAB_IDS.RED_COMPARISON && cause === 'component') {
        this.handleChangeTab(this.tabId);
      } else {
        this.requestTransactionList();
      }
    },
    handleCompareDateConfirm() {
      this.requestSingleInterFaceDetail();
    },
    async handleCompareControlChange() {
      if (needRequestCompareData(this.compareMode, this.compareDate, this.machineIds)) {
        this.$nextTick(() => {
          this.requestSingleInterFaceDetail();
        });
      }
    },
    handleChangeTab(newTabId) {
      if (this.indicatorsDataList.length === 0) {
        this.$Message.warning(this.$t('transaction.noDataInRange'))
        this.tabId = TAB_IDS.INDICATOR_LIST;
        // iView's <Tabs> already flipped its internal activeKey to newTabId on
        // click; reassigning the same tabId won't trigger its value watcher, so
        // force a remount to reset the visible selection back to overview.
        this.tabsKey++;
        return;
      }
      this.tabId = newTabId
      this.handleChangeSort(this.sortBy);
      this.setDefaultSelectedInterfaceName();
      this.$nextTick(() => {
        this.reloadDataByTabId(this.tabId);
      });
    },
    handleChangeSort(sortKey) {
      if (this.list.length === 0) {
        this.sortList = []
        return
      }

      this.sortBy = sortKey
      this.sortList = this.metricSorter.sort(this.list, sortKey);
    },
    handleAnalysePageChange(pageNum) {
      this.gatherPageNum = pageNum;
      this.requestServiceAnalyseGather(pageNum, this.gatherPageSize);
    },
    handleAnalysePageSizeChange(pageSize) {
      this.gatherPageSize = pageSize;
      this.requestServiceAnalyseGather(this.gatherPageNum, pageSize);
    },
    handleAnalyseMapGroupByKeyChangeHandler() {
      if (this.analyseMapGroupByKey) {
        this.$nextTick(() => {
          this.requestServiceAnalyseMap();
        })
      }
    },
    handleWhoCallMePageChange(pageNum) {
      this.whoCallMePageNum = pageNum;
      this.requestWhoCallMeGroupByIface(pageNum, this.whoCallMePageSize);
    },
    handleWhoCallMePageSizeChange(pageSize) {
      this.whoCallMePageSize = pageSize;
      this.requestWhoCallMeGroupByIface(this.whoCallMePageNum, pageSize);
    },
    handleWhoCallMeIfaceSortChange(sortParams) {
      this.requestWhoCallMeGroupByIface(this.whoCallMePageNum, this.whoCallMePageSize, sortParams);
    },
    handleGatherGroupByKeyChangeHandler() {
      if (this.gatherGroupByKey) {
        this.$nextTick(() => {
          this.requestServiceAnalyseGather();
        })
      }
    },
    handleChangeSelectInterfaceName() {
      this.whoCallMeByServiceLoading = true;
      this.whoCallMeLoading = true;
      this.reloadDataByTabId(this.tabId);
    },
    //Event Handler End==================================

    //Data request part==================================
    async requestTransactionList() {
      let params = {
        startDate: this.freshTime[0],
        endDate: this.freshTime[1],
        serviceName: this.serviceId
      }
      if (this.machineId !== DEFAULT_MACHINE_ID) {
        params.machineId = this.machineId
      }
      try {
        this.indicatorsTableLoading = true
        let res = await reqServiceTransactionList(params).finally(() => {
          this.indicatorsTableLoading = false;
        })
        if (isEmptyResponse(res)) {
          handleEmptyResponse([this.list, this.indicatorsDataList, this.sortList, this.gatherData, this.whoCallMeByIfaceData, this.whoCallMeByServiceData], Object.values(CHART_IDS));

          return;
        }
        this.list = res.data.map(item => new TransactionInfoResponse(item));
        this.indicatorsDataList = res.data.map(item => {
          const dto = new TransactionInfoResponse(item);
          return new TransactionRowModel(dto);
        });
        this.handleChangeTab(this.tabId);
      } catch (err) {
        handleErrorResponse(err, this.$t('transaction.fetchListError'));
      }
    },
    async requestServiceAnalyseMap() {
      let params = {
        startDate: this.freshTime[0],
        endDate: this.freshTime[1],
        serviceName: this.serviceId,
        ifaceName: this.selectedIFace.ifaceName,
        ifaceNameHashKey: this.selectedIFace.ifaceNameHashKey,
        groupByKey: this.analyseMapGroupByKey,
        concurrentId: 'getServiceAnalyseMap'
      }
      if (this.machineId !== DEFAULT_MACHINE_ID) {
        params.machineId = this.machineId
      }
      try {
        showLoading(CHART_IDS.ANALYSE);
        let res = await reqServiceAnalyseMap(params).finally(() => {
          hideLoading(CHART_IDS.ANALYSE);
        });
        if (isEmptyResponse(res)) {
          handleEmptyResponse([], [CHART_IDS.ANALYSE]);
          return;
        }
        this.analyseMap = res.data.map(item => new IfaceAnalyseMapResponse(item));
        this.initAnalyseMapChart();
      } catch (err) {
        handleErrorResponse(err, this.$t('transaction.fetchInternalCallError'));
      }
    },
    async requestServiceAnalyseGather(pageNum, pageSize) {
      let params = {
        startDate: this.freshTime[0],
        endDate: this.freshTime[1],
        serviceName: this.serviceId,
        ifaceName: this.selectedIFace.ifaceName,
        ifaceNameHashKey: this.selectedIFace.ifaceNameHashKey,
        groupByKey: this.gatherGroupByKey,
        concurrentId: 'getServiceAnalyseGather'
      }
      if (this.machineId !== DEFAULT_MACHINE_ID) {
        params.machineId = this.machineId
      }
      if (pageNum) {
        params.pageNum = pageNum;
      }
      if (pageSize) {
        params.pageSize = pageSize;
      }
      try {
        const matchedItem = this.indicatorsDataList.find(
            item => item.ifaceName === this.selectedIFace.ifaceName
        );

        params.ifaceTotalCount = matchedItem?.totalCount || 0;
        params.ifaceAvgDuration = matchedItem?.avgDuration || 0;

        this.gatherLoading = true;
        let res = await reqServiceAnalyseGather(params).finally(() => {
          this.gatherLoading = false;
        })

        if (isEmptyResponse(res)) {
          this.gatherDataTotal = 0;
          handleEmptyResponse([this.gatherData], []);
          return;
        }

        this.gatherDataTotal = res.total;
        this.gatherData = res.data.map(item => {
          let dto = new IfaceGatherInfoResponse(item);
          return new GatherRowModel(dto);
        });

      } catch (ex) {
        handleErrorResponse(ex, this.$t('transaction.fetchAnalysisError'));
      }
    },
    async requestWhoCallMeGroupByServiceList() {
      let params = {
        startDate: this.freshTime[0],
        endDate: this.freshTime[1],
        serviceName: this.serviceId,
        ifaceName: this.selectedIFace.ifaceName,
        ifaceNameHashKey: this.selectedIFace.ifaceNameHashKey,
        concurrentId: 'getWhoCallMeGroupByServiceList'
      }
      if (this.machineId !== DEFAULT_MACHINE_ID) {
        params.machineId = this.machineId
      }
      try {
        let res = await reqServiceWhoCallMeGroupByServiceList(params).finally(() => {
          this.whoCallMeByServiceLoading = false;
        })
        if (isEmptyResponse(res)) {
          handleEmptyResponse([this.whoCallMeByServiceData], []);
          return;
        }

        this.whoCallMeByServiceData = res.data.map(item => {
          let dto = new WhoCallMeResponse(item);
          return new WhoCallMeRowModel(dto);
        });
        for (let item of this.whoCallMeByServiceData) {
          item.totalTime = (item.totalTime).toFixed(2);
          item.avgDuration = (item.avgDuration).toFixed(2);
        }
      } catch (err) {
        handleErrorResponse(err, this.$t('transaction.fetchCallSourceServiceError'));
      }
    },
    async requestWhoCallMeGroupByIface(pageNum, pageSize, sortParams) {
      let params = {
        startDate: this.freshTime[0],
        endDate: this.freshTime[1],
        serviceName: this.serviceId,
        ifaceName: this.selectedIFace.ifaceName,
        ifaceNameHashKey: this.selectedIFace.ifaceNameHashKey,
        concurrentId: 'getWhoCallMeGroupByIface'
      }
      if (this.machineId !== DEFAULT_MACHINE_ID) {
        params.machineId = this.machineId
      }
      if (pageNum) {
        params.pageNum = pageNum;
      }
      if (pageSize) {
        params.pageSize = pageSize;
      }
      if (sortParams) {
        params.sortBy = sortParams.key;
        params.sortOrder = sortParams.order;
      }
      try {
        showLoading([CHART_IDS.FROM_PIE_1, CHART_IDS.FROM_PIE_2, CHART_IDS.FROM_PIE_3]);
        let res = await reqServiceWhoCallMeListGroupByIface(params).finally(() => {
          this.whoCallMeLoading = false;
          hideLoading([CHART_IDS.FROM_PIE_1, CHART_IDS.FROM_PIE_2, CHART_IDS.FROM_PIE_3]);
        });
        if (isEmptyResponse(res)) {
          this.whoCallMeDataTotal = 0;
          handleEmptyResponse([this.whoCallMeByIfaceData], []);
          return;
        }
        this.whoCallMeByIfaceData = res.data.map(item => {
          let dto = new WhoCallMeResponse(item);
          return new WhoCallMeRowModel(dto);
        });
        this.whoCallMeDataTotal = res.total;
        for (let item of this.whoCallMeByIfaceData) {
          item.totalTime = (item.totalTime).toFixed(3);
          item.avgDuration = (item.avgDuration).toFixed(2);
        }
        this.initNewWhoCallMeIfacePieChart();
      } catch (err) {
        handleErrorResponse(err, this.$t('transaction.fetchCallSourceIfaceError'));
      }
    },
    async requestSingleInterFaceDetail() {
      let params = {
        startDate: this.freshTime[0],
        endDate: this.freshTime[1],
        serviceName: this.serviceId,
        ifaceName: this.selectedIFace.ifaceName,
        ifaceNameHashKey: this.selectedIFace.ifaceNameHashKey,
        machineId: pickMachineIdForApiParams(this.compareMode, this.compareDate, false, this.machineId, this.machineIds),
        concurrentId: 'getSingleInterFaceDetail'
      }
      try {
        if (this.tabId === TAB_IDS.RED_COMPARISON) {
          showLoading(CHART_IDS.DURATION);
          showLoading(CHART_IDS.COUNT);
          showLoading(CHART_IDS.ERROR);
        } else if (this.tabId === TAB_IDS.INTERNAL_DETAILS) {
          showLoading(CHART_IDS.RED);
        }
        let res = await reqSingleIfaceDetail(params)
        if (isEmptyResponse(res)) {
          this.mainChartData.clear();
          this.initREDChart();
          return;
        }
        const entityList = res.data.map(item => new IfaceDetailResponse(item));
        this.mainChartData.load(entityList);

        if (this.tabId === TAB_IDS.RED_COMPARISON && needRequestCompareData(this.compareMode, this.compareDate, this.machineIds)) {
          await this.requestSingleInterfaceDetail4Compare();
        } else {
          this.$nextTick(() => {
            this.initREDChart()
          })
        }
      } catch (err) {
        handleErrorResponse(err, this.$t('transaction.fetchIfaceDetailError'));
      }
    },
    async requestSingleInterfaceDetail4Compare() {
      let params = {
        serviceName: this.serviceId,
        ifaceName: this.selectedIFace.ifaceName,
        ifaceNameHashKey: this.selectedIFace.ifaceNameHashKey,
        concurrentId: 'getSingleInterfaceDetail4Compare'
      };
      if (isTimeCompareMode(this.compareMode)) {
        let date = calculateDateRangeByDiff(this.freshTime[0], this.freshTime[1], this.compareDate)
        this.compareDateValue = date
        params = {
          ...params,
          startDate: date.startDate,
          endDate: date.endDate,
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
        let res = await reqSingleIfaceDetail(params)
        if (isEmptyResponse(res)) {
          this.compareChartData.clear();
          this.initREDChart();
          this.$Message.error(this.$t('transaction.noCompareData'));
          return;
        }
        const entityList = res.data.map(item => new IfaceDetailResponse(item));
        this.compareChartData.load(entityList);
        this.initREDChart(true)
      } catch (err) {
        handleErrorResponse(err, this.$t('transaction.fetchCompareError'));
      }
    },

    //Data request End=========================================


    //Echarts part====================
    initAnalyseMapChart() {
      let option = getAnalyseMapOption(this.timeValueList);
      let map_data = this.analyseMap;

      let processedData = map_data.map(data => {
        const target_name = data.ifaceName;
        const ifaceDetail = data.ifaceDetail || [];
        let total = 0;
        const seriesData = ifaceDetail.map(item => {
          const val = item.redMetric.avgDuration || 0;
          total += val;
          return val;
        });

        return {
          name: target_name,
          data: seriesData,
          total: total
        };
      });

      processedData.sort((a, b) => b.total - a.total);
      if (this.analyseMapGroupByKey === 'TN') {
        processedData = processedData.slice(0, this.analyseMapLimit);
      }

      processedData.forEach(item => {
        option.legend.data.push(item.name);
        option.series.push({
          name: item.name,
          type: 'line',
          smooth: true,
          areaStyle: {
            opacity: 0.3
          },
          itemStyle: {
          },
          data: item.data,
        });
      });

      setOptionWithResize(CHART_IDS.ANALYSE, option, true)
    },
    initNewWhoCallMeIfacePieChart() {
      setOptionWithResize(CHART_IDS.FROM_PIE_1, getWhoCallMePieOption(this.whoCallMeByIfaceData, this.$t('transaction.totalTimeRatio'), 'totalTime', 's', 's'));
      setOptionWithResize(CHART_IDS.FROM_PIE_2, getWhoCallMePieOption(this.whoCallMeByIfaceData, this.$t('transaction.totalCountRatio'), 'totalCount', this.$t('transaction.countUnit'), ''));
      setOptionWithResize(CHART_IDS.FROM_PIE_3, getWhoCallMePieOption(this.whoCallMeByIfaceData, this.$t('transaction.avgDurationRatio'), 'avgDuration', 'ms', 'ms'));
    },
    initREDChart(isCompare) {
      if (this.tabId === TAB_IDS.INTERNAL_DETAILS) {
        let option = getREDEchartsOptionObject(this.timeValueList, this.mainChartData.durations, this.mainChartData.counts, this.mainChartData.errs);
        setOptionWithResize(CHART_IDS.RED, option);
        hideLoading(CHART_IDS.RED);
      } else if (this.tabId === TAB_IDS.RED_COMPARISON) {
        let compareOptions = null;
        if (isCompare) {
          compareOptions = new EchartsRedCompareOption(this.compareMode, this.compareDateValue, this.freshTime, this.machineIds);
        }
        let options = getComparableEchartsOption(compareOptions, this.timeValueList, this.mainChartData, this.compareChartData);
        setOptionWithResize(CHART_IDS.DURATION, options.duration);
        setOptionWithResize(CHART_IDS.COUNT, options.count);
        setOptionWithResize(CHART_IDS.ERROR, options.error);
        hideLoading(CHART_IDS.DURATION)
        hideLoading(CHART_IDS.COUNT)
        hideLoading(CHART_IDS.ERROR)
      }
    },
    //Echarts End========================


    //Util part================
    setDefaultSelectedInterfaceName() {
      this.whoCallMeByServiceLoading = true;
      this.whoCallMeLoading = true;
      if (this.sortList.length === 0) return;
      let iface_name_list = this.sortList.map(item => item.ifaceName);
      if (this.selectedIFace === null || !iface_name_list.includes(this.selectedIFace.ifaceName)) {
        this.selectedIFace = this.sortList[0];
      }
      if (!iface_name_list.includes(this.selectedIFace.ifaceName)) {
        this.$Message.info(this.$t('transaction.switchedToDefaultIface'))
      }
    },
    reloadDataByTabId(tabId) {
      if (tabId === TAB_IDS.INTERNAL_DETAILS) {
        this.requestSingleInterFaceDetail();
        this.requestServiceAnalyseMap();
        this.requestServiceAnalyseGather();
      } else if (tabId === TAB_IDS.SOURCE_STATISTICS) {
        this.requestWhoCallMeGroupByServiceList();
        this.requestWhoCallMeGroupByIface(this.whoCallMePageNum, this.whoCallMePageSize);
      } else if (tabId === TAB_IDS.RED_COMPARISON) {
        this.requestSingleInterFaceDetail();
      }
    },
  }
}
</script>
<style lang="less" scoped>
.table-width {
  margin: 12px 20px 20px 20px;
  background: #fff;
}

.custom-tab-wrapper {
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

    width: 120px;
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

    width: 114px;
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
    width: 114px !important;
  }

  /deep/ .ivu-tabs-content {
    height: 0;
  }
}
</style>
