<template>
  <div class="topology-wrapper">
    <transition name="trans-topo-loading">
      <div class="loading-overlay" v-if="loading">
        <div class="loading-container">
          <div class="loading-content">
            <div class="loading-icon">
              <svg class="progress-ring" viewBox="0 0 60 60">
                <circle class="progress-ring-bg" cx="30" cy="30" r="26" />
                <circle class="progress-ring-circle" cx="30" cy="30" r="26"
                  :style="{ strokeDashoffset: progressCircleOffset }" />
              </svg>
              <span class="progress-text">{{ loadingProgress }}%</span>
            </div>
            <div class="loading-bar-container">
              <div class="loading-bar">
                <div class="loading-bar-fill" :style="{ width: loadingProgress + '%' }"></div>
                <div class="loading-bar-glow"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>
    <div class="changeBtn" :style="changeBtnStyle">
      <b>{{ $t('topology.currentLayout') }}:</b> <span style="color: #d24747">{{ currentLayout.name }}</span>
      <Button id="bbb" @click="changeLayout" size="small">{{ $t('topology.switchLayout') }}</Button>
      -
      <Button id="bbc" @click="reLayout" size="small">{{ $t('topology.reLayout') }}</Button>
      |
      <Button @click="toggleHighlightRulesModal" size="small">{{ $t('topology.highlightRules') }}</Button>
    </div>
    <div class="searchBox" :style="searchBoxStyle">
      <span>
        <Select v-model="wannaToFocusNode" @on-change="focusTo" :placeholder="$t('topology.searchPlaceholder')"
          ref="nodeSelect" filterable clearable>
          <Option v-for="option in focusToOptions" :value="option.value" :key="option.value" :label="option.text">
          </Option>
        </Select>
      </span>
    </div>
    <div id="toolbar-container" class="toolbar-container"></div>
    <div id="mountNode" class="echartsMap"></div>
    <Modal v-model="isHighlightRulesModalVisible" :title="$t('topology.highlightRulesList')" scrollable width="90">
      <topology-warning-rules ref="warningRules"></topology-warning-rules>
      <div slot="footer"></div>
    </Modal>
    <Modal v-model="showEdgeAlarmModal" :title="$t('topology.alarmDetails')" width="60">
      <div v-if="currentEdgeAlarm">
        <h3>{{ $t('topology.alarmRuleInfo') }}</h3>
        <div class="alarm-rule-info">
          <p><strong>{{ $t('topology.ruleName') }}:</strong>{{ currentEdgeAlarm.rule.ruleName }}</p>
          <p><strong>{{ $t('topology.serviceName') }}:</strong>{{ currentEdgeAlarm.rule.serviceName }}</p>
          <p><strong>{{ $t('topology.alarmLevel') }}:</strong>
            <span :style="{ color: currentEdgeAlarm.rule.warningLevel === 1 ? '#ff4d4f' : '#faad14' }">
              {{ currentEdgeAlarm.rule.warningLevel === 1 ? $t('topology.urgent') : $t('topology.normal') }}
            </span>
          </p>
          <p><strong>{{ $t('topology.ruleDetails') }}:</strong></p>
          <div class="rule-details">
            <template v-if="currentEdgeAlarm.rule.ruleDetails">
              <span v-for="(rule, index) in JSON.parse(currentEdgeAlarm.rule.ruleDetails)" :key="index">
                {{ rule.indicatorName }} {{ rule.operator }} {{ rule.value }} {{ rule.logicalOperator }}
              </span>
            </template>
          </div>
        </div>
        <h3>{{ $t('topology.currentEdgeMetrics') }} ({{ currentEdgeAlarm.metrics.source }} => {{
          currentEdgeAlarm.metrics.target
        }})</h3>
        <div class="metrics-info">
          <p><strong>{{ $t('topology.callCount') }}:</strong>{{ currentEdgeAlarm.metrics.cnt }} <span
              v-if="currentEdgeAlarm.metrics.influencePercentage_cnt" class="influence-percentage">({{
                $t('topology.influencePercent') }}: {{ formatTinyNumber(2, true,
                currentEdgeAlarm.metrics.influencePercentage_cnt * 100) }})</span></p>
          <p><strong>{{ $t('topology.avgResponseTime') }}:</strong>{{ formatTinyNumber(2, false,
            currentEdgeAlarm.metrics.avgDuration / 1000) }}ms <span
              v-if="currentEdgeAlarm.metrics.influencePercentage_avgDuration" class="influence-percentage">({{
                $t('topology.influencePercent') }}: {{ formatTinyNumber(2, true,
                currentEdgeAlarm.metrics.influencePercentage_avgDuration * 100) }})</span></p>
          <p><strong>{{ $t('topology.maxResponseTime') }}:</strong>{{ formatTinyNumber(2, false,
            currentEdgeAlarm.metrics.maxDuration / 1000) }}ms <span
              v-if="currentEdgeAlarm.metrics.influencePercentage_maxDuration" class="influence-percentage">({{
                $t('topology.influencePercent') }}: {{ formatTinyNumber(2, true,
                currentEdgeAlarm.metrics.influencePercentage_maxDuration * 100) }})</span></p>
          <p><strong>{{ $t('topology.minResponseTime') }}:</strong>{{ formatTinyNumber(2, false,
            currentEdgeAlarm.metrics.minDuration / 1000) }}ms <span
              v-if="currentEdgeAlarm.metrics.influencePercentage_minDuration" class="influence-percentage">({{
                $t('topology.influencePercent') }}: {{ formatTinyNumber(2, true,
                currentEdgeAlarm.metrics.influencePercentage_minDuration * 100) }})</span></p>
          <p><strong>{{ $t('topology.errorCount') }}:</strong>{{ currentEdgeAlarm.metrics.errorNum }} <span
              v-if="currentEdgeAlarm.metrics.influencePercentage_errorNum" class="influence-percentage">({{
                $t('topology.influencePercent') }}: {{ formatTinyNumber(2, true,
                currentEdgeAlarm.metrics.influencePercentage_errorNum * 100) }})</span></p>
          <p><strong>{{ $t('topology.p90ResponseTime') }}:</strong>{{ formatTinyNumber(2, false,
            currentEdgeAlarm.metrics.p90Duration / 1000) }}ms <span
              v-if="currentEdgeAlarm.metrics.influencePercentage_p90Duration" class="influence-percentage">({{
                $t('topology.influencePercent') }}: {{ formatTinyNumber(2, true,
                currentEdgeAlarm.metrics.influencePercentage_p90Duration * 100) }})</span></p>
          <p><strong>{{ $t('topology.p95ResponseTime') }}:</strong>{{ formatTinyNumber(2, false,
            currentEdgeAlarm.metrics.p95Duration / 1000) }}ms <span
              v-if="currentEdgeAlarm.metrics.influencePercentage_p95Duration" class="influence-percentage">({{
                $t('topology.influencePercent') }}: {{ formatTinyNumber(2, true,
                currentEdgeAlarm.metrics.influencePercentage_p95Duration * 100) }})</span></p>
          <p><strong>{{ $t('topology.p99ResponseTime') }}:</strong>{{ formatTinyNumber(2, false,
            currentEdgeAlarm.metrics.p99Duration / 1000) }}ms <span
              v-if="currentEdgeAlarm.metrics.influencePercentage_p99Duration" class="influence-percentage">({{
                $t('topology.influencePercent') }}: {{ formatTinyNumber(2, true,
                currentEdgeAlarm.metrics.influencePercentage_p99Duration * 100) }})</span></p>
        </div>
      </div>
      <div slot="footer"></div>
    </Modal>
  </div>
