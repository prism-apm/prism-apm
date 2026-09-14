<template>
  <div class="side-menu-wrapper">
    <Menu v-show="!collapsed" ref="sideMenu" :active-name="activeName" width="auto" @on-select="handleSelect">
      <template v-for="item in menuList">
        <Submenu v-if="item.children && item.children.length > 1" :name="item.path" :key="'open_' + item.name">
          <template>
            <Icon :type="item.icon"></Icon>
            <span>{{ $t(item.name) }}</span>
          </template>
          <MenuItem v-for="childItem in item.children" :key="'open_' + childItem.path" :name="childItem.path">
          {{ $t(childItem.name) }}
          </MenuItem>
        </Submenu>

        <MenuItem v-else :key="'open_' + (item.children ? item.children[0].name : item.name)"
          :name="item.children[0].path">
        <Icon :type="item.icon"></Icon>
        <span>{{ $t(item.children[0].name) }}</span>
        </MenuItem>
      </template>
    </Menu>

    <div v-show="collapsed" class="menu-collapsed">
      <template v-for="item in menuList">
        <Dropdown ref="dropdown" class="collapsed-menu-item" v-if="item.children && item.children.length > 1"
          placement="right-start" :key="'collapsed_' + item.name" @on-click="handleSelect">
          <a href="javascript:void(0)" class="drop-trigger">
            <Icon :type="item.icon" size="24" :class="{ 'icon-active': isActive(item.path) }"></Icon>
          </a>
          <DropdownMenu>
            <DropdownItem v-for="childItem in item.children" :key="'collapsed_' + childItem.path"
              :name="childItem.path">
              {{ $t(childItem.name) }}
            </DropdownItem>
          </DropdownMenu>
        </Dropdown>

        <Tooltip v-else class="collapsed-menu-item" :content="$t(item.children[0].name)" placement="right" transfer
          :key="'collapsed_' + (item.children ? item.children[0].name : item.name)">
          <a href="javascript:void(0)" class="drop-trigger" @click="handleSelect(item.children[0].path)">
            <Icon :type="item.icon" size="24" :class="{ 'icon-active': isActive(item.children[0].path) }"></Icon>
          </a>
        </Tooltip>
      </template>
    </div>
  </div>
</template>

<script>
import {useAppStore} from "@/store/store";

export default {
  name: 'SideMenu',
  props: {
    collapsed: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    menuList() {
      let menuList = [...useAppStore().menuList];
      let app = useAppStore().app;
      return menuList.filter(item => item.app === app && !item.hideInMenu);
    },
    activeName() {
      return useAppStore().activeRoutePath;
    }
  },
  watch: {
    activeName() {
      if (!this.collapsed) {
        this.$nextTick(() => {
          if (this.$refs.sideMenu) {
            this.$refs.sideMenu.updateActiveName();
          }
        });
      }
    },
    collapsed(val) {
      if (!val) {
        this.$nextTick(() => {
          if (this.$refs.sideMenu) {
            this.$refs.sideMenu.updateOpened();
            this.$refs.sideMenu.updateActiveName();
          }
        });
      }
    },
    $route: {
      handler(to) {
        useAppStore().app = to.matched.map(item => item.meta.app).find(item => item !== undefined);
        useAppStore().activeRoutePath = to.path;
      },
      immediate: true
    }
  },
  methods: {
    handleSelect(path) {
      if (this.$route.path !== path) {
        this.$router.push({ path: path });
      }
    },
    isActive(path) {
      return this.$route.path.indexOf(path) !== -1;
    }
  }
}
</script>

<style scoped lang="less">
.side-menu-wrapper {
  user-select: none;

  .ivu-menu-item span,
  .ivu-menu-submenu-title span {
    vertical-align: middle;
  }
}

.menu-collapsed {
  padding-top: 5px;
  text-align: center;

  .collapsed-menu-item {
    display: block !important;
    margin: 0 auto;
    padding: 2px 0;
  }

  .drop-trigger {
    display: block;
    padding: 10px 0;
    color: rgba(255, 255, 255, 0.7);
    transition: color .2s;

    &:hover {
      color: #fff;
    }

    .icon-active {
      color: #20b5ad;
    }
  }
}

.ivu-menu {
  background: #28282e;

  &:after {
    display: none !important;
    width: 0 !important;
  }
}

.ivu-menu-item,
.ivu-menu-submenu-title {
  color: #95a0aa !important;
  padding: 12px 24px;

  border-right: 2px solid transparent;

  transition: all 0.2s ease-in-out;
}

.ivu-menu-item-active {
  background: #20b5ad !important;
  color: #fff !important;
  font-size: 14px;
  font-weight: bold;
}

.ivu-menu-light.ivu-menu-vertical .ivu-menu-item-active:not(.ivu-menu-submenu):after {
  background: #20b5ad !important;
}

.ivu-menu-item:hover,
.ivu-menu-submenu-title:hover {
  background: #20b5ad !important;
  color: #fff !important;
}

.icon-active {
  color: #20b5ad;
}

.side-bar .ivu-icon {
  font-size: 19px;
  margin-right: 12px;
}
</style>