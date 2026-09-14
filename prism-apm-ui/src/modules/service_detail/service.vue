<template>
  <div>
    <div style="margin-top: 20px">
      <div class="title-decoration"></div>
      <span class="main-title">{{ $t('service.compositeMetrics') }}</span>
      <span class="sub-title">{{ $t('service.allHostsNoCompare') }}</span>
    </div>
    <div style="background: #f0f2f5; padding: 0 10px;">
      <Row :gutter="24" class="map-container">

        <Col span="8">
        <Card :bordered="false" class="metric-card" shadow>
          <div class="card-content">
            <div class="left-area">
              <p class="card-title">{{ $t('service.rpm') }}</p>
              <h2 class="card-value color-primary">{{ serviceOverview.rpm }}</h2>
            </div>
            <div class="right-icon bg-primary-light">
              <Icon type="ios-speedometer" size="32" class="color-primary" />
            </div>
          </div>
          <Spin size="large" :fix="true" v-if="isLoading"></Spin>
        </Card>
        </Col>

        <Col span="8">
        <Card :bordered="false" class="metric-card" shadow>
          <div class="card-content">
            <div class="left-area">
              <p class="card-title">{{ $t('service.avgDuration') }}</p>
              <h2 class="card-value color-duration">{{ formatTinyNumber(2, false, serviceOverview.redMetric.avgDuration)
              }}
                <span class="unit">ms</span>
              </h2>
            </div>
            <div class="right-icon bg-warning-light">
              <Icon type="ios-time" size="32" class="color-duration" />
            </div>
          </div>
          <Spin size="large" :fix="true" v-if="isLoading"></Spin>
        </Card>
        </Col>

        <Col span="8">
        <Card :bordered="false" class="metric-card" shadow>
          <div class="card-content">
            <div class="left-area">
              <p class="card-title">{{ $t('service.errorRate') }}</p>
              <h2 class="card-value color-error">{{ serviceOverview.errRate }}%</h2>
            </div>
            <div class="right-icon bg-error-light">
              <Icon type="ios-alert" size="32" class="color-error" />
            </div>
          </div>
          <Spin size="large" :fix="true" v-if="isLoading"></Spin>
        </Card>
        </Col>

      </Row>
    </div>
    <div class="section-header" style="margin-top: 20px">
      <div class="title-decoration"></div>
      <span class="main-title">{{ $t('service.detailMetrics') }}</span>
    </div>
    <Row class="map-container ml10 mr10">
      <Col span="24" class="bgfff">
      <div>
        <h2 class="title">{{ $t('transaction.responseTime') }}</h2>
        <div class="echarts-container bgfff" id='service-duration-chart'></div>
      </div>
      </Col>
    </Row>
    <Row class="map-container ml10 mr10">
      <Col span="24" class="bgfff ">
      <div>
        <h2 class="title">{{ $t('transaction.callCount') }}</h2>
        <div class="echarts-container bgfff" id='service-count-chart'></div>
      </div>
      </Col>
    </Row>
    <Row class="map-container ml10 mr10">
      <Col span="24" class="bgfff ">
      <div>
        <h2 class="title">{{ $t('transaction.errorCount') }}</h2>
        <div class="echarts-container bgfff" id='service-error-chart'></div>
      </div>
      </Col>
    </Row>
  </div>
</template>

<script>
import ServiceDetailInfoResponse from '@/entity/response/service_detail/ServiceDetailResponse';
import { RedMapDataCalculator } from '@/entity/view/RedMapDataCalculator';
import ServiceOverviewResponse from "@/entity/response/service_detail/ServiceOverviewResponse";
import { hideLoading, setOptionWithResize, showLoading } from "@/utils/echarts-utils";
import { reqAppOverviewDetail, reqServiceDetail } from '@/api/service/req'
import { calculateDateRangeByDiff, } from '@/utils/date-utils'
import { isTimeCompareMode, needRequestCompareData, pickMachineIdForApiParams } from "@/helper/request-helper";
import { formatTinyNumber } from '@/utils/number-utils';
import { getComparableEchartsOption } from "@/helper/echarts-option-helper";
import { handleErrorResponse, isEmptyResponse } from "@/utils/response-utils";
import EchartsRedCompareOption from "@/entity/dto/EchartsRedCompareOption";
import {eventBus} from "@/events/event-bus";
import {mapState} from "pinia";
import {useAppStore} from "@/store/store";


const CHART_IDS = Object.freeze({
  DURATION: 'service-duration-chart',
  COUNT: 'service-count-chart',
  ERROR: 'service-error-chart',
});

