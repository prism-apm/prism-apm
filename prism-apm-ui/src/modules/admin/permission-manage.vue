<template>
  <div class="permission-manage">
    <div class="header">
      <Button type="primary" @click="goBack">
        <Icon type="md-arrow-back" />
        {{ $t('admin.common.backToCenter') }}
      </Button>
      <h2>{{ $t('admin.permission.title') }}</h2>
    </div>

    <Tabs v-model="activeTab" type="card">
      <TabPane :label="$t('admin.permission.userManage')" name="user">
        <div class="tab-content">
          <div class="toolbar">
            <Button type="primary" @click="showAddUserModal">
              <Icon type="md-add" />
              {{ $t('admin.permission.addUser') }}
            </Button>
            <Button @click="refreshUsers">
              <Icon type="md-refresh" />
              {{ $t('admin.common.refresh') }}
            </Button>
          </div>

          <Table :columns="userColumns" :data="userList" :loading="userLoading" stripe>
            <template slot-scope="{ row }" slot="actions">
              <Button type="info" size="small" @click="editUser(row)" style="margin-right: 5px;">{{
                $t('admin.common.edit') }}</Button>
              <Button type="error" size="small" @click="deleteUser(row.id)">{{ $t('admin.common.delete') }}</Button>
            </template>
          </Table>
        </div>
      </TabPane>

      <TabPane :label="$t('admin.permission.roleManage')" name="role">
        <div class="tab-content">
          <div class="toolbar">
            <Button type="primary" @click="showAddRoleModal">
              <Icon type="md-add" />
              {{ $t('admin.permission.addRole') }}
            </Button>
            <Button @click="refreshRoles">
              <Icon type="md-refresh" />
              {{ $t('admin.common.refresh') }}
            </Button>
          </div>

          <Table :columns="roleColumns" :data="roleList" :loading="roleLoading" stripe>
            <template slot-scope="{ row }" slot="actions">
              <Button type="info" size="small" @click="editRole(row)" style="margin-right: 5px;">{{
                $t('admin.common.edit') }}</Button>
              <Button type="error" size="small" @click="deleteRole(row.id)">{{ $t('admin.common.delete') }}</Button>
            </template>
          </Table>
        </div>
      </TabPane>

      <TabPane :label="$t('admin.permission.permManage')" name="permission">
        <div class="tab-content">
          <div class="toolbar">
            <Button type="primary" @click="showAddPermissionModal">
              <Icon type="md-add" />
              {{ $t('admin.permission.addPermission') }}
            </Button>
            <Button @click="refreshPermissions">
              <Icon type="md-refresh" />
              {{ $t('admin.common.refresh') }}
            </Button>
          </div>

          <Table :columns="permissionColumns" :data="permissionList" :loading="permissionLoading" stripe>
            <template slot-scope="{ row }" slot="actions">
              <Button type="info" size="small" @click="editPermission(row)" style="margin-right: 5px;">{{
                $t('admin.common.edit') }}</Button>
              <Button type="error" size="small" @click="deletePermission(row.id)">{{ $t('admin.common.delete')
              }}</Button>
            </template>
          </Table>
        </div>
      </TabPane>
    </Tabs>

    <Modal v-model="userModalVisible" :title="userModalTitle" @on-ok="handleUserSubmit" @on-cancel="resetUserForm">
      <Form ref="userForm" :model="userForm" :rules="userRules" :label-width="80">
        <FormItem :label="$t('admin.permission.username')" prop="username">
          <Input v-model="userForm.username" :placeholder="$t('admin.permission.enterUsername')" />
        </FormItem>
        <FormItem :label="$t('admin.permission.password')" prop="password">
          <Input v-model="userForm.password" type="password"
            :placeholder="userForm.id ? $t('admin.permission.passwordKeep') : $t('admin.permission.enterPassword')" />
        </FormItem>
      </Form>
    </Modal>

    <Modal v-model="roleModalVisible" :title="roleModalTitle" @on-ok="handleRoleSubmit" @on-cancel="resetRoleForm">
      <Form ref="roleForm" :model="roleForm" :rules="roleRules" :label-width="80">
        <FormItem :label="$t('admin.permission.roleName')" prop="name">
          <Input v-model="roleForm.name" :placeholder="$t('admin.permission.enterRoleName')" />
        </FormItem>
      </Form>
    </Modal>

    <Modal v-model="permissionModalVisible" :title="permissionModalTitle" @on-ok="handlePermissionSubmit"
      @on-cancel="resetPermissionForm">
      <Form ref="permissionForm" :model="permissionForm" :rules="permissionRules" :label-width="80">
        <FormItem :label="$t('admin.permission.permName')" prop="name">
          <Input v-model="permissionForm.name" :placeholder="$t('admin.permission.enterPermName')" />
        </FormItem>
        <FormItem :label="$t('admin.permission.permUrl')" prop="url">
          <Input v-model="permissionForm.url" :placeholder="$t('admin.permission.enterPermUrl')" />
        </FormItem>
      </Form>
    </Modal>
  </div>