</template>

<script>
import G6 from '@antv/g6';
import {getTopologyData} from '@/api/topology/req';
import TopologyWarningRules from "./topology-warning-rules.vue";
import {ToolBar} from '@antv/g6-plugin';
import {formatTinyNumber} from "@/utils/number-utils";
import {isEmptyResponse} from "@/utils/response-utils";
import {mapState} from "pinia";
import {useAppStore} from "@/store/store";
import M1 from '@/assets/topology/M1.svg';
import DB1 from '@/assets/topology/DB1.png';
import Redis from '@/assets/topology/redis.png';
import MQ from '@/assets/topology/MQ.png';

const imageMap = {
  'Service': M1,
  'JDBC': DB1,
  'Redis': Redis,
  'MQ': MQ
};

export default {
  name: 'AntVG6',
  components: { TopologyWarningRules },
  data() {
    return {
      currentLayout: {},

      // var about focus
      wannaToFocusNode: '',
      highlightTimer: null,
      highlightedNodeId: null,
      focusToOptions: [],

      isHighlightRulesModalVisible: false,
      showEdgeAlarmModal: false,
      currentEdgeAlarm: null,
      loading: false,
      loadingProgress: 0,
      progressTimers: [],
    };
  },
  computed: {
    ...mapState(useAppStore, {
      freshTime: 'freshTime',
      timezone: 'timezone',
      sidebarCollapsed: 'sidebarCollapsed',
    }),
    // calculate the offset of the progress circle
    progressCircleOffset() {
      const circumference = 2 * Math.PI * 26;
      return circumference - (this.loadingProgress / 100) * circumference;
    },
    changeBtnStyle() {
      const sidebarW = this.sidebarCollapsed ? 80 : 200;
      return { left: (sidebarW + 100) + 'px' };
    },
    searchBoxStyle() {
      const sidebarW = this.sidebarCollapsed ? 80 : 200;
      return { left: `calc(50% + ${sidebarW / 2}px)` };
    }
  },
  watch: {
    freshTime: 'refreshOnTimeContextChange',
    timezone: 'refreshOnTimeContextChange',
    sidebarCollapsed() {
      this.$nextTick(() => {
        if (this.graph) {
          this.graph.fitView(20);
        }
      });
    }
  },
  created() {
    // default label style configuration
    this.defaultLabelStyleCfg = {
      style: {
        fill: '#000',
        fontSize: 18,
        lineWidth: 3,
        stroke: ""
      },
    };
    // default edge style
    this.defaultEdgeStyle = {
      stroke: 'rgba(103,159,207,0.7)',
      endArrow: {
        path: G6.Arrow.vee(8, 16, 0)
      },
      lineAppendWidth: 5,
      lineWidth: 1,
      lineDash: [4, 3],
      opacity: 1,
    }

    this.focusLabelStyle = {
      fill: "pink",
      lineWidth: 5,
      stroke: "black"
    };
    // graph layout configuration
    this.graphLayoutDefaultCfg = [
      {
        name: 'dagre',
        edgesType: 'cubic-vertical',
        layoutConfig: {
          type: 'dagre',
          controlPoints: true,
        }
      },
      {
        name: 'grid',
        edgesType: 'line',
        layoutConfig: {
          type: 'grid',
          preventOverlap: true,
          preventOverlapPadding: 15,
          nodeSize: 50,
        }
      },
      {
        name: 'force2',
        edgesType: 'line',
        layoutConfig: {
          type: 'force2',
          preventOverlap: true,
          linkDistance: 500,
          nodeSpacing: 500,
          nodeSize: 200,
          preset: 'random',
          animate: true,
          minMovement: 0.1,
          maxIteration: 3000
        }
      },

    ];
    // graph data
    this.graphData = {};
    // graph events
    this.graphEvents = [
      {
        name: 'node:mouseenter, node:mouseleave',
        event: e => {
          let inEdges = e.item.getInEdges();
          let outEdges = e.item.getOutEdges();
          this.changeNodeEdgesStyle(e.type, e.item, inEdges, outEdges)
        }
      },
      {
        name: 'edge:click',
        event: e => {
          this.handleEdgeClick(e.item);
        }
      }
    ];

    this.edgeStates = {
      inLocked : 'lockInEdges',
      outLocked : 'lockOutEdges',
      inHighlight : 'inEdgesHighLight',
      outHighlight : 'outEdgesHighLight'
    };
    // high light in edges style
    this.highLightInEdgesStyle = {
      stroke: '#8c8cff',
      lineWidth: 3,
      opacity: 1,
    };
    // high light out edges style
    this.highLightOutEdgesStyle = {
      stroke: '#8ad247',
      lineWidth: 3,
      opacity: 1,
    };
    this.edgeStateStyleMapping = {
      inEdgesHighLight: this.highLightInEdgesStyle,
      outEdgesHighLight: this.highLightOutEdgesStyle,
      lockInEdges: this.highLightInEdgesStyle,
      lockOutEdges: this.highLightOutEdgesStyle,
    };
  },
  mounted() {
    this.currentLayout = this.graphLayoutDefaultCfg[0];
    this.requestTopologyData(response => {
      this.processResponseGraphData(response.data);
      this.initGraph();
      this.graph.read(this.graphData);
      this.loadFocusToOptions();
      this.graph.zoom(0.8, { x: 100, y: 100 });
    });

  },
  methods: {
    //Event Handler part==================================
    formatTinyNumber,
    refreshOnTimeContextChange() {
      if (!this.graph) {
        return;
      }
      this.requestTopologyData(response => {
        this.unlockEdges(this.graph.getEdges());
        this.graph.getNodes().forEach(node => node.show());
        this.graph.getEdges().forEach(edge => edge.show());
        this.processResponseGraphData(response.data);
        this.graph.changeData(this.graphData, false);
        this.loadFocusToOptions();
      });
    },
    //load focus options
    loadFocusToOptions() {
      this.focusToOptions = this.graph.getNodes()
        .filter(item => item.isVisible())
        .map(item => {
          return {
            text: item.getID(),
            value: item.getID()
          }
        });
    },

    focusTo(targetNode) {
      if (this.wannaToFocusNode === undefined && targetNode === null) {
        return;
      }
      let realTarget = targetNode ? targetNode : this.wannaToFocusNode;

      if (this.highlightedNodeId) {
        if (this.highlightTimer) {
          clearTimeout(this.highlightTimer);
          this.highlightTimer = null;
        }

        const prevNode = this.graph.findById(this.highlightedNodeId);
        if (prevNode && !prevNode.destroyed) {
          this.graph.updateItem(prevNode, {
            labelCfg: this.defaultLabelStyleCfg
          });
        }

        this.highlightedNodeId = null;
      }

      let node = this.graph.findById(realTarget);
      if (!node) return;

      this.highlightedNodeId = node.getID();

      this.graph.updateItem(node, {
        labelCfg: {
          style: this.focusLabelStyle
        }
      });

      this.graph.focusItem(node, true, {
        duration: 450,
        easing: 'easeCubic',
        callback: () => {
          const width = this.graph.getWidth();
          const height = this.graph.getHeight();
          this.graph.zoomTo(Math.max(this.graph.getZoom(), 0.8), {x: width / 2, y: height / 2}, true, {
            duration: 500,
            easing: 'easeCubic',
          });
        }
      });

      this.highlightTimer = setTimeout(() => {
        const targetNode = this.graph.findById(realTarget);
        if (targetNode && !targetNode.destroyed) {
          this.graph.updateItem(targetNode, {
            labelCfg: this.defaultLabelStyleCfg,
          });
        }
        this.highlightedNodeId = null;
        this.highlightTimer = null;
      }, 3000);
    },

    changeLayout() {
      let current_layout = this.graph.cfg.layout.type;
      for (let i = 0; i < this.graphLayoutDefaultCfg.length; i++) {
        let next_index = i + 1;
        if (next_index === this.graphLayoutDefaultCfg.length) {
          next_index = 0;
        }
        if (current_layout === this.graphLayoutDefaultCfg[i].name) {
          G6.Util.processParallelEdges(this.graphData.edges, 15, this.graphLayoutDefaultCfg[next_index].edgesType, this.graphLayoutDefaultCfg[next_index].edgesType, 'loop');
          this.currentLayout = this.graphLayoutDefaultCfg[next_index];
          this.graph.changeData(this.graphData)
          this.graph.updateLayout(this.currentLayout.layoutConfig);
          break;
        }
      }
    },

    reLayout() {
      this.graph.layout();
    },

    //Event Handler End==================================

    //Graph part====================
    initGraph() {
      // init toolbar
      const toolbar = new ToolBar({
        container: 'toolbar-container',
        getContent: () => {
          // noinspection HtmlUnknownAttribute
          return `
            <ul class="g6-toolbar-ul">
              <li code="zoom-in" title="Zoom In">
                <svg class="icon" viewBox="0 0 1024 1024" width="24" height="24">
                  <circle cx="384" cy="384" r="200" fill="none" stroke="#000000" stroke-width="32"/>
                  <path d="M544 544L768 768" stroke="#000000" stroke-width="32" stroke-linecap="round"/>
                  <path d="M384 288v192M288 384h192" stroke="#000000" stroke-width="32" stroke-linecap="round"/>
                </svg>
              </li>
              <li code="zoom-out" title="Zoom Out">
                <svg class="icon" viewBox="0 0 1024 1024" width="24" height="24">
                  <circle cx="384" cy="384" r="200" fill="none" stroke="#000000" stroke-width="32"/>
                  <path d="M544 544L768 768" stroke="#000000" stroke-width="32" stroke-linecap="round"/>
                  <path d="M288 384h192" stroke="#000000" stroke-width="32" stroke-linecap="round"/>
                </svg>
              </li>
              <li code="fit-view" title="Fit View">
                <svg class="icon" viewBox="0 0 1024 1024" width="24" height="24">
                  <path d="M896 128H128v768h768V128z" fill="none" stroke="#000000" stroke-width="64"></path>
                  <path d="M896 416L768 288h128v128zM128 416L256 288H128v128zM896 608l-128 128h128V608zM128 608l128 128H128V608z" fill="#000000"></path>
                  <path d="M320 320h384v384H320z" fill="none" stroke="#000000" stroke-width="32"></path>
                  <path d="M320 512h384M512 320v384" stroke="#000000" stroke-width="32"></path>
                </svg>
              </li>
            </ul>
          `;
        },
        handleClick: (code, graph) => {
          if (code === 'zoom-in') {
            const current = graph.getZoom();
            graph.zoomTo(current * 1.1);
          } else if (code === 'zoom-out') {
            const current = graph.getZoom();
            graph.zoomTo(current * 0.9);
          } else if (code === 'fit-view') {
            graph.fitView(20);
          }
        },
      });
      // init graph
      this.graph = new G6.Graph({
        container: 'mountNode',
        animate: true,
        fitView: true,
        fitViewPadding: 20,
        //initial layout
        layout: this.graphLayoutDefaultCfg[0].layoutConfig,
        //node configuration
        // defaultNode: this.defaultNodeCfg,
        //edge configuration
        defaultEdge: {
          type: 'line',
          style: this.defaultEdgeStyle,
          labelCfg: this.defaultLabelStyleCfg
        },
        edgeStateStyles: this.edgeStateStyleMapping,
        //operation mode
        modes: {
          default: ['drag-canvas', 'zoom-canvas', 'drag-node'],
        },
        plugins: [toolbar],
      });
      //Add node menu
      this.graph.addPlugin(new G6.Menu({
        getContent: () => {
          return `
          <ul style="list-style-type: none;">
            <li><strong>== ${this.$t('topology.menu.lockUnlock')} ==</strong></li>
            <li data-action="lockEdges" style="cursor: pointer;" onmouseover="this.style.backgroundColor='#f0f0f0'" onmouseout="this.style.backgroundColor=''">${this.$t('topology.menu.lockRelatedEdges')}</li>
            <li data-action="unlockEdges" style="cursor: pointer;" onmouseover="this.style.backgroundColor='#f0f0f0'" onmouseout="this.style.backgroundColor=''">${this.$t('topology.menu.unlockRelatedEdges')}</li>
            <li data-action="hideOthers" style="cursor: pointer;" onmouseover="this.style.backgroundColor='#f0f0f0'" onmouseout="this.style.backgroundColor=''">${this.$t('topology.menu.hideOtherNodesEdges')}</li>
            <li><strong>== ${this.$t('topology.menu.hideShow')} ==</strong></li>
            <li data-action="hideEdges" style="cursor: pointer;" onmouseover="this.style.backgroundColor='#f0f0f0'" onmouseout="this.style.backgroundColor=''">${this.$t('topology.menu.hideRelatedEdges')}</li>
            <li data-action="showEdges" style="cursor: pointer;" onmouseover="this.style.backgroundColor='#f0f0f0'" onmouseout="this.style.backgroundColor=''">${this.$t('topology.menu.showRelatedEdges')}</li>
            <li data-action="hideNodeEdges" style="cursor: pointer;" onmouseover="this.style.backgroundColor='#f0f0f0'" onmouseout="this.style.backgroundColor=''">${this.$t('topology.menu.hideCurrentNodeEdges')}</li>
            <li data-action="showNodesEdges" style="cursor: pointer;" onmouseover="this.style.backgroundColor='#f0f0f0'" onmouseout="this.style.backgroundColor=''">${this.$t('topology.menu.showRelatedNodesEdges')}</li>
            <li><strong>== ${this.$t('topology.menu.globalOperation')} ==</strong></li>
            <li data-action="restoreAll" style="cursor: pointer;" onmouseover="this.style.backgroundColor='#f0f0f0'" onmouseout="this.style.backgroundColor=''">${this.$t('topology.menu.restoreAllNodesEdges')}</li>
          </ul>`
        },
        offsetX: 10,
        offsetY: -25,
        itemTypes: ['node'],
        handleMenuClick: (target, currentItem) => {
          const action = target.dataset.action;
          switch (action) {
            case 'lockEdges':
              this.lockEdges(currentItem.getInEdges(), currentItem.getOutEdges());
              break;
            case 'unlockEdges':
              this.unlockEdges(currentItem.getEdges());
              break;
            case 'hideOthers':
              this.lockEdges(currentItem.getInEdges(), currentItem.getOutEdges());
              let locked_edges = this.graph.getEdges()
                  .filter(e => e.isVisible())
                  .filter(e => this.isEdgeLocked(e));
              let lockNodes = locked_edges
                  .reduce((accumulator, e) => {
                    // merge B and C arrays
                    const B = e.getSource();
                    const C = e.getTarget();
                    return accumulator.concat(B, C);  // merge to accumulator
                  }, []);
              this.graph.getNodes()
                  .filter(node => !lockNodes.includes(node))
                  .forEach(node => {
                    node.hide();
                  });
              this.graph.getEdges()
                  .filter(e => !locked_edges.includes(e))
                  .forEach(edge => {
                    edge.hide();
                  });
              this.loadFocusToOptions();
              this.reLayout();
              this.unlockEdges(this.graph.getEdges());
              setTimeout(() => {
                this.$refs.nodeSelect.clearSingleSelect();
                let focus_target = currentItem.getID();
                this.focusTo(focus_target);
              }, 500);
              break;
            case 'restoreAll':
              this.unlockEdges(this.graph.getEdges());
              this.graph.getNodes().forEach(node => {
                node.show();
              });
              this.graph.getEdges().forEach(edge => {
                edge.show();
              });
              this.reLayout();
              this.loadFocusToOptions();
              this.$refs.nodeSelect.clearSingleSelect();
              this.graph.fitView(20, null, true);
              break;
            case 'hideEdges':
              currentItem.getEdges().forEach(edge => {
                edge.hide();
              });
              break;
            case 'showEdges':
              currentItem.getEdges().forEach(item => {
                // only restore edges where both source and target nodes are visible
                const source = item.getSource();
                const target = item.getTarget();
                if (source.isVisible() && target.isVisible()) {
                  item.show();
                }
              })
              break;
            case 'hideNodeEdges':
              // get all edges of the current node
              const currentEdges = currentItem.getEdges();
              // get all related nodes
              const relatedNodes = new Set();
              // record the connection count of each related node
              const nodeConnectionCount = new Map();

              //calculate the connection count of each related node
              currentEdges.forEach(edge => {
                const source = edge.getSource();
                const target = edge.getTarget();
                const otherNode = source === currentItem ? target : source;

                relatedNodes.add(otherNode);

                // initialize or increase node connection count
                if (!nodeConnectionCount.has(otherNode)) {
                  nodeConnectionCount.set(otherNode, 1);
                } else {
                  nodeConnectionCount.set(otherNode, nodeConnectionCount.get(otherNode) + 1);
                }

                // hide edge
                edge.hide();
              });

              // hide current node
              currentItem.hide();

              // check if each related node is only connected to the current node
              relatedNodes.forEach(node => {
                // get all edges of the node
                const nodeEdges = node.getEdges();
                // calculate the number of visible edges
                const visibleEdgesCount = nodeEdges.filter(edge => edge.isVisible()).length;

                // if the node has no other visible edges connected, hide it
                if (visibleEdgesCount === 0) {
                  node.hide();
                }
              });
              break;
            case 'showNodesEdges':
              currentItem.getEdges().forEach(item => {
                // only restore edges where both source and target nodes are visible
                const source = item.getSource();
                const target = item.getTarget();
                source.show();
                target.show();
                item.show();
                this.reLayout();
              });
              break;
          }
        }
      }));
      //Add edge menu
      this.graph.addPlugin(new G6.Menu({
        getContent: () => {
          return `
          <ul style="list-style-type: none;">
            <li data-action="hideEdge" style="cursor: pointer;" onmouseover="this.style.backgroundColor='#f0f0f0'" onmouseout="this.style.backgroundColor=''">${this.$t('topology.menu.hideEdge')}</li>
          </ul>`
        },
        offsetX: 10,
        offsetY: -25,
        itemTypes: ['edge'],
        handleMenuClick: (target, currentItem) => {
          const action = target.dataset.action;
          if (action === 'hideEdge') {
            currentItem.hide();
          }
        }
      }));

      this.bindEvent(this.graphEvents);
      console.debug("================ G6.Graph has been loaded ===================")
    },

    isEdgeLocked(edge) {
      return edge.hasState(this.edgeStates.inLocked) || edge.hasState(this.edgeStates.outLocked);
    },
    // high light edges
    highLightEdges(inEdges, outEdges) {
      inEdges.forEach(edge => {
        if (!this.isEdgeLocked(edge)) {
          this.graph.setItemState(edge, this.edgeStates.inHighlight, true);
        }
      });
      outEdges.forEach(edge => {
        if (!this.isEdgeLocked(edge)) {
          this.graph.setItemState(edge, this.edgeStates.outHighlight, true);
        }
      })
    },
    removeHighlightEdges(edges) {
      edges.forEach(item => {
          this.graph.clearItemStates(item, [this.edgeStates.inHighlight, this.edgeStates.outHighlight]);
      });
    },
    lockEdges(inEdges, outEdges) {
      inEdges.forEach(edge => {
        if (!this.isEdgeLocked(edge)) {
          this.graph.setItemState(edge, this.edgeStates.inLocked, true);
        }
      });
      outEdges.forEach(edge => {
        if (!this.isEdgeLocked(edge)) {
          this.graph.setItemState(edge, this.edgeStates.outLocked, true);
        }
      })
    },
    // cancel high light edges
    unlockEdges(edges) {
      edges.forEach(item => {
          this.graph.clearItemStates(item, [this.edgeStates.inLocked, this.edgeStates.outLocked]);
      })
    },
    // change node edges style while mouse enter or leave
    changeNodeEdgesStyle(evt, node, inEdges, outEdges) {
      let remove_style = evt === 'mouseleave';
      if (remove_style) {
        this.removeHighlightEdges(node.getEdges());
      } else {
        this.highLightEdges(inEdges, outEdges);
      }
    },

    handleEdgeClick(edge) {
      // get edge data
      const edgeData = edge.getModel();

      // find the corresponding alarm information
      if (this.graphData.highlights) {
        for (const highlight of this.graphData.highlights) {
          if (highlight.highlightEdges) {
            const matchedEdge = highlight.highlightEdges.find(e =>
                e.source === edgeData.source && e.target === edgeData.target
            );

            if (matchedEdge && matchedEdge.influencePercentage) {
              // set current edge alarm information
              this.currentEdgeAlarm = {
                rule: highlight.rule,
                metrics: matchedEdge
              };
              // show alarm details modal
              this.showEdgeAlarmModal = true;
              return;
            }
          }
        }
      }
    },
    //Graph part end ====================

    //Data request part==================================
    async requestTopologyData(sucHandler) {
      this.loading = true;
      //Start progress bar animation
      this.startFakeProgress();

      let param = {
        "startDate": this.freshTime[0],
        "endDate": this.freshTime[1]
      };
      await getTopologyData(param).then(res => {
        this.graphData = res.data;
        if (isEmptyResponse(res)) {
          return;
        }
        sucHandler(res)
      }).finally(() => {
        this.finishProgress();
      });
    },

    processResponseGraphData(responseGraphData) {
      // process node image
      this.graphData['nodes'].forEach(item => {
        if (imageMap[item.nodeComponent]) {
          item.img = imageMap[item.nodeComponent];
        }
      });

      // process high light rules
      if (responseGraphData.highlights) {
        responseGraphData.highlights.forEach(highlight => {
          let warningLevel = highlight.rule.warningLevel;
          // process node high light
          if (highlight.highlightNodes && highlight.highlightNodes.length > 0) {
            const node = this.graphData.nodes.find(n => n.id === highlight.highlightNodes[0].id);
            if (node) {
              // check if the node already has an emergency alarm style (red)
              const hasEmergencyStyle = node.labelCfg && node.labelCfg.style && node.labelCfg.style.fill === '#ff4d4f';

              // only apply the new style if there is no emergency alarm style or the current alarm level is emergency
              if (!hasEmergencyStyle || warningLevel === 1) {
                // set different styles based on alarm level
                // normal alarm yellow
                node.labelCfg.style = warningLevel === 1 ?
                    { fill: '#ff4d4f', stroke: 'black', lineWidth: 4 } : // emergency alarm red
                    { fill: '#faad14', stroke: 'black', lineWidth: 3 };
              }
            }
          }
          // process edge high light
          if (highlight.highlightEdges && highlight.highlightEdges.length > 0) {
            highlight.highlightEdges.forEach(edgeInfo => {
              const edge = this.graphData.edges.find(e =>
                  e.source === edgeInfo.source && e.target === edgeInfo.target
              );
              if (edge) {
                // check if the edge already has an emergency alarm style (red)
                const hasEmergencyStyle = edge.style && edge.style.stroke === '#ff4d4f';

                // only apply the new style if there is no emergency alarm style or the current alarm level is emergency
                if (!hasEmergencyStyle || warningLevel === 1) {
                  // get influence percentage
                  const influencePercentage = edgeInfo.influencePercentage || 0;
                  // determine the color depth and line width based on the percentage
                  let opacity;
                  let lineWidth;
                  if (influencePercentage <= 0) {
                    return;
                  } else if (influencePercentage < 0.34) {
                    opacity = 0.3;
                    lineWidth = warningLevel === 1 ? 3 : 3;
                  } else if (influencePercentage < 0.67) {
                    opacity = 0.7;
                    lineWidth = warningLevel === 1 ? 3 : 3;
                  } else if (influencePercentage >= 0.67) {
                    opacity = 1;
                    lineWidth = warningLevel === 1 ? 5 : 5;
                  }

                  // set different styles based on alarm level
                  const baseColor = warningLevel === 1 ? '#ff4d4f' : '#faad14';
                  const style = {
                    stroke: baseColor,
                    lineWidth: lineWidth,
                    opacity: opacity,
                    lineAppendWidth: 20 // extend the click recognition area width (unit: pixels)
                  };

                  edge.style = { ...this.defaultEdgeStyle, ...style };
                }
              }
            });
          }
        });
      }

      // Implement the parallel edges handler when upgrading G6 to version 5.x
      // https://g6.antv.antgroup.com/manual/transform/custom-transform
      G6.Util.processParallelEdges(this.graphData.edges, 15, this.currentLayout.edgesType, this.currentLayout.edgesType, 'loop');
    },
    //Data request End=========================================

    //Util part================
    bindEvent(arr) {
      for (let item of arr) {
        if (item.name.includes(',')) {
          let names = item.name.split(',');
          for (let name of names) {
            this.graph.on(name.trim(), item.event);
          }
        } else {
          this.graph.on(item.name, item.event);
        }
      }
    },

    toggleHighlightRulesModal() {
      this.$refs.warningRules.fetchRules();
      this.isHighlightRulesModalVisible = !this.isHighlightRulesModalVisible;
    },

    startFakeProgress() {
      // Clear previous timers
      this.clearProgressTimers();

      // Immediately reach 80%
      this.loadingProgress = 0;

      setTimeout(() => {
        this.loadingProgress = 80;
      }, 100);

      const timer1 = setTimeout(() => {
        this.loadingProgress = 90;
      }, 1000);
      this.progressTimers.push(timer1);

      const timer2 = setTimeout(() => {
        this.loadingProgress = 99;
      }, 3000);
      this.progressTimers.push(timer2);
    },

    finishProgress() {
      this.clearProgressTimers();
      this.loadingProgress = 100;

      setTimeout(() => {
        this.loading = false;
        this.loadingProgress = 0;
      }, 300);
    },

    clearProgressTimers() {
      this.progressTimers.forEach(timer => clearTimeout(timer));
      this.progressTimers = [];
    },
  },
};
</script>

