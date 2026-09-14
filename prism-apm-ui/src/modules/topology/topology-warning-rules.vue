<template>
  <div>
    <div class="mb10">
      <Button type="primary" @click="openModal('add')">{{ $t('topology.addRule') }}</Button>
    </div>
    <Table :columns="columns" :data="rules" size="small" border></Table>
    <Modal v-model="showModal" :title="modalTitle" @on-ok="handleSave" :ok-text="$t('topology.save')"
      :loading="modalLoading" width="60">
      <Form :model="formData" :label-width="80">
        <FormItem :label="$t('topology.ruleNameLabel')" prop="ruleName">
          <Input v-model="formData.ruleName" :disabled="mode === 'view'" />
        </FormItem>
        <FormItem :label="$t('topology.serviceNameLabel')" prop="serviceName">
          <Select v-model="formData.serviceName" :disabled="mode === 'view'" filterable>
            <Option v-for="service in serviceList" :value="service" :key="service">{{ service }}</Option>
          </Select>
        </FormItem>
        <FormItem :label="$t('topology.alarmLevelLabel')" prop="warningLevel">
          <Select v-model="formData.warningLevel" :disabled="mode === 'view'">
            <Option :value="2">{{ $t('topology.normal') }}</Option>
            <Option :value="1">{{ $t('topology.urgent') }}</Option>
          </Select>
        </FormItem>
        <FormItem :label="$t('topology.ruleDetailsLabel')" prop="ruleDetails">
          <div v-if="mode !== 'view'">
            <div v-for="(rule, index) in localRuleDetails" :key="index" class="rule-section mb10">
              <Row :gutter="8">
                <Col span="6">
                <Select v-model="rule.indicatorName" :placeholder="$t('topology.indicatorName')">
                  <Option value="cnt">{{ $t('topology.indicators.cnt') }}</Option>
                  <Option value="avg_duration">{{ $t('topology.indicators.avgDuration') }}</Option>
                  <Option value="max_duration">{{ $t('topology.indicators.maxDuration') }}</Option>
                  <Option value="min_duration">{{ $t('topology.indicators.minDuration') }}</Option>
                  <Option value="error_num">{{ $t('topology.indicators.errorNum') }}</Option>
                  <Option value="p90_duration">{{ $t('topology.indicators.p90Duration') }}</Option>
                  <Option value="p95_duration">{{ $t('topology.indicators.p95Duration') }}</Option>
                  <Option value="p99_duration">{{ $t('topology.indicators.p99Duration') }}</Option>
                </Select>
                </Col>
                <Col span="4">
                <Select v-model="rule.operator" :placeholder="$t('topology.operator')">
                  <Option value=">">{{ $t('topology.greaterThan') }}</Option>
                  <Option value="<">{{ $t('topology.lessThan') }}</Option>
                  <Option value=">=">{{ $t('topology.greaterEqual') }}</Option>
                  <Option value="<=">{{ $t('topology.lessEqual') }}</Option>
                  <Option value="=">{{ $t('topology.equal') }}</Option>
                  <Option value="!=">{{ $t('topology.notEqual') }}</Option>
                </Select>
                </Col>
                <Col span="6">
                <Input v-model="rule.value" :placeholder="$t('topology.threshold')" type="number" />
                </Col>
                <Col span="4" v-if="index !== localRuleDetails.length - 1">
                <Select v-model="rule.logicalOperator" :placeholder="$t('topology.logicalOperator')">
                  <Option value="AND">AND</Option>
                  <Option value="OR">OR</Option>
                </Select>
                </Col>
                <Col span="4">
                <Button type="error" @click="removeRule(index)" icon="md-close">{{ $t('topology.delete') }}</Button>
                </Col>
              </Row>
            </div>
            <Button type="dashed" long @click="addRule" icon="md-add">{{ $t('topology.addRuleBtn') }}</Button>
          </div>
          <div v-else>
            <span v-for="(rule, index) in localRuleDetails" :key="index" class="mb10">
              {{ rule.indicatorName }} {{ rule.operator }} {{ rule.value }} {{ rule.logicalOperator }}
            </span>
          </div>
        </FormItem>
      </Form>
      <div slot="footer" v-if="mode === 'view'"></div>
    </Modal>
  </div>
