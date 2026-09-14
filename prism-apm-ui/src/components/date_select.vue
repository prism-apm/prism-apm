<template>
  <div class="date_select" @click.stop id="dateSelect">
    <div class="date-trigger" @click="toggleShowDate" @mouseenter="mouseEnter" @mouseleave="mouseLeave" :class="{ 'is-active': showDate }">
      <Icon type="ios-time-outline" class="main-icon" />
      <div class="date-trigger-content">
        <span class="range-text">{{ whichRange }}</span>
        <span class="refresh-badge" v-if="autoRefreshDisplay">{{ autoRefreshDisplay }}</span>
      </div>
      <Icon type="ios-arrow-down" class="arrow-icon" :class="{ 'is-open': showDate }" />
    </div>

    <div class="refresh-now-btn" @click="refreshNow" title="Refresh Now">
      <Icon type="md-refresh" />
    </div>

    <transition name="trans-date-selector">
      <div class="pop-container" id='popContainer' v-show="showTip && !showDate">
        <div class="pop-arrow"></div>
        <div class="pop-content">
          <div class="time-row">
            <span class="label">Start:</span>
            <span class="value">{{ startDateStr }}</span>
          </div>
          <div class="time-row">
            <span class="label">End:</span>
            <span class="value">{{ endDateStr }}</span>
          </div>
          <div class="time-row" v-if="selectedTimezone">
            <span class="label">TZ:</span>
            <span class="value">{{ selectedTimezone }}</span>
          </div>
        </div>
      </div>
    </transition>

    <transition name="trans-date-pop">
      <div id="pop-div" class="modern-pop-div" v-show="showDate">
        <div class="pop-body">
          <!-- Left: Custom Ranges -->
          <div class="sidebar">
            <h3 class="section-title"><Icon type="md-options" /> Custom Range</h3>
            
            <div class="form-group">
              <label>FROM <span style="font-size: 10px; color: #999; font-weight: normal;">(yyyy-MM-dd HH:mm)</span></label>
              <DatePicker type="datetime"
                          v-model="startDate"
                          @on-change="fromDateChange"
                          format="yyyy-MM-dd HH:mm"
                          placeholder="Select start time" class="custom-datepicker"></DatePicker>
            </div>

            <div class="form-group">
              <label>TO <span style="font-size: 10px; color: #999; font-weight: normal;">(yyyy-MM-dd HH:mm)</span></label>
              <DatePicker type="datetime"
                          v-model="endDate"
                          @on-change="toDateChange"
                          format="yyyy-MM-dd HH:mm"
                          placeholder="Select end time" class="custom-datepicker"></DatePicker>
            </div>

            <div class="form-group">
              <label>AUTO REFRESH</label>
              <div class="refresh-controls">
                <Select class="select-refresh" v-model="refreshTime" filterable>
                  <Option v-for="item in autoRefreshOptions" :value="item.time + '_' + item.title" :key="item.time"
                          :label="item.title"></Option>
                </Select>
                <Button type="success" @click="apply" class="apply-btn">APPLY</Button>
              </div>
            </div>
          </div>

          <!-- Right: Quick Ranges -->
          <div class="main-content">
            <h3 class="section-title"><Icon type="md-flash" /> Quick Ranges</h3>
            <div class="quick-grid">
              <div class="quick-column">
                <div class="column-header">Past Periods</div>
                <div class="quick-item" :class="{'active': whichRange === 'Last 2 days'}" @click="quickLastRange('Last 2 days')">Last 2 days</div>
                <div class="quick-item" :class="{'active': whichRange === 'Last 7 days'}" @click="quickLastRange('Last 7 days')">Last 7 days</div>
                <div class="quick-item" :class="{'active': whichRange === 'Last 30 days'}" @click="quickLastRange('Last 30 days')">Last 30 days</div>
                <div class="quick-item" :class="{'active': whichRange === 'Last 90 days'}" @click="quickLastRange('Last 90 days')">Last 90 days</div>
              </div>
              
              <div class="quick-column">
                <div class="column-header">Previous</div>
                <div class="quick-item" :class="{'active': whichRange === 'Yesterday'}" @click="quickRange('Yesterday', 1)">Yesterday</div>
                <div class="quick-item" :class="{'active': whichRange === 'Day before yesterday'}" @click="quickRange('Day before yesterday', 2)">Day before yesterday</div>
                <div class="quick-item" :class="{'active': whichRange === 'This day last week'}" @click="quickRange('This day last week', 7)">This day last week</div>
                <div class="quick-item" :class="{'active': whichRange === 'Previous week'}" @click="quickLastWeekRange('Previous week')">Previous week</div>
              </div>

              <div class="quick-column">
                <div class="column-header">Current</div>
                <div class="quick-item" :class="{'active': whichRange === 'Today'}" @click="quickRange('Today', 0)">Today</div>
                <div class="quick-item" :class="{'active': whichRange === 'Today so far'}" @click="todaySoFarRange('Today so far')">Today so far</div>
                <div class="quick-item" :class="{'active': whichRange === 'This week'}" @click="thisWeekRange('This week')">This week</div>
                <div class="quick-item" :class="{'active': whichRange === 'This week so far'}" @click="thisWeekSoFarRange('This week so far')">This week so far</div>
              </div>

              <div class="quick-column">
                <div class="column-header">Recent</div>
                <div class="quick-item-row">
                  <div class="quick-item" :class="{'active': whichRange === 'Last 5 minutes'}" @click="quickLastRange('Last 5 minutes')">5 mins</div>
                  <div class="quick-item" :class="{'active': whichRange === 'Last 15 minutes'}" @click="quickLastRange('Last 15 minutes')">15 mins</div>
                </div>
                <div class="quick-item-row">
                  <div class="quick-item" :class="{'active': whichRange === 'Last 30 minutes'}" @click="quickLastRange('Last 30 minutes')">30 mins</div>
                  <div class="quick-item" :class="{'active': whichRange === 'Last 1 hours'}" @click="quickLastRange('Last 1 hours')">1 hour</div>
                </div>
                <div class="quick-item-row">
                  <div class="quick-item" :class="{'active': whichRange === 'Last 3 hours'}" @click="quickLastRange('Last 3 hours')">3 hours</div>
                  <div class="quick-item" :class="{'active': whichRange === 'Last 6 hours'}" @click="quickLastRange('Last 6 hours')">6 hours</div>
                </div>
                <div class="quick-item" :class="{'active': whichRange === 'Last 12 hours'}" @click="quickLastRange('Last 12 hours')">12 hours</div>
              </div>
            </div>
          </div>
        </div>
        
        <div class="pop-footer">
          <div class="footer-left">
             <span class="tz-label">Timezone:</span>
             <Select v-model="selectedTimezone" filterable clearable transfer placement="top-start" placeholder="Select Timezone" size="small" class="footer-tz-select">
                <Option v-for="item in timezoneList" :value="item.value" :key="item.value" :label="item.label">
                  <span>{{ item.label }}</span>
                </Option>
              </Select>
          </div>
          <div class="footer-right">
            <Icon type="ios-information-circle-outline" />
            <span class="logic-text">Logic: <code>[Start, End)</code> &mdash; <code>Time &ge; Start AND Time &lt; End</code></span>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>