<style lang="less" scoped>
.topology-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 100vh;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.95) 0%, rgba(241, 245, 249, 0.95) 100%);
  backdrop-filter: blur(8px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.loading-container {
  text-align: center;
  padding: 40px 60px;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.9) 0%, rgba(255, 255, 255, 0.8) 100%);
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  box-shadow:
    0 25px 50px -12px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

.loading-icon {
  position: relative;
  width: 80px;
  height: 80px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.progress-ring {
  width: 80px;
  height: 80px;
  transform: rotate(-90deg);
}

.progress-ring-bg {
  fill: none;
  stroke: rgba(203, 213, 225, 0.5);
  stroke-width: 4;
}

.progress-ring-circle {
  fill: none;
  stroke: #3b82f6;
  stroke-width: 4;
  stroke-linecap: round;
  stroke-dasharray: 163.36; // 2 * PI * 26
  transition: stroke-dashoffset 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  filter: drop-shadow(0 0 8px rgba(59, 130, 246, 0.5));
}

.progress-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 16px;
  font-weight: 600;
  color: #334155;
  font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
}

.loading-bar-container {
  width: 280px;
}

.loading-bar {
  position: relative;
  height: 8px;
  background: rgba(203, 213, 225, 0.4);
  border-radius: 10px;
  overflow: hidden;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.08);
}

