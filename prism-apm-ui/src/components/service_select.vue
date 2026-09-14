<template>
  <Select v-model="selectedValue" class="service-option" filterable clearable @on-change="handleChange"
          @on-open-change="$emit('on-open-change', $event)">
    <Option v-for="item in serviceList" :value="item" :key="item" :label="item">
    </Option>
  </Select>
</template>

<script>
import { reqServiceList } from '@/api/common/req';
import { handleErrorResponse, isEmptyResponse } from "@/utils/response-utils";
import {ERROR_TIMEOUT, FORBIDDEN, UNAUTHORIZED} from "@/constants/error-code";
import {useAppStore} from "@/store/store";

export default {
  name: 'ServiceSelect',
  props: {
    value: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      serviceList: [],
      selectedValue: '',
    };
  },
  watch: {
    value(newValue) {
      this.selectedValue = newValue;
    },
  },
  created() {
    this.getServiceList();
  },
  mounted() {
    this.selectedValue = useAppStore().serviceName;
  },
  methods: {
    async getServiceList() {
      try {
        const res = await reqServiceList();
        if (isEmptyResponse(res)) {
          if (ERROR_TIMEOUT !== res.status && res.status !== UNAUTHORIZED && res.status !== FORBIDDEN) {
            this.$Message.error(this.$t('serviceSelect.fetchFailed'));
          }
          return;
        }
        console.debug('Service list fetched successfully, current selection:', this.selectedValue);
        this.serviceList = res.data;
        if (!this.selectedValue && this.serviceList.length > 0) {
          this.selectedValue = this.serviceList[0];
        }
        this.$emit('input', this.selectedValue);
        this.$emit('change', this.selectedValue);
      } catch (err) {
        handleErrorResponse(err, this.$t('serviceSelect.fetchError'));
      }
    },

    async handleChange(value) {
      useAppStore().serviceName = this.selectedValue;
      this.$emit('input', value);
      this.$emit('change', value);
    }
  }
};
</script>

<style scoped lang="less"></style>