<script>
import {formateDate, getPastDate, getThisWeekDay, truncateTimeStrToMinute} from '@/utils/date-utils.ts'
import {reqTimeAxis} from "@/api/common/req";
import {TIMEZONE_DATA} from "@/constants/timezone";
import {mapWritableState} from "pinia";
import {useAppStore} from "@/store/store";

const AUTO_REFRESH_SWITCH_OFF = "0_off";
const AUTO_REFRESH_OPTIONS = [
  { time: 0, title: 'off' },
  { time: 5, title: '5s' },
  { time: 10, title: '10s' },
  { time: 30, title: '30s' },
  { time: 60, title: '1m' },
  { time: 300, title: '5m' },
  { time: 900, title: '15m' },
  { time: 1800, title: '30m' },
  { time: 3600, title: '1h' },
  { time: 7200, title: '2h' },
  { time: 86400, title: '1d' }
];
const QUICK_RANGES_MAP = {
  'Last 5 minutes': 5,
  'Last 15 minutes': 15,
  'Last 30 minutes': 30,
  'Last 1 hours': 60,
  'Last 3 hours': 180,
  'Last 6 hours': 360,
  'Last 12 hours': 720,
  'Last 2 days': 2880,
  'Last 7 days': 10080,
  'Last 30 days': 43200,
  'Last 90 days': 129600,
  'Today': 0,
  'Yesterday': 1,
  'Day before yesterday': 2,
  'This day last week': 7,
}