.loading-bar-fill {
  height: 100%;
  background: linear-gradient(90deg,
      #3b82f6 0%,
      #8b5cf6 35%,
      #06b6d4 65%,
      #3b82f6 100%);
  background-size: 200% 100%;
  border-radius: 10px;
  transition: width 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  animation: shimmer 2s linear infinite;
  box-shadow:
    0 0 20px rgba(59, 130, 246, 0.5),
    0 0 40px rgba(139, 92, 246, 0.3);
}

.loading-bar-glow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 100%;
  background: linear-gradient(90deg,
      transparent 0%,
      rgba(255, 255, 255, 0.4) 50%,
      transparent 100%);
  animation: glow-slide 1.5s ease-in-out infinite;
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }

  100% {
    background-position: -200% 0;
  }
}

@keyframes glow-slide {
  0% {
    transform: translateX(-100%);
  }

  100% {
    transform: translateX(100%);
  }
}



.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter,
.fade-leave-to {
  opacity: 0;
}

.changeBtn {
  position: fixed;
  top: 13px;
  z-index: 9;
  background-color: white;
  white-space: nowrap;
  font-size: 13px;

  /deep/ .ivu-btn-small {
    font-size: 12px;
    padding: 0 8px;
    height: 24px;
  }
}

.searchBox {
  position: fixed;
  top: 8px;
  transform: translateX(-50%);
  z-index: 8;
  width: 30%;
  min-width: 120px;
  max-width: 400px;
  background-color: transparent;

  > span {
    display: block;

    .ivu-select {
      width: 100%;
    }

    .ivu-select-selection {
      font-size: 12px;
    }
  }
}

