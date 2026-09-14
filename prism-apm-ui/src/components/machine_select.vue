<template>
  <div class="select-container">
    <Select v-if="isSingleMode" v-model="internalValue" class="service-option" filterable
      @on-change="handleSingleChange" @on-open-change="$emit('on-open-change', $event)">
      <Option v-for="item in machineList" :key="item" :value="item" :label="item"></Option>
    </Select>
    <Select v-else v-model="internalValue" :multiple="true" class="multi-service-option"
      @on-change="handleMultipleChange" @on-open-change="$emit('on-open-change', $event)">
      <Option v-for="item in machineList" :key="item" :value="item" :label="item"></Option>
    </Select>
    <Spin :fix="true" v-if="isLoading" class="custom-spin">
      <Icon type="ios-loading" size="18" class="spin-icon-load"></Icon>
    </Spin>
  </div>
</template>

<script>
import { reqMachineList } from '@/api/common/req'
import { DEFAULT_MACHINE_ID } from "@/constants/default-vals";
import { handleErrorResponse, isEmptyResponse } from "@/utils/response-utils";

export default {
  name: 'MachineSelect',
  props: {
    value: {
      type: [String, Array],
      default: () => DEFAULT_MACHINE_ID
    },
    isSingleMode: {
      type: Boolean,
      default: false
    },
    queryParams: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      internalValue: this.value,
      machineList: [DEFAULT_MACHINE_ID],
      isLoading: false
    };
  },
  watch: {
    value: {
      handler(newValue) {
        this.internalValue = newValue;
      },
      deep: true
    },
    queryParams: {
      handler(newParams, oldParams) {
        if (newParams && newParams.serviceName && JSON.stringify(newParams) !== JSON.stringify(oldParams)) {
          this.fetchMachineList('query');
        }
      },
      deep: true,
      immediate: true
    }
  },
  methods: {
    async fetchMachineList(cause) {
      if (!this.queryParams || !this.queryParams.serviceName) {
        this.machineList = [DEFAULT_MACHINE_ID];
        this.internalValue = this.isSingleMode ? DEFAULT_MACHINE_ID : [DEFAULT_MACHINE_ID];
        this.emitChanges(cause, this.internalValue);
        return;
      }

      const defaultValue = this.isSingleMode ? DEFAULT_MACHINE_ID : [DEFAULT_MACHINE_ID];
      this.internalValue = defaultValue;
      this.emitChanges(cause, defaultValue);

      this.isLoading = true;
      try {
        const res = await reqMachineList(this.queryParams);
        if (isEmptyResponse(res)) {
          this.machineList = [DEFAULT_MACHINE_ID];
          return;
        }
        this.machineList = [DEFAULT_MACHINE_ID, ...res.data];
      } catch (err) {
        this.machineList = [DEFAULT_MACHINE_ID];
        handleErrorResponse(err, this.$t('machineSelect.fetchError'));
      } finally {
        this.isLoading = false;
      }
    },

    handleSingleChange(currentValue) {
      this.emitChanges('component', currentValue);
    },
    handleMultipleChange(currentValue) {
      let finalValue = [...currentValue];
      const lastSelectedItem = finalValue.length > 0 ? finalValue[finalValue.length - 1] : null;

      if (lastSelectedItem === DEFAULT_MACHINE_ID) {
        finalValue = [DEFAULT_MACHINE_ID];
      } else if (finalValue.length === 0) {
        finalValue = [DEFAULT_MACHINE_ID];
      } else {
        const defaultIndex = finalValue.indexOf(DEFAULT_MACHINE_ID);
        if (defaultIndex > -1) {
          finalValue.splice(defaultIndex, 1);
        }
        if (finalValue.length > 2) {
          this.$Message.error(this.$t('machineSelect.maxTwoHosts'));
          finalValue = finalValue.slice(0, 2);
        }
      }

      this.internalValue = finalValue;

      if (JSON.stringify(finalValue) !== JSON.stringify(this.value)) {
        this.emitChanges('component', finalValue);
      }
    },

    emitChanges(cause, value) {
      let emit_value = {
        cause: cause,
        value: value
      };
      this.$emit('input', value);
      this.$emit('change', emit_value);
    }
  }
};
</script>

<style scoped lang="less">
.multi-service-option {
  width: 100%;
  display: inline-block !important;
  vertical-align: middle;
  margin-right: 8px;

  ::v-deep .ivu-select-selection {
    background-color: #3a6b68;
    border: 1px solid rgba(255, 255, 255, 0.2);
    height: 28px !important;
    line-height: 25px !important;
    padding: 0 24px 0 4px !important;
    align-items: center;
    flex-wrap: nowrap;
    overflow: hidden;

    .ivu-icon-ios-arrow-down:before {
      color: #ffffff;
    }
  }

  ::v-deep .ivu-tag {
    background: #325e5b;
    border: 1px solid rgba(255, 255, 255, 0.2);
    font-size: 12px;
    height: 22px !important;
    line-height: 22px !important;
    margin: 0 4px 0 0 !important;
    display: inline-flex;
    align-items: center;
    max-width: 45% !important;

    .ivu-tag-text {
      color: #ffffff;
      display: inline-block;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .ivu-icon-ios-close {
      color: #ffffff;
      top: 3px;
      margin-left: 2px;
    }
  }

  &:hover ::v-deep .ivu-select-selection {
    background-color: #325e5b;
    border-color: rgba(255, 255, 255, 0.3);
  }
}

.select-container {
  position: relative;
  display: inline-block;
  width: 100%;
  vertical-align: middle;
}

.custom-spin {
  background-color: rgba(255, 255, 255, 0.5);
  border-radius: 4px;
  z-index: 10;
}

.spin-icon-load {
  animation: ani-demo-spin 1s linear infinite;
  color: #2d8cf0;
}

@keyframes ani-demo-spin {
  from { transform: rotate(0deg); }
  50% { transform: rotate(180deg); }
  to { transform: rotate(360deg); }
}
</style>