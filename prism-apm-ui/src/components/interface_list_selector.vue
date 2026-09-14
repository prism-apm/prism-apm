<template>
  <div class="interface-list-selector">
    <h2 class="title sort-title">
      <slot name="header"></slot>
    </h2>
    <span class="app-list sort-list">
      <i :class="{ 'active': sortBy === 'totalCount' }" @click="handleSort('totalCount')">RPM</i>
      <i :class="{ 'active': sortBy === 'avgDuration' }" @click="handleSort('avgDuration')">AVG</i>
      <i :class="{ 'active': sortBy === 'totalError' }" @click="handleSort('totalError')">ERROR</i>
    </span>
    <Tooltip placement="top" max-width="200">
      <Icon type="ios-information-circle-outline" />
      <template v-slot:content>
        <p>{{ $t('transaction.percentTip') }}</p>
      </template>
    </Tooltip>
    <div class="list-wrapper">
      <ul class="transaction-list">
        <li v-for="item in data" :key="item[itemKey]"
            :class="{ 'active': item[itemKey] === selectedIFaceUniqueId }"
            @click="handleSelect(item)"
            @mouseenter="handleItemMouseEnter($event, item)">
          <div style="position: relative; height: 100%; width: 100%">
            <p class="left" :style="{ width: item.sortByPercent + '%' }"></p>
            <p class="transaction-title">
              <span class="method-head">{{ splitMethod(item.method).head }}</span><span
                class="method-tail">{{ splitMethod(item.method).tail }}</span>
              <i class="percent">{{ item.percentFormatted }}%</i>
            </p>
          </div>
        </li>
      </ul>
      <Spin size="large" :fix="true" v-if="loading"></Spin>
    </div>
  </div>
</template>

<script>

import {InterfaceSortedItem} from "@/entity/view/InterfaceSortedList";

// Tail-length tuning for the middle-ellipsis split.
// MAX_TAIL_LENGTH: if the natural separator-based tail is longer, fall back to
//   a fixed-length tail so it can't eat the whole row.
// NO_DOT_TAIL_LENGTH: fixed tail length used when there is no usable separator.
const MAX_TAIL_LENGTH = 40;
const NO_DOT_TAIL_LENGTH = 20;

export default {
  name: 'InterfaceListSelector',
  props: {
    value: {
      type: InterfaceSortedItem,
      default: () => new InterfaceSortedItem(),
    },
    data: {
      type: Array,
      default: () => []
    },
    sortBy: {
      type: String,
      default: 'totalCount'
    },
    loading: {
      type: Boolean,
      default: false
    },
    itemKey: {
      type: String,
      default: 'uniqueId'
    },
    // Optional: custom native title tooltip. When not provided, a built-in
    // tooltip shows the full method name on hover, but only when the text is
    // actually truncated (ellipsized).
    getItemTitle: {
      type: Function,
      default: null
    }
  },
  computed: {
    selectedIFaceUniqueId() {
      return this.value.uniqueId;
    }
  },
  methods: {
    handleSort(sortKey) {
      this.$emit('on-sort-change', sortKey);
    },
    handleSelect(item) {
      this.$emit('input', item);
      this.$emit('on-select', item);
    },
    // Set the row's native title on hover. Custom titles (e.g. db.vue) are
    // always delegated to the parent's function; the built-in title only shows
    // the full method name when the head is actually ellipsized. The title is
    // placed on the <li> so hovering anywhere on the row triggers it.
    handleItemMouseEnter(event, item) {
      const li = event.currentTarget;
      if (this.getItemTitle) {
        li.title = this.getItemTitle(item);
        return;
      }
      const headEl = li.querySelector('.method-head');
      li.title = (headEl && headEl.scrollWidth > headEl.clientWidth) ? item.method : '';
    },
    // Split a method name into head/tail so the CSS can render a middle
    // ellipsis: head ellipsizes when space is tight, tail stays visible.
    // Strategy (by priority):
    //   1. last '/'  — for URLs (keeps the last path segment as tail; IPs and
    //      host:port stay intact in head, never split mid-IP).
    //   2. last '.'  — for Java-like method signatures (tail = method name).
    //   3. fixed-length tail — dot-less content (e.g. SQL / topic).
    // If the natural tail is too long (would dominate the row), fall back to a
    // fixed-length tail too.
    splitMethod(method) {
      if (!method) {
        return {head: '', tail: ''};
      }
      const fixedTail = (len) => {
        if (method.length <= len) {
          return {head: '', tail: method};
        }
        return {
          head: method.slice(0, method.length - len),
          tail: method.slice(method.length - len)
        };
      };
      // Pick the best separator: '/' beats '.'; keep the tail intact.
      const slashIndex = method.lastIndexOf('/');
      const dotIndex = method.lastIndexOf('.');
      let sepIndex = -1;
      if (slashIndex >= 0) {
        sepIndex = slashIndex;
      } else if (dotIndex >= 0) {
        sepIndex = dotIndex;
      }
      if (sepIndex >= 0 && sepIndex < method.length - 1) {
        const tail = method.slice(sepIndex + 1);
        if (tail.length <= MAX_TAIL_LENGTH) {
          return {head: method.slice(0, sepIndex + 1), tail};
        }
      }
      return fixedTail(NO_DOT_TAIL_LENGTH);
    }
  }
};
</script>

<style lang="less" scoped>
.interface-list-selector {
  .list-wrapper {
    position: relative;
  }
}

.transaction-list {
  height: calc(100vh - 280px);
  overflow-y: auto;
  overflow-x: hidden;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-thumb {
    background: #ccc;
    border-radius: 4px;
  }

  &::-webkit-scrollbar-track {
    background: #f1f1f1;
  }

  li.active {
    border: 1px solid orange;

    .left {
      background: orange;
    }
  }

  li {
    position: relative;
    height: 30px;
    margin: 10px;
    list-style: none;
    cursor: pointer;

    .left {
      position: absolute;
      width: 20%;
      height: 100%;
      left: 0;
      background: rgba(167, 183, 218, 0.5);
      border-radius: 1px;
      z-index: 2;
      top: 0;
    }

    .transaction-title {
      position: absolute;
      padding-top: 6px;
      padding-left: 3px;
      width: 100%;
      left: 0;
      z-index: 3;
      top: 0;
      display: flex;
      white-space: nowrap;
      align-items: baseline;

      .method-head {
        flex: 0 1 auto;
        min-width: 0;
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
      }

      .method-tail {
        flex: 0 0 auto;
      }

      .percent {
        flex: 0 0 auto;
        margin-left: auto;
        padding-right: 5px;
        padding-left: 8px;
      }
    }
  }

  li:hover {
    border: 1px solid orange;
  }
}

.sort-list {
  i {
    font-style: normal;
    background: #fff;
    color: #00000082;
    font-size: 12px;
    border: 1px solid #eee;
    display: inline-block;
    font-weight: normal;
    padding: 2px;
    width: 60px;
    text-align: center;
    margin-right: 3px;
    cursor: pointer;
  }

  i:hover {
    background: #20bba6;
    border: 1px solid #20bba6;
    color: #fff;
  }

  .active {
    background: #20bba6;
    border: 1px solid #20bba6;
    color: #fff;
  }
}

.app-list {
  padding-left: 12px;
}

.sort-title {
  margin-bottom: 10px;
  padding: 3px 0 0 20px;
  position: relative;
  font-weight: bold;
  font-size: 14px;
}
</style>
