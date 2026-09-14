<template>
  <div class="assign-manage">
    <div class="header">
      <Button type="primary" @click="goBack">
        <Icon type="md-arrow-back" />
        {{ $t('admin.common.backToCenter') }}
      </Button>
      <h2>{{ $t('admin.assign.title') }}</h2>
    </div>

    <Tabs v-model="activeTab" type="card">
      <TabPane :label="$t('admin.assign.userRoleTab')" name="userRole">
        <div class="tab-content">
          <div class="toolbar">
            <Button type="primary" @click="showAssignUserRoleModal">
              <Icon type="md-add" />
              {{ $t('admin.assign.assignUserRole') }}
            </Button>
            <Button @click="refreshUserRoles">
              <Icon type="md-refresh" />
              {{ $t('admin.common.refresh') }}
            </Button>
          </div>

          <Table :columns="userRoleColumns" :data="userRoleList" :loading="userRoleLoading" stripe>
            <template slot-scope="{ row }" slot="actions">
              <Button type="error" size="small" @click="removeUserRole(row.relationId)">{{ $t('admin.common.remove')
              }}</Button>
            </template>
          </Table>
        </div>
      </TabPane>

      <TabPane :label="$t('admin.assign.rolePermTab')" name="rolePermission">
        <div class="tab-content">
          <div class="toolbar">
            <Button type="primary" @click="showAssignRolePermissionModal">
              <Icon type="md-add" />
              {{ $t('admin.assign.assignRolePerm') }}
            </Button>
            <Button @click="refreshRolePermissions">
              <Icon type="md-refresh" />
              {{ $t('admin.common.refresh') }}
            </Button>
          </div>

          <Table :columns="rolePermissionColumns" :data="rolePermissionList" :loading="rolePermissionLoading" stripe>
            <template slot-scope="{ row }" slot="actions">
              <Button type="error" size="small" @click="removeRolePermission(row.relationId)">{{
                $t('admin.common.remove') }}</Button>
            </template>
          </Table>
        </div>
      </TabPane>

      <TabPane :label="$t('admin.assign.projectGroupUserTab')" name="projectGroupUser">
        <div class="tab-content">
          <div class="toolbar">
            <Button type="primary" @click="showAssignProjectGroupUserModal">
              <Icon type="md-add" />
              {{ $t('admin.assign.assignUserToGroup') }}
            </Button>
            <Button @click="refreshProjectGroupUsers">
              <Icon type="md-refresh" />
              {{ $t('admin.common.refresh') }}
            </Button>
          </div>

          <Table :columns="projectGroupUserColumns" :data="projectGroupUserList" :loading="projectGroupUserLoading"
            stripe>
            <template slot-scope="{ row }" slot="actions">
              <Button type="error" size="small" @click="removeProjectGroupUser(row.relationId)">{{
                $t('admin.common.remove') }}</Button>
            </template>
          </Table>
        </div>
      </TabPane>
    </Tabs>

    <Modal v-model="userRoleModalVisible" :title="$t('admin.assign.assignUserRole')" @on-ok="handleUserRoleAssign"
      @on-cancel="resetUserRoleForm">
      <Form ref="userRoleForm" :model="userRoleForm" :rules="userRoleRules" :label-width="80">
        <FormItem :label="$t('admin.assign.selectUser')" prop="userId">
          <Select v-model="userRoleForm.userId" :placeholder="$t('admin.assign.pleaseSelectUser')" filterable>
            <Option v-for="user in userOptions" :key="user.id" :value="user.id">
              {{ user.username }}
            </Option>
          </Select>
        </FormItem>
        <FormItem :label="$t('admin.assign.selectRole')" prop="roleId">
          <Select v-model="userRoleForm.roleId" :placeholder="$t('admin.assign.pleaseSelectRole')" filterable>
            <Option v-for="role in roleOptions" :key="role.id" :value="role.id">
              {{ role.name }}
            </Option>
          </Select>
        </FormItem>
      </Form>
    </Modal>

    <Modal v-model="rolePermissionModalVisible" :title="$t('admin.assign.assignRolePerm')"
      @on-ok="handleRolePermissionAssign" @on-cancel="resetRolePermissionForm">
      <Form ref="rolePermissionForm" :model="rolePermissionForm" :rules="rolePermissionRules" :label-width="80">
        <FormItem :label="$t('admin.assign.selectRole')" prop="roleId">
          <Select v-model="rolePermissionForm.roleId" :placeholder="$t('admin.assign.pleaseSelectRole')" filterable>
            <Option v-for="role in roleOptions" :key="role.id" :value="role.id">
              {{ role.name }}
            </Option>
          </Select>
        </FormItem>
        <FormItem :label="$t('admin.assign.selectPermission')" prop="permissionId">
          <Select v-model="rolePermissionForm.permissionId" :placeholder="$t('admin.assign.pleaseSelectPermission')"
            filterable>
            <Option v-for="permission in permissionOptions" :key="permission.id" :value="permission.id">
              {{ permission.name }} ({{ permission.url }})
            </Option>
          </Select>
        </FormItem>
      </Form>
    </Modal>

    <Modal v-model="projectGroupUserModalVisible" :title="$t('admin.assign.assignUserToGroup')"
      @on-ok="handleProjectGroupUserAssign" @on-cancel="resetProjectGroupUserForm">
      <Form ref="projectGroupUserForm" :model="projectGroupUserForm" :rules="projectGroupUserRules" :label-width="80">
        <FormItem :label="$t('admin.assign.selectProjectGroup')" prop="projectGroupId">
          <Select v-model="projectGroupUserForm.projectGroupId"
            :placeholder="$t('admin.assign.pleaseSelectProjectGroup')" filterable>
            <Option v-for="group in projectGroupOptions" :key="group.id" :value="group.id">
              {{ group.name }}
            </Option>
          </Select>
        </FormItem>
        <FormItem :label="$t('admin.assign.selectUser')" prop="userId">
          <Select v-model="projectGroupUserForm.userId" :placeholder="$t('admin.assign.pleaseSelectUser')" filterable>
            <Option v-for="user in userOptions" :key="user.id" :value="user.id">
              {{ user.username }}
            </Option>
          </Select>
        </FormItem>
      </Form>
    </Modal>
  </div>