</template>

<script>
import * as adminApi from '@/api/admin/req'

export default {
  name: 'PermissionManage',
  data() {
    return {
      activeTab: 'user',

      userList: [],
      userLoading: false,
      userModalVisible: false,
      userModalTitle: 'new user',
      userForm: {
        id: null,
        username: '',
        password: '',
        enabled: true
      },
      userRules: {
        username: [
          { required: true, message: 'usernameRequired', trigger: 'blur' }
        ],
        password: [
          {
            validator: (rule, value, callback) => {
              // 新增用户必填；编辑用户留空表示不修改密码
              if (!value && !this.userForm.id) {
                return callback(new Error('passwordRequired'))
              }
              if (value && value.length < 6) {
                return callback(new Error('passwordMinLength'))
              }
              callback()
            },
            trigger: 'blur'
          }
        ]
      },
      userColumns: [],

      roleList: [],
      roleLoading: false,
      roleModalVisible: false,
      roleModalTitle: this.$t('admin.permission.addRole'),
      roleForm: {
        id: null,
        name: ''
      },
      roleRules: {
        name: [
          { required: true, message: 'roleNameRequired', trigger: 'blur' }
        ]
      },
      roleColumns: [],

      permissionList: [],
      permissionLoading: false,
      permissionModalVisible: false,
      permissionModalTitle: this.$t('admin.permission.addPermission'),
      permissionForm: {
        id: null,
        name: '',
        url: ''
      },
      permissionRules: {
        name: [
          { required: true, message: 'permNameRequired', trigger: 'blur' }
        ],
        url: [
          { required: true, message: 'permUrlRequired', trigger: 'blur' }
        ]
      },
      permissionColumns: []
    }
  },

  mounted() {
    this.initColumns()
    this.loadData()
  },

  watch: {
    activeTab(newTab) {
      this.loadData()
    }
  },

  methods: {
    initColumns() {
      this.userColumns = [
        { title: 'ID', key: 'id', width: 80 },
        { title: this.$t('admin.permission.username'), key: 'username' },
        {
          title: this.$t('admin.common.status'), key: 'enabled', render: (h, params) => {
            return h('Tag', {
              props: { color: params.row.enabled ? 'success' : 'error' }
            }, params.row.enabled ? this.$t('admin.common.enabled') : this.$t('admin.common.disabled'))
          }
        },
        { title: this.$t('admin.common.createdAt'), key: 'createdAt' },
        { title: this.$t('admin.common.actions'), slot: 'actions', width: 150, align: 'center' }
      ]
      this.roleColumns = [
        { title: 'ID', key: 'id', width: 80 },
        { title: this.$t('admin.permission.roleName'), key: 'name' },
        { title: this.$t('admin.common.createdAt'), key: 'createdAt' },
        { title: this.$t('admin.common.actions'), slot: 'actions', width: 150, align: 'center' }
      ]
      this.permissionColumns = [
        { title: 'ID', key: 'id', width: 80 },
        { title: this.$t('admin.permission.permName'), key: 'name' },
        { title: this.$t('admin.permission.permUrl'), key: 'url' },
        { title: this.$t('admin.common.createdAt'), key: 'createdAt' },
        { title: this.$t('admin.common.actions'), slot: 'actions', width: 150, align: 'center' }
      ]
    },

    goBack() {
      this.$router.push('/admin/dashboard')
    },

    loadData() {
      if (this.activeTab === 'user') {
        this.loadUsers()
      } else if (this.activeTab === 'role') {
        this.loadRoles()
      } else if (this.activeTab === 'permission') {
        this.loadPermissions()
      }
    },

    async loadUsers() {
      this.userLoading = true
      try {
        const res = await adminApi.listUsers({ pageNum: 1, pageSize: 100 })
        if (res.status === 200) {
          this.userList = res.data || []
        }
      } catch (error) {
        this.$Message.error(this.$t('admin.permission.loadUsersFailed'))
      } finally {
        this.userLoading = false
      }
    },

    showAddUserModal() {
      this.userModalTitle = this.$t('admin.permission.addUser')
      this.resetUserForm()
      this.userModalVisible = true
    },

    editUser(row) {
      this.userModalTitle = this.$t('admin.permission.editUser')
      // 列表接口不返回密码，留空表示不修改
      this.userForm = { ...row, password: '' }
      this.userModalVisible = true
    },

    async handleUserSubmit() {
      try {
        await this.$refs.userForm.validate()
        const isEdit = !!this.userForm.id
        const apiMethod = isEdit ? adminApi.modifyUser : adminApi.addUser

        const res = await apiMethod(this.userForm)
        if (res.status === 200) {
          this.$Message.success(isEdit ? this.$t('admin.permission.userUpdateSuccess') : this.$t('admin.permission.userCreateSuccess'))
          this.userModalVisible = false
          this.loadUsers()
        }
      } catch (error) {
      }
    },

    async deleteUser(id) {
      this.$Modal.confirm({
        title: this.$t('admin.common.confirmDelete'),
        content: this.$t('admin.permission.confirmDeleteUser'),
        onOk: async () => {
          try {
            const res = await adminApi.deleteUser(id)
            if (res.status === 200) {
              this.$Message.success(this.$t('admin.permission.userDeleteSuccess'))
              this.loadUsers()
            }
          } catch (error) {
            this.$Message.error(this.$t('admin.common.deleteFailed'))
          }
        }
      })
    },

    resetUserForm() {
      this.userForm = {
        id: null,
        username: '',
        password: '',
        enabled: true
      }
      this.$refs.userForm && this.$refs.userForm.resetFields()
    },

    refreshUsers() {
      this.loadUsers()
    },

    async loadRoles() {
      this.roleLoading = true
      try {
        const res = await adminApi.listRoles({ pageNum: 1, pageSize: 100 })
        if (res.status === 200) {
          this.roleList = res.data || []
        }
      } catch (error) {
        this.$Message.error(this.$t('admin.permission.loadRolesFailed'))
      } finally {
        this.roleLoading = false
      }
    },

    showAddRoleModal() {
      this.roleModalTitle = this.$t('admin.permission.addRole')
      this.resetRoleForm()
      this.roleModalVisible = true
    },

    editRole(row) {
      this.roleModalTitle = this.$t('admin.permission.editRole')
      this.roleForm = { ...row }
      this.roleModalVisible = true
    },

    async handleRoleSubmit() {
      try {
        await this.$refs.roleForm.validate()
        const isEdit = !!this.roleForm.id
        const apiMethod = isEdit ? adminApi.modifyRole : adminApi.addRole

        const res = await apiMethod(this.roleForm)
        if (res.status === 200) {
          this.$Message.success(isEdit ? this.$t('admin.permission.roleUpdateSuccess') : this.$t('admin.permission.roleCreateSuccess'))
          this.roleModalVisible = false
          this.loadRoles()
        }
      } catch (error) {
      }
    },

    async deleteRole(id) {
      this.$Modal.confirm({
        title: this.$t('admin.common.confirmDelete'),
        content: this.$t('admin.permission.confirmDeleteRole'),
        onOk: async () => {
          try {
            const res = await adminApi.deleteRole(id)
            if (res.status === 200) {
              this.$Message.success(this.$t('admin.permission.roleDeleteSuccess'))
              this.loadRoles()
            }
          } catch (error) {
            this.$Message.error(this.$t('admin.common.deleteFailed'))
          }
        }
      })
    },

    resetRoleForm() {
      this.roleForm = {
        id: null,
        name: ''
      }
      this.$refs.roleForm && this.$refs.roleForm.resetFields()
    },

    refreshRoles() {
      this.loadRoles()
    },

    async loadPermissions() {
      this.permissionLoading = true
      try {
        const res = await adminApi.listPermissions({ pageNum: 1, pageSize: 100 })
        if (res.status === 200) {
          this.permissionList = res.data || []
        }
      } catch (error) {
        this.$Message.error(this.$t('admin.permission.loadPermissionsFailed'))
      } finally {
        this.permissionLoading = false
      }
    },

    showAddPermissionModal() {
      this.permissionModalTitle = this.$t('admin.permission.addPermission')
      this.resetPermissionForm()
      this.permissionModalVisible = true
    },

    editPermission(row) {
      this.permissionModalTitle = this.$t('admin.permission.editPermission')
      this.permissionForm = { ...row }
      this.permissionModalVisible = true
    },

    async handlePermissionSubmit() {
      try {
        await this.$refs.permissionForm.validate()
        const isEdit = !!this.permissionForm.id
        const apiMethod = isEdit ? adminApi.modifyPermission : adminApi.addPermission

        const res = await apiMethod(this.permissionForm)
        if (res.status === 200) {
          this.$Message.success(isEdit ? this.$t('admin.permission.permUpdateSuccess') : this.$t('admin.permission.permCreateSuccess'))
          this.permissionModalVisible = false
          this.loadPermissions()
        }
      } catch (error) {
      }
    },

    async deletePermission(id) {
      this.$Modal.confirm({
        title: this.$t('admin.common.confirmDelete'),
        content: this.$t('admin.permission.confirmDeletePermission'),
        onOk: async () => {
          try {
            const res = await adminApi.deletePermission(id)
            if (res.status === 200) {
              this.$Message.success(this.$t('admin.permission.permDeleteSuccess'))
              this.loadPermissions()
            }
          } catch (error) {
            this.$Message.error(this.$t('admin.common.deleteFailed'))
          }
        }
      })
    },

    resetPermissionForm() {
      this.permissionForm = {
        id: null,
        name: '',
        url: ''
      }
      this.$refs.permissionForm && this.$refs.permissionForm.resetFields()
    },

    refreshPermissions() {
      this.loadPermissions()
    }
  }
}
</script>

<style scoped>
.permission-manage {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header h2 {
  margin: 0 0 0 20px;
  color: #333;
}

.tab-content {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.toolbar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

.toolbar .ivu-btn {
  display: flex;
  align-items: center;
  gap: 5px;
}
</style>