// Progressive narrowing, hide at very narrow widths
@media screen and (max-width: 1400px) {
  .searchBox {
    max-width: 250px;
  }
}

@media screen and (max-width: 1000px) {
  .searchBox {
    display: none;
  }
}

.toolbar-container {
  position: fixed;
  top: 60px;
  right: 60px;
  z-index: 1000;
  background-color: rgba(255, 255, 255, 0.8);
  border-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
  padding: 8px;
}

.g6-toolbar-ul {
  list-style-type: none;
  padding: 0;
  margin: 0;
}

.g6-toolbar-ul li {
  width: 24px;
  height: 24px;
  margin: 8px 0;
  cursor: pointer;
  transition: all 0.3s;
}

.g6-toolbar-ul li:hover {
  transform: scale(1.2);
}

.metrics-info {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  padding: 16px;
  background-color: #fafafa;
  border-radius: 8px;
  margin-top: 12px;

  p {
    margin: 0;
    padding: 12px;
    background: #fff;
    border-radius: 6px;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    }

    strong {
      color: #1f1f1f;
      font-size: 14px;
      margin-right: 8px;
    }

    .influence-percentage {
      display: inline-block;
      margin-left: 8px;
      padding: 2px 6px;
      font-size: 12px;
      color: #5b8c00;
      background-color: #f6ffed;
      border: 1px solid #b7eb8f;
      border-radius: 4px;
      cursor: help;
    }
  }
}

.alarm-rule-info {
  background-color: #fafafa;
  border-radius: 8px;
  padding: 16px;
  margin-top: 12px;

  p {
    margin: 8px 0;
    line-height: 1.6;
  }

  .rule-details {
    background: #fff;
    border-radius: 6px;
    padding: 12px;
    margin-top: 8px;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);

    div {
      padding: 4px 0;
      color: #666;
    }
  }
}

h3 {
  color: #1f1f1f;
  font-size: 16px;
  margin: 24px 0 8px;
  padding-left: 12px;
  border-left: 4px solid #1890ff;
}
.echartsMap {
  width: 100%;
  height: calc(100vh - 50px);
  padding-bottom: 0;
  border: 1px solid #ccc;
}
</style>