export default {
  data() {
    return {
      compareDateValue: {},
      isLoading: false, //card loading
      serviceOverview: new ServiceOverviewResponse(),

      mainChartData: new RedMapDataCalculator(),
      compareChartData: new RedMapDataCalculator(),

    }
  },
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
    compareMode: {
      handler() {
        this.handleCompareControlChange();
      }
    }
  },
  mounted() {
    useAppStore().showCompareControl = true;
    eventBus.on('machine-change', this.handleMachineChange);
    eventBus.on('compare-confirm', this.handleCompareDateConfirm);
    if (this.serviceId) {
      this.requestServiceOverview();
      this.requestServiceDetail();
    }
  },
  beforeDestroy() {
    eventBus.off('machine-change', this.handleMachineChange);
    eventBus.off('compare-confirm', this.handleCompareDateConfirm);
  },

  methods: {
    formatTinyNumber,
    //Event Handler part==================================
    handleMachineChange() {
      this.requestServiceOverview();
      this.requestServiceDetail();
    },
    async handleCompareDateConfirm() {
      await this.requestServiceDetail()
    },
    async handleCompareControlChange() {
      if (needRequestCompareData(this.compareMode, this.compareDate, this.machineIds)) {
        this.$nextTick(() => {
          this.requestServiceDetail();
        })
      }
    },
    //Event Handler End==================================

    //Data request part==================================
    async requestServiceOverview() {
      let params = {
        startDate: this.freshTime[0],
        endDate: this.freshTime[1],
        serviceName: this.serviceId,
      }
      this.isLoading = true;
      let res = await reqAppOverviewDetail(params).finally(() => { this.isLoading = false });
      if (isEmptyResponse(res)) {
        return;
      }
      this.serviceOverview = new ServiceOverviewResponse(res.data);
    },
    async requestServiceDetail() {
      let params = {
        startDate: this.freshTime[0],
        endDate: this.freshTime[1],
        serviceName: this.serviceId,
        machineId: pickMachineIdForApiParams(this.compareMode, this.compareDate, false, this.machineId, this.machineIds)
      }
      try {
        showLoading(Object.values(CHART_IDS))
        let res = await reqServiceDetail(params)
        if (isEmptyResponse(res)) {
          this.mainChartData.clear();
          this.initEcharts();
          return;
        }

        const entityList = res.data.map(item => new ServiceDetailInfoResponse(item));
        this.mainChartData.load(entityList);

        if (needRequestCompareData(this.compareMode, this.compareDate, this.machineIds)) {
          await this.requestServiceDetail4Compare();
        } else {
          this.$nextTick(() => {
            this.initEcharts()
          })
        }
      } catch (err) {
        handleErrorResponse(err, this.$t('service.fetchChartError'));
      }
    },
    async requestServiceDetail4Compare() {
      let params
      if (isTimeCompareMode(this.compareMode)) {
        this.compareDateValue = calculateDateRangeByDiff(this.freshTime[0], this.freshTime[1], this.compareDate);
        params = {
          startDate: this.compareDateValue.startDate,
          endDate: this.compareDateValue.endDate,
          serviceName: this.serviceId,
          machineId: pickMachineIdForApiParams(this.compareMode, this.compareDate, false, this.machineId, this.machineIds)
        }
      } else {
        params = {
          startDate: this.freshTime[0],
          endDate: this.freshTime[1],
          serviceName: this.serviceId,
          machineId: pickMachineIdForApiParams(this.compareMode, this.compareDate, true, this.machineId, this.machineIds)
        }
      }
      try {
        let res = await reqServiceDetail(params)
        if (isEmptyResponse(res)) {
          this.compareChartData.clear();
          this.initEcharts();
          this.$Message.error(this.$t('service.noCompareData'));
          return;
        }
        const entityList = res.data.map(item => new ServiceDetailInfoResponse(item));
        this.compareChartData.load(entityList);
        this.initEcharts(true)
      } catch (err) {
        handleErrorResponse(err, this.$t('service.fetchCompareError'));
      }
    },
    //Data request End=========================================

    //Echarts part====================
    initEcharts(isCompare) {
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
    },
    //Echarts End========================
  }
}
</script>

<style lang="less" scoped>
.notice {
  background: #fff;
  padding: 10px;
  border-radius: 5px;
  margin-bottom: 10px;
}

.metric-card {
  cursor: pointer;
  transition: all 0.3s;
  overflow: hidden;
}

.metric-card:hover {
  transform: translateY(-5px);
}

.card-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 5px;
}

.card-title {
  color: #808695;
  font-size: 14px;
  margin-bottom: 5px;
  font-weight: 500;
}

.card-value {
  font-size: 30px;
  font-weight: bold;
  margin: 0;
  line-height: 1.2;
}

.unit {
  font-size: 16px;
  color: #9ea7b4;
  font-weight: normal;
}

.right-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.color-primary {
  color: #2d8cf0;
}

.bg-primary-light {
  background-color: rgba(45, 140, 240, 0.1);
}

.color-duration {
  color: #32d2c9;
}

.bg-warning-light {
  background-color: rgba(255, 153, 0, 0.1);
}

.color-error {
  color: #ed4014;
}

.bg-error-light {
  background-color: rgba(237, 64, 20, 0.1);
}

.main-title {
  padding: 18px 26px;
  position: relative;
  font-size: 18px;
  font-weight: bold;
}

.main-title::before {
  width: 2px;
  height: 17px;
  background: #409eff;
  border-radius: 10px;
  content: '';
  position: absolute;
  left: 19px;
  top: 23px;
}
</style>