</template>

<script>
import * as adminApi from '@/api/admin/req'

export default {
  name: 'AssignManage',
  data() {
    return {
      activeTab: 'userRole',

      userOptions: [],
      roleOptions: [],
      permissionOptions: [],
      projectGroupOptions: [],
      projectOptions: [],

      userRoleList: [],
      userRoleLoading: false,
      userRoleModalVisible: false,
      userRoleForm: {
        userId: null,
        roleId: null
      },
      userRoleRules: {
        userId: [{ required: true, type: 'number', message: 'selectUserRequired', trigger: 'change' }],
        roleId: [{ required: true, type: 'number', message: 'selectRoleRequired', trigger: 'change' }]
      },
      userRoleColumns: [],

      rolePermissionList: [],
      rolePermissionLoading: false,
      rolePermissionModalVisible: false,
      rolePermissionForm: {
        roleId: null,
        permissionId: null
      },
      rolePermissionRules: {
        roleId: [{ required: true, type: 'number', message: 'selectRoleRequired', trigger: 'change' }],
        permissionId: [{ required: true, type: 'number', message: 'selectPermRequired', trigger: 'change' }]
      },
      rolePermissionColumns: [],

      projectGroupUserList: [],
      projectGroupUserLoading: false,
      projectGroupUserModalVisible: false,
      projectGroupUserForm: {
        projectGroupId: null,
        userId: null
      },
      projectGroupUserRules: {
        projectGroupId: [{ required: true, type: 'number', message: 'selectGroupRequired', trigger: 'change' }],
        userId: [{ required: true, type: 'number', message: 'selectUserRequired', trigger: 'change' }]
      },
      projectGroupUserColumns: []
    }
  },

  mounted() {
    this.initColumns()
    this.loadOptions()
    this.loadData()
  },

  watch: {
    activeTab(newTab) {
      this.loadData()
    }
  },

  methods: {
    initColumns() {
      this.userRoleColumns = [
        { title: 'ID', key: 'relationId', width: 80 },
        { title: this.$t('admin.permission.username'), key: 'username' },
        { title: this.$t('admin.permission.roleName'), key: 'roleName' },
        { title: this.$t('admin.assign.assignedTime'), key: 'createTime' },
        { title: this.$t('admin.common.actions'), slot: 'actions', width: 100, align: 'center' }
      ]
      this.rolePermissionColumns = [
        { title: 'ID', key: 'relationId', width: 80 },
        { title: this.$t('admin.permission.roleName'), key: 'roleName' },
        { title: this.$t('admin.permission.permName'), key: 'permissionName' },
        { title: this.$t('admin.permission.permUrl'), key: 'url' },
        { title: this.$t('admin.assign.assignedTime'), key: 'createTime' },
        { title: this.$t('admin.common.actions'), slot: 'actions', width: 100, align: 'center' }
      ]
      this.projectGroupUserColumns = [
        { title: 'ID', key: 'relationId', width: 80 },
        { title: this.$t('admin.assign.groupName'), key: 'groupName' },
        { title: this.$t('admin.permission.username'), key: 'username' },
        { title: this.$t('admin.assign.assignedTime'), key: 'createTime' },
        { title: this.$t('admin.common.actions'), slot: 'actions', width: 100, align: 'center' }
      ]
    },

    goBack() {
      this.$router.push('/admin/dashboard')
    },

    async loadOptions() {
      try {
        const [users, roles, permissions, projectGroups, projects] = await Promise.all([
          adminApi.listUsers({ pageNum: 1, pageSize: 1000 }),
          adminApi.listRoles({ pageNum: 1, pageSize: 1000 }),
          adminApi.listPermissions({ pageNum: 1, pageSize: 1000 }),
          adminApi.listProjectGroups({ pageNum: 1, pageSize: 1000 }),
          adminApi.listProjects({ pageNum: 1, pageSize: 1000 })
        ])

        this.userOptions = users.data || []
        this.roleOptions = roles.data || []
        this.permissionOptions = permissions.data || []
        this.projectGroupOptions = projectGroups.data || []
        this.projectOptions = projects.data || []
      } catch (error) {
        this.$Message.error(this.$t('admin.assign.loadOptionsFailed'))
      }
    },

    loadData() {
      if (this.activeTab === 'userRole') {
        this.loadUserRoles()
      } else if (this.activeTab === 'rolePermission') {
        this.loadRolePermissions()
      } else if (this.activeTab === 'projectGroupUser') {
        this.loadProjectGroupUsers()
      }
    },

    async loadUserRoles() {
      this.userRoleLoading = true
      try {
        const res = await adminApi.listUserRoleRelations({ pageNum: 1, pageSize: 100 })
        if (res.status === 200) {
          this.userRoleList = res.data || []
        }
      } catch (error) {
        this.$Message.error(this.$t('admin.assign.loadUserRolesFailed'))
      } finally {
        this.userRoleLoading = false
      }
    },

    async loadRolePermissions() {
      this.rolePermissionLoading = true
      try {
        const res = await adminApi.listRolePermissionRelations({ pageNum: 1, pageSize: 100 })
        if (res.status === 200) {
          this.rolePermissionList = res.data || []
        }
      } catch (error) {
        this.$Message.error(this.$t('admin.assign.loadRolePermsFailed'))
      } finally {
        this.rolePermissionLoading = false
      }
    },

    async loadProjectGroupUsers() {
      this.projectGroupUserLoading = true
      try {
        const res = await adminApi.listProjectGroupUserRelations({ pageNum: 1, pageSize: 100 })
        if (res.status === 200) {
          this.projectGroupUserList = res.data || []
        }
      } catch (error) {
        this.$Message.error(this.$t('admin.assign.loadProjectGroupUsersFailed'))
      } finally {
        this.projectGroupUserLoading = false
      }
    },

    showAssignUserRoleModal() {
      this.resetUserRoleForm()
      this.userRoleModalVisible = true
    },

    async handleUserRoleAssign() {
      try {
        await this.$refs.userRoleForm.validate()
        const res = await adminApi.assignRoleToUser(this.userRoleForm.roleId, this.userRoleForm.userId)
        if (res.status === 200) {
          this.$Message.success(this.$t('admin.assign.userRoleAssignSuccess'))
          this.userRoleModalVisible = false
          this.loadUserRoles()
        } else {
          this.$Message.error(res.message || this.$t('admin.common.assignFailed'))
        }
      } catch (error) {
      }
    },

    async removeUserRole(relationId) {
      this.$Modal.confirm({
        title: this.$t('admin.common.confirmRemove'),
        content: this.$t('admin.assign.confirmRemoveUserRole'),
        onOk: async () => {
          try {
            const res = await adminApi.removeUserRole(relationId)
            if (res.status === 200) {
              this.$Message.success(this.$t('admin.assign.userRoleRemoveSuccess'))
              this.loadUserRoles()
            } else {
              this.$Message.error(res.message || this.$t('admin.common.removeFailed'))
            }
          } catch (error) {
            this.$Message.error(this.$t('admin.common.removeFailed'))
          }
        }
      })
    },

    resetUserRoleForm() {
      this.userRoleForm = {
        userId: null,
        roleId: null
      }
      this.$refs.userRoleForm && this.$refs.userRoleForm.resetFields()
    },

    refreshUserRoles() {
      this.loadUserRoles()
    },

    showAssignRolePermissionModal() {
      this.resetRolePermissionForm()
      this.rolePermissionModalVisible = true
    },

    async handleRolePermissionAssign() {
      try {
        await this.$refs.rolePermissionForm.validate()
        const res = await adminApi.assignPermissionToRole(this.rolePermissionForm.roleId, this.rolePermissionForm.permissionId)
        if (res.status === 200) {
          this.$Message.success(this.$t('admin.assign.rolePermAssignSuccess'))
          this.rolePermissionModalVisible = false
          this.loadRolePermissions()
        } else {
          this.$Message.error(res.message || this.$t('admin.common.assignFailed'))
        }
      } catch (error) {
      }
    },

    async removeRolePermission(relationId) {
      this.$Modal.confirm({
        title: this.$t('admin.common.confirmRemove'),
        content: this.$t('admin.assign.confirmRemoveRolePerm'),
        onOk: async () => {
          try {
            const res = await adminApi.removeRolePermission(relationId)
            if (res.status === 200) {
              this.$Message.success(this.$t('admin.assign.rolePermRemoveSuccess'))
              this.loadRolePermissions()
            } else {
              this.$Message.error(res.message || this.$t('admin.common.removeFailed'))
            }
          } catch (error) {
            this.$Message.error(this.$t('admin.common.removeFailed'))
          }
        }
      })
    },

    resetRolePermissionForm() {
      this.rolePermissionForm = {
        roleId: null,
        permissionId: null
      }
      this.$refs.rolePermissionForm && this.$refs.rolePermissionForm.resetFields()
    },

    refreshRolePermissions() {
      this.loadRolePermissions()
    },

    showAssignProjectGroupUserModal() {
      this.resetProjectGroupUserForm()
      this.projectGroupUserModalVisible = true
    },

    async handleProjectGroupUserAssign() {
      try {
        await this.$refs.projectGroupUserForm.validate()
        const res = await adminApi.assignUserToProjectGroup(this.projectGroupUserForm.projectGroupId, this.projectGroupUserForm.userId)
        if (res.status === 200) {
          this.$Message.success(this.$t('admin.assign.userToGroupSuccess'))
          this.projectGroupUserModalVisible = false
          this.loadProjectGroupUsers()
        } else {
          this.$Message.error(res.message || this.$t('admin.common.assignFailed'))
        }
      } catch (error) {
      }
    },

    async removeProjectGroupUser(relationId) {
      this.$Modal.confirm({
        title: this.$t('admin.common.confirmRemove'),
        content: this.$t('admin.assign.confirmRemoveUser'),
        onOk: async () => {
          try {
            const res = await adminApi.removeProjectGroupUser(relationId)
            if (res.status === 200) {
              this.$Message.success(this.$t('admin.assign.userRemoveSuccess'))
              this.loadProjectGroupUsers()
            } else {
              this.$Message.error(res.message || this.$t('admin.common.removeFailed'))
            }
          } catch (error) {
            this.$Message.error(this.$t('admin.common.removeFailed'))
          }
        }
      })
    },

    resetProjectGroupUserForm() {
      this.projectGroupUserForm = {
        projectGroupId: null,
        userId: null
      }
      this.$refs.projectGroupUserForm && this.$refs.projectGroupUserForm.resetFields()
    },

    refreshProjectGroupUsers() {
      this.loadProjectGroupUsers()
    }
  }
}
</script>

<style scoped>
.assign-manage {
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