</template>

<script>
import { getTopologyWarningRules, switchTopologyWarningStatus, deleteTopologyWarningRule, addTopologyWarningRule, updateTopologyWarningRule } from '../../api/topology/req';
import { reqServiceList } from '../../api/common/req';
import {ERROR_TIMEOUT, FORBIDDEN, UNAUTHORIZED} from "@/constants/error-code";
import {isEmptyResponse} from "@/utils/response-utils";

export default {
  name: 'TopologyWarningRules',
  data() {
    return {
      rules: [],
      showModal: false,
      modalLoading: false,
      mode: 'add',
      serviceList: [],
      localRuleDetails: [],
      formData: {
        id: null,
        ruleName: '',
        serviceName: '',
        warningLevel: 1,
        ruleDetails: '[]'
      }
    };
  },
  computed: {
    columns() {
      return [
        { title: 'ID', key: 'id', width: 80 },
        { title: this.$t('topology.ruleName'), key: 'ruleName', minWidth: 100 },
        { title: this.$t('topology.serviceName'), key: 'serviceName', minWidth: 40 },
        {
          title: this.$t('topology.ruleDetails'), key: 'ruleDetails', minWidth: 500, render: (h, params) => {
            try {
              const ruleDetails = JSON.parse(params.row.ruleDetails);
              return h('div', ruleDetails.map(rule => {
                return h('span', `${rule.indicatorName} ${rule.operator} ${rule.value} ${rule.logicalOperator} `);
              }));
            } catch (e) {
              return h('span', { style: { color: 'red' } }, this.$t('topology.parseRuleDetailsFailed'));
            }
          }
        },
        {
          title: this.$t('topology.alarmLevel'), key: 'warningLevel', width: 100, render: (h, params) => {
            const levelMap = {
              2: { text: this.$t('topology.normal'), color: '#ff9900' },
              1: { text: this.$t('topology.urgent'), color: '#ed4014' }
            };
            const level = levelMap[params.row.warningLevel] || { text: this.$t('topology.unknown'), color: '#999' };
            return h('span', {
              style: {
                color: level.color
              }
            }, level.text);
          }
        },
        {
          title: this.$t('topology.status'), key: 'enabled', width: 80, render: (h, params) => {
            return h('span', {
              style: {
                color: params.row.enabled ? 'green' : 'red'
              }
            }, params.row.enabled ? this.$t('topology.enabled') : this.$t('topology.disabled'));
          }
        },
        {
          title: this.$t('topology.actions'),
          key: 'action',
          render: (h, params) => {
            return h('div', [
              h('Button', {
                props: {
                  type: this.rules[params.index].enabled ? 'error' : 'success',
                  size: 'small'
                },
                style: {
                  marginRight: '5px'
                },
                on: {
                  click: () => {
                    this.toggleRuleStatus(params.index);
                  }
                }
              }, this.rules[params.index].enabled ? this.$t('topology.disable') : this.$t('topology.enable')),
              h('Button', {
                props: {
                  type: 'primary',
                  size: 'small'
                },
                style: {
                  marginRight: '5px'
                },
                on: {
                  click: () => {
                    this.openModal('edit', params.index);
                  }
                }
              }, this.$t('topology.edit')),
              h('Button', {
                props: {
                  type: 'info',
                  size: 'small'
                },
                style: {
                  marginRight: '5px'
                },
                on: {
                  click: () => {
                    this.openModal('view', params.index);
                  }
                }
              }, this.$t('topology.view')),
              h('Button', {
                props: {
                  type: 'warning',
                  size: 'small'
                },
                on: {
                  click: () => {
                    this.deleteRule(params.index);
                  }
                }
              }, this.$t('topology.delete'))
            ]);
          }
        }
      ];
    },
    modalTitle() {
      const titles = {
        add: this.$t('topology.addRule'),
        edit: this.$t('topology.editRule'),
        view: this.$t('topology.viewRule')
      };
      return titles[this.mode];
    }
  },
  created() {
    this.fetchRules();
    this.fetchServiceList();
  },
  methods: {
    fetchRules() {
      getTopologyWarningRules().then(response => {
        this.rules = response.data || [];
      });
    },
    async fetchServiceList() {
      try {
        let res = await reqServiceList()
        if (isEmptyResponse(res)) {
          if (ERROR_TIMEOUT !== res.status && res.status !== UNAUTHORIZED && res.status !== FORBIDDEN) {
            this.$Message.error(this.$t('serviceSelect.fetchFailed'));
          }
          return;
        }
        this.serviceList = res.data || [];
      } catch (res) {
        console.error(this.$t('topology.fetchServiceListError'), res);
        this.$Message.error(res.err || this.$t('topology.fetchServiceListError'))
      }
    },
    toggleRuleStatus(index) {
      const rule = this.rules[index];
      switchTopologyWarningStatus(rule.id, !rule.enabled).then(() => {
        this.fetchRules();
      });
    },
    deleteRule(index) {
      const rule = this.rules[index];
      this.$Modal.confirm({
        title: this.$t('topology.confirmDelete'),
        content: this.$t('topology.confirmDeleteContent'),
        onOk: () => {
          deleteTopologyWarningRule(rule.id).then(() => {
            this.$Message.success(this.$t('topology.deleteSuccess'));
            this.fetchRules();
          });
        }
      });
    },
    openModal(mode, index) {
      this.mode = mode;
      this.showModal = true;

      if (mode === 'add') {
        this.formData = {
          id: null,
          ruleName: '',
          serviceName: '',
          ruleDetails: '[]'
        };
        this.localRuleDetails = [];
      } else {
        const rule = this.rules[index];
        this.formData = { ...rule };
        try {
          this.localRuleDetails = JSON.parse(rule.ruleDetails);
        } catch (e) {
          this.localRuleDetails = [];
          console.error('parse rule details failed:', e);
        }
      }
    },
    addRule() {
      if (this.localRuleDetails.length > 0) {
        this.localRuleDetails[this.localRuleDetails.length - 1].logicalOperator = 'AND';
      }
      this.localRuleDetails.push({
        indicatorName: 'cnt',
        operator: '>',
        value: '0',
        logicalOperator: 'AND'
      });
    },
    removeRule(index) {
      this.localRuleDetails.splice(index, 1);
    },
    async handleSave() {
      if (this.mode === 'view') {
        this.showModal = false;
        return;
      }

      this.modalLoading = true;
      try {
        if (this.localRuleDetails.length > 0) {
          this.localRuleDetails[this.localRuleDetails.length - 1].logicalOperator = '';
        }
        this.formData.ruleDetails = JSON.stringify(this.localRuleDetails);

        if (this.mode === 'add') {
          await addTopologyWarningRule(this.formData).then(() => {
            this.$Message.success(this.$t('topology.addSuccess'));
            this.fetchRules();
          });
        } else {
          await updateTopologyWarningRule(this.formData).then(() => {
            this.$Message.success(this.$t('topology.updateSuccess'));
            this.fetchRules();
          });
        }
        this.showModal = false;
      } catch (error) {
        this.$Message.error(this.$t('topology.operationFailed') + '：' + error.message);
      } finally {
        this.modalLoading = false;
      }
    }
  }
};
</script>

<style scoped lang="less">
.mb10 {
  margin-bottom: 10px;
}

.rule-section {
  padding: 10px;
  border: 1px dashed #dcdee2;
  border-radius: 4px;
}
</style>