export default {
  name: 'DateSelect',
  data() {
    return {
      showDate: false,
      startDate: '',
      endDate: '',
      startDateStr: '-',
      endDateStr: '-',
      showTip: false,
      whichRange: 'Last 30 minutes',
      quickSelectNumber: 30,
      refreshFn: '',
      autoRefreshOptions: AUTO_REFRESH_OPTIONS,
      refreshTime: AUTO_REFRESH_SWITCH_OFF,
      autoRefreshDisplay: '',
      timer: null,
      lastOperator: -1,
      timezoneList: []
    }
  },
  computed: {
    ...mapWritableState(useAppStore, {
      selectedTimezone: 'timezone',
    }),
  },
  created() {
    const timezoneNameKey = this.$i18n.locale === 'en-US' ? 'enName' : 'zhName';
    this.timezoneList = TIMEZONE_DATA.map(item => {
      return {
        value: item.iana,
        label: `(GMT${item.offset}) ${item[timezoneNameKey]} - ${item.iana}`,
        name: item.iana
      };
    });
  },
  mounted() {
    document.body.addEventListener('click', () => {
      this.showDate = false
    }, false)

    // --- Timezone Detection Logic ---
    if (!this.selectedTimezone) {
      try {
        const sysTz = Intl.DateTimeFormat().resolvedOptions().timeZone;
        const matched = this.timezoneList.find(t => t.name === sysTz);
        if (matched) {
          this.selectedTimezone = matched.value;
        } else {
          this.selectedTimezone = 'UTC';
        }
      } catch (e) {
        console.warn('Timezone detection failed, defaulting to UTC', e);
        this.selectedTimezone = 'UTC';
      }
    }
    // --------------------------------

    let from_share = Object.keys(this.$route.query).length > 0;
    if (from_share) {
      this.startDateStr = useAppStore().freshTime[0];
      this.endDateStr = useAppStore().freshTime[1];
      this.startDate = new Date(this.startDateStr)
      this.endDate = new Date(this.endDateStr)
      this.formatWhichRange(false);

      this.refreshTime = AUTO_REFRESH_SWITCH_OFF
      this.autoRefreshDisplay = ''
      this.destroyTimer()
      this.lastOperator = 0
      this.dispatchTime();
    } else {
      this.quickLastRange('Last 30 minutes', 30);
    }
  },
  methods: {
    apply() {
      if (this.startDate) {
        const s = new Date(this.startDate);
        s.setSeconds(0, 0);
        this.startDate = s;
      }
      if (this.endDate) {
        const e = new Date(this.endDate);
        e.setSeconds(0, 0);
        this.endDate = e;
      }

      if (!this.startDate || !this.endDate) {
        this.$Message.warning('Please select both Start and End time.');
        return;
      }

      const startTime = new Date(this.startDate).getTime();
      const endTime = new Date(this.endDate).getTime();

      if (startTime > endTime) {
        this.$Message.error('Start time cannot be greater than End time.');
        return;
      }

      if ((endTime - startTime) < 60000) {
        this.$Message.warning('The time span must be at least 60 seconds.');
        return;
      }

      this.startDateStr = formateDate(this.startDate, true)
      this.endDateStr = formateDate(this.endDate, true)
      this.destroyTimer()

      if (this.lastOperator === 0) {
        this.refreshTime = AUTO_REFRESH_SWITCH_OFF
        this.formatWhichRange(false);
        this.autoRefreshDisplay = ''
      } else {
        if (this.refreshTime !== AUTO_REFRESH_SWITCH_OFF) {
          this.autoRefreshDisplay = 'Refreshing every ' + this.refreshTime.split('_')[1]
          this.startAutoRefresh(this.refreshTime.split('_')[0])
        } else {
          this.autoRefreshDisplay = ''
        }
      }
      this.dispatchTime()
      this.showDate = false
    },
    mouseEnter() {
      this.showTip = true
    },
    mouseLeave() {
      this.showTip = false
    },
    quickLastRange(name) {
      this.refreshFn = 'quickLastRange'
      this.lastOperator = 1
      this.whichRange = name
      this.quickSelectNumber = QUICK_RANGES_MAP[name]
      if (this.refreshTime !== AUTO_REFRESH_SWITCH_OFF) {
        this.autoRefreshDisplay = 'Refreshing every ' + this.refreshTime.split('_')[1]
      } else {
        this.autoRefreshDisplay = ''
        this.destroyTimer()
      }
      this.startDateStr = truncateTimeStrToMinute(getPastDate('minutes', this.quickSelectNumber, true));
      this.endDateStr = truncateTimeStrToMinute(getPastDate('minutes', 0, true));
      this.startDate = new Date(this.startDateStr);
      this.endDate = new Date(this.endDateStr);
      this.dispatchTime()
      this.showDate = false
    },
    quickRange(name) {
      this.whichRange = name
      this.refreshFn = 'quickRange'
      this.quickSelectNumber = QUICK_RANGES_MAP[name]
      this.startDateStr = getPastDate('days', this.quickSelectNumber, false) + ' 00:00:00'
      this.endDateStr = getPastDate('days', this.quickSelectNumber - 1, false) + ' 00:00:00'
      this.startDate = new Date(this.startDateStr)
      this.endDate = new Date(this.endDateStr)
      this.dispatchTime()
      this.showDate = false
    },
    todaySoFarRange(name) {
      this.refreshFn = 'todaySoFarRange'
      this.whichRange = name
      this.startDateStr = getPastDate('days', 0, false) + ' 00:00:00'
      this.endDateStr = truncateTimeStrToMinute(getPastDate('days', 0, true))
      this.startDate = new Date(this.startDateStr)
      this.endDate = new Date(this.endDateStr)
      this.dispatchTime()
      this.showDate = false
    },
    quickLastWeekRange(name) {
      this.refreshFn = 'quickLastWeekRange'
      this.whichRange = name
      this.startDateStr = getPastDate('days', getThisWeekDay() + 6, false) + ' 00:00:00'
      this.endDateStr = getPastDate('days', getThisWeekDay() - 1, false) + ' 00:00:00'
      this.startDate = new Date(this.startDateStr)
      this.endDate = new Date(this.endDateStr)
      this.dispatchTime()
      this.showDate = false
    },
    thisWeekRange(name) {
      this.refreshFn = 'thisWeekRange'
      this.whichRange = name

      const currentWeekDay = getThisWeekDay()

      this.startDateStr = getPastDate('days', currentWeekDay - 1, false) + ' 00:00:00'
      this.endDateStr = getPastDate('days', currentWeekDay - 8, false) + ' 00:00:00'

      this.startDate = new Date(this.startDateStr)
      this.endDate = new Date(this.endDateStr)
      this.dispatchTime()
      this.showDate = false
    },
    thisWeekSoFarRange(name) {
      this.refreshFn = 'thisWeekSoFarRange'
      this.whichRange = name
      this.startDateStr = getPastDate('days', getThisWeekDay() - 1, false) + ' 00:00:00'
      this.endDateStr = truncateTimeStrToMinute(getPastDate('days', 0, true));
      this.startDate = new Date(this.startDateStr)
      this.endDate = new Date(this.endDateStr)
      this.dispatchTime()
      this.showDate = false
    },
    fromDateChange() {
      this.lastOperator = 0
      this.refreshFn = 'timeSelect'
    },
    toDateChange() {
      this.lastOperator = 0
      this.refreshFn = 'timeSelect'
    },
    timeSelect() {
      this.dispatchTime()
    },
    async dispatchTime() {
      useAppStore().freshTime = [this.startDateStr, this.endDateStr];
      useAppStore().timeValueList = await this.fetchTimeAxis();
    },
    toggleShowDate() {
      this.showDate = !this.showDate
    },
    destroyTimer() {
      if (this.timer) window.clearInterval(this.timer)
    },
    refreshNow() {
      this[this.refreshFn](this.whichRange, this.quickSelectNumber)
    },
    startAutoRefresh(time) {
      this.timer = setInterval(() => {
        this[this.refreshFn](this.whichRange, this.quickSelectNumber)
      }, time * 1000)
    },
    async fetchTimeAxis() {
      let param = {startDate: this.startDateStr, endDate: this.endDateStr};
      let res = await reqTimeAxis(param);
      return res.data;
    },
    formatWhichRange(isQuickRange, quickName) {
      if (isQuickRange) {
        this.whichRange = quickName;
      } else {
        this.whichRange = `${this.startDateStr.substring(0, 16)} to ${this.endDateStr.substring(0, 16)}`;
      }
    }
  }
}
</script>
<style lang="less">
.date_select {
  display: inline-flex;
  align-items: center;
  position: relative;
  z-index: 100;
  margin-right: 10px; /* Consistent with Share and Logout distance */
  gap: 2px; /* Match the distance between other buttons */
  vertical-align: middle;
  height: 29px; /* Anchor the container height */

  .date-trigger {
    display: inline-flex;
    align-items: center;
    padding: 0 12px;
    height: 29px; /* Explicit height */
    cursor: pointer;
    user-select: none;
    background: #fff;
    border: 1px solid #c7d0d9;
    border-radius: 4px;
    transition: all 0.2s;
    box-sizing: border-box;

    &:hover {
      border-color: #57a3f3;
      color: #57a3f3;
      .main-icon, .arrow-icon { color: #57a3f3; }
    }

    &.is-active {
      border-color: #2d8cf0;
      box-shadow: 0 0 0 2px rgba(45, 140, 240, 0.2);
    }

    .main-icon {
      font-size: 16px;
      color: #515a6e;
      margin-right: 8px;
    }

    .date-trigger-content {
      display: flex;
      align-items: center;
      min-width: 100px; /* Reduced min-width to be more flexible */

      .range-text {
        font-size: 13px;
        color: #515a6e;
        font-weight: normal;
        white-space: nowrap;
      }

      .refresh-badge {
        font-size: 11px;
        color: #ff9900;
        margin-left: 8px;
        font-weight: bold;
      }
    }

    .arrow-icon {
      font-size: 14px;
      color: #808695;
      margin-left: 8px;
      transition: transform 0.2s ease;

      &.is-open {
        transform: rotate(180deg);
      }
    }
  }

  .refresh-now-btn {
    width: 32px;
    height: 29px; /* Explicit height */
    display: inline-flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    color: #515a6e;
    transition: all 0.2s;
    background: #fff;
    border: 1px solid #c7d0d9;
    border-radius: 2px;
    box-sizing: border-box;
    flex-shrink: 0;

    &:hover {
      border-color: #57a3f3;
      color: #57a3f3;
    }
    
    &:active {
      background: #f8f8f9;
    }
  }

  .modern-pop-div {
    position: absolute;
    top: calc(100% + 5px);
    right: 0;
    width: 800px;
    background: #fff;
    border-radius: 4px;
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1), 0 2px 8px rgba(0, 0, 0, 0.05);
    border: 1px solid #dcdee2;
    animation: slideDown 0.2s cubic-bezier(0.23, 1, 0.32, 1);

    .pop-body {
      display: flex;
    }

    .sidebar {
      width: 260px;
      background: #f8f9fa;
      border-right: 1px solid #e8eaec;
      padding: 16px;

      .form-group {
        margin-bottom: 15px;

        label {
          display: block;
          font-size: 11px;
          color: #808695;
          margin-bottom: 4px;
          font-weight: 600;
        }

        .custom-select, .custom-datepicker {
          width: 100%;
        }

        .refresh-controls {
          display: flex;
          gap: 6px;

          .select-refresh {
            flex: 1;
          }
        }
      }
    }

    .main-content {
      flex: 1;
      padding: 16px;

      .quick-grid {
        display: grid;
        grid-template-columns: 100px 130px 120px 1fr;
        gap: 15px;
      }

      .quick-column {
        .column-header {
          font-size: 11px;
          font-weight: 600;
          color: #adb5bd;
          margin-bottom: 8px;
          padding-bottom: 4px;
          border-bottom: 1px solid #f1f3f5;
        }

        .quick-item-row {
          display: flex;
          gap: 8px;
          margin-bottom: 2px;
          
          .quick-item {
            flex: 1;
            margin: 0;
          }
        }

        .quick-item {
          padding: 4px 8px;
          margin-bottom: 2px;
          border-radius: 4px;
          cursor: pointer;
          font-size: 12px;
          color: #515a6e;
          transition: all 0.2s;
          white-space: nowrap;

          &:hover {
            background: #f1f3f5;
            color: #2d8cf0;
          }

          &.active {
            background: #e7f5ff;
            color: #228be6;
            font-weight: 600;
          }
        }
      }
    }

    .section-title {
      font-size: 13px;
      font-weight: 600;
      color: #17233d;
      margin-bottom: 12px;
      display: flex;
      align-items: center;
      gap: 6px;

      i {
        font-size: 15px;
        color: #2d8cf0;
      }
    }

    .pop-footer {
      padding: 8px 16px;
      background: #f8f9fa;
      border-top: 1px solid #e8eaec;
      display: flex;
      justify-content: space-between;
      align-items: center;

      .footer-left {
        display: flex;
        align-items: center;
        gap: 8px;

        .tz-label {
          font-size: 11px;
          color: #808695;
          font-weight: 600;
          white-space: nowrap;
        }

        .footer-tz-select {
          width: 320px;
          
          .ivu-select-selection {
            background: #fff;
            border-radius: 4px;
          }

          /* Handle long timezone strings */
          .ivu-select-placeholder, .ivu-select-selected-value {
            font-size: 11px;
            padding-right: 20px;
          }
        }
      }

      .footer-right {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 11px;
        color: #adb5bd;

        .logic-text {
          font-size: 11px;
          white-space: nowrap;
        }

        code {
          background: #e9ecef;
          padding: 1px 4px;
          border-radius: 3px;
          color: #808695;
        }
      }
    }
  }
}


/* Tooltip style reverted as requested */
.pop-container {
  position: absolute;
  z-index: 1000;
  background: rgba(0, 131, 179, 0.75);
  color: #fff;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 8px 12px;
  top: 40px;
  min-width: 210px;
  font-size: 12px;
  line-height: 1.5;
  text-align: left;
  left: 50%;
  transform: translateX(-50%);

  .pop-arrow {
    position: absolute;
    top: -6px;
    left: 50%;
    transform: translateX(-50%);
    width: 0;
    height: 0;
    border-left: 6px solid transparent;
    border-right: 6px solid transparent;
    border-bottom: 6px solid rgba(0, 131, 179, 0.75);
  }

  .pop-content {
    .time-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 2px;

      &:last-child {
        margin-bottom: 0;
      }

      .label {
        opacity: 0.7;
        margin-right: 10px;
      }

      .value {
        font-family: Monaco, Menlo, Consolas, "Courier New", monospace;
        font-weight: 500;
      }
    }
  }
}

@keyframes slideDown {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

.drop-enter-active, .drop-leave-active {
  transition: all 0.2s ease;
}
.drop-enter, .drop-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}


</style>