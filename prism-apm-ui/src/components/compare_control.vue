<template>
  <div class="compare-control-container">
    <RadioGroup :value="mode" @on-change="handleModeChange" type="button" size="small">
      <Radio label="HOST">
        <span>{{ $t('compare.hostMode') }}</span>
      </Radio>
      <Radio label="TIME">
        <span>{{ $t('compare.timeMode') }}</span>
      </Radio>
    </RadioGroup>

    <Tooltip placement="top" max-width="250" transfer>
      <Icon type="ios-information-circle-outline" class="info-icon" />
      <div slot="content">
        <p class="mb5"><b>{{ $t('compare.hostMode') }} ({{ $t('compare.default') }}):</b> {{ $t('compare.hostModeDesc')
        }}</p>
        <p><b>{{ $t('compare.timeMode') }}:</b> {{ $t('compare.timeModeDesc') }}</p>
      </div>
    </Tooltip>

    <span v-show="isCompareTime" class="date-controls">
      <span class="date-width">
        <DatePicker :type="pickerType" :format="pickerFormat" v-model="internalDate" @on-change="handleDateChange"
          @on-ok="handleDateConfirm" value-format="yyyy-MM-dd HH:mm:ss" placeholder="Select compare time" size="small"
          :options="dateOptions"></DatePicker>
      </span>

      <Tooltip placement="top" transfer max-width="300" class="ml5">
        <i-switch v-model="align" @on-change="handleAlignChange" size="small"></i-switch>

        <div slot="content">
          <p><b>{{ $t('compare.on') }}:</b> {{ $t('compare.alignOnDesc') }}</p>
          <p><b>{{ $t('compare.off') }}:</b> {{ $t('compare.alignOffDesc') }}</p>
        </div>
      </Tooltip>
    </span>
  </div>
</template>

<script>
import { getDatePart, getTimePart } from '@/utils/date-utils'
import {COMPARE_MODES} from "@/constants/compare-mode";

export default {
  name: "CompareControl",
  model: {
    prop: 'mode',
    event: 'mode-change'
  },
  data() {
    return {
      align: true,
      internalDate: this.date
    }
  },
  watch: {
    date: {
      immediate: true,
      handler(val) {
        this.internalDate = val;
      }
    }
  },
  props: {
    mode: {
      type: String,
      default: COMPARE_MODES.HOST
    },
    date: {
      type: String,
      default: ''
    },
    referenceTime: {
      type: String,
      required: false,
      default: ''
    }
  },
  computed: {
    isCompareTime() {
      return this.mode === COMPARE_MODES.TIME;
    },
    pickerType() {
      return this.align ? 'date' : 'datetime';
    },
    pickerFormat() {
      return this.align ? 'yyyy-MM-dd HH:mm:ss' : 'yyyy-MM-dd HH:mm:ss';
    },
    dateOptions() {
      return {};
    }
  },
  methods: {
    handleModeChange(val) {
      this.$emit('mode-change', val);
      this.$emit('change', val);
    },
    handleAlignChange(val) {
      if (val && this.date && this.referenceTime) {
        this.triggerAlignLogic(this.date);
      }
    },
    handleDateChange(dateStr) {
      if (this.align && this.referenceTime && dateStr) {
        this.triggerAlignLogic(dateStr);
      } else {
        this.$emit('update:date', dateStr);
      }
      if (this.align) {
        this.$nextTick(() => {
          this.handleDateConfirm();
        });
      }
    },
    triggerAlignLogic(inputDateStr) {
      const datePart = getDatePart(inputDateStr);
      const timePart = getTimePart(this.referenceTime);
      const alignedDate = `${datePart} ${timePart}`;
      this.internalDate = alignedDate;
      this.$emit('update:date', alignedDate);
    },
    handleDateConfirm() {
      this.$emit('confirm');
    }
  }
}
</script>

<style scoped>
.compare-control-container {
  display: inline-block;
}

.info-icon {
  margin: 0 5px;
  cursor: pointer;
  vertical-align: middle;
}

.date-controls {
  margin-left: 5px;
}

.date-width {
  display: inline-block;
  width: 168px;
}

.ml5 {
  margin-left: 5px;
}

.mb5 {
  margin-bottom: 5px;
}
</style>