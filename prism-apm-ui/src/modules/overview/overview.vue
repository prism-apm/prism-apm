<template>
  <div class="service-card-list">
    <Spin size="large" :fix="true" v-if="loading"></Spin>

    <Row :gutter="24" v-else>
      <Col :xs="24" :sm="12" :md="8" :lg="6" :xl="4" v-for="(item, index) in list" :key="index" class="card-col">

      <Card class="service-card" :bordered="false" @click.native="enter(item)">

        <div class="status-bar" :style="{ background: '#20b5ad' }"></div>

        <div class="card-content">
          <div class="card-header">
            <Avatar shape="square" icon="ios-apps" size="large"
              :style="{ backgroundColor: stringToColor(item.name), verticalAlign: 'middle' }" class="service-icon">
              {{ item.name.substring(0, 1).toUpperCase() }}
            </Avatar>

            <Icon type="md-open" class="action-icon" />
          </div>

          <div class="service-info">
            <div class="main-name" :title="item.name">
              {{ item.name }}
            </div>

            <div class="sub-name" :title="item.display">
              {{ item.display }}
            </div>
          </div>

          <div class="service-desc">
            ID: {{ item.id }} | Created: {{ item.createdAt.substring(0, 10) }}
          </div>
        </div>
      </Card>
      </Col>
    </Row>
  </div>
</template>

<script>
import { reqServiceObjectList } from "@/api/common/req";
import {useAppStore} from "@/store/store";

export default {
  data() {
    return {
      list: [],
      loading: false
    }
  },
  components: {},
  mounted() {
    this.getServiceList()
  },
  methods: {
    async getServiceList() {
      try {
        this.loading = true;
        let res = await reqServiceObjectList({})
        if (res.status === 200) {
          this.list = res.data
          this.loading = false
        }
      } catch (res) {
        console.error(res)
        this.$Message.error(this.$t('overview.fetchFailed'))
        this.loading = false;
      }
    },
    enter(item) {
      useAppStore().app = 'server-detail';
      useAppStore().serviceName = item.name;
      this.$router.push({ path: '/overview/transaction' })
    },

    stringToColor(str) {
      let hash = 0;
      for (let i = 0; i < str.length; i++) {
        hash = str.charCodeAt(i) + ((hash << 5) - hash);
      }
      const c = (hash & 0x00ffffff).toString(16).toUpperCase();
      return '#' + '00000'.substring(0, 6 - c.length) + c;
    }
  }
}
</script>

<style lang="less" scoped>
.service-card-list {
  padding: 20px;
  background: #f5f7f9;

  .card-col {
    margin-bottom: 20px;
  }

  .service-card {
    cursor: pointer;
    transition: all 0.3s ease;
    border-radius: 8px;
    overflow: hidden;
    position: relative;
    height: 100%;

    &:hover {
      transform: translateY(-5px);
      box-shadow: 0 10px 20px rgba(0, 0, 0, 0.08);

      .action-icon {
        color: #20b5ad;
        opacity: 1;
      }
    }

    .status-bar {
      height: 4px;
      width: 100%;
      position: absolute;
      top: 0;
      left: 0;
    }

    .card-content {
      padding: 16px 8px;
      display: flex;
      flex-direction: column;

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 15px;

        .service-icon {
          box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
          font-weight: bold;
          font-size: 20px;
        }

        .action-icon {
          font-size: 20px;
          color: #ccc;
          opacity: 0.5;
          transition: all 0.3s;
        }
      }

      .service-info {
        margin-bottom: 12px;
        width: 100%;

        .main-name {
          font-size: 16px;
          font-weight: 700;
          color: #333;
          line-height: 1.4;

          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .sub-name {
          font-size: 12px;
          color: #999;
          margin-top: 4px;

          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }

      .service-tags {
        margin-bottom: 15px;
        display: flex;
        gap: 5px;
      }

      .service-desc {
        font-size: 12px;
        color: #999;
        margin-top: auto;
        border-top: 1px dashed #eee;
        padding-top: 10px;
      }
    }
  }
}
</style>