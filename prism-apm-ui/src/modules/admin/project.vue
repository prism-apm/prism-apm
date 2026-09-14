<template>
  <div class="project-manage">
    <div class="header">
      <Button type="primary" @click="goBack">
        <Icon type="md-arrow-back" />
        {{ $t('admin.common.backToCenter') }}
      </Button>
      <h2>{{ $t('admin.project.title') }}</h2>
    </div>

    <Tabs v-model="activeTab" type="card">
      <TabPane :label="$t('admin.project.projectTab')" name="project">
        <div class="tab-content">
          <div class="toolbar">
            <Button type="primary" @click="showAddProjectModal">
              <Icon type="md-add" />
              {{ $t('admin.project.addProject') }}
            </Button>
            <Button @click="refreshProjects">
              <Icon type="md-refresh" />
              {{ $t('admin.common.refresh') }}
            </Button>
          </div>

          <Table :columns="projectColumns" :data="projectList" :loading="projectLoading" stripe>
            <template slot-scope="{ row }" slot="actions">
              <Button type="info" size="small" @click="editProject(row)" style="margin-right: 5px;">{{
                $t('admin.common.edit') }}</Button>
              <Button type="error" size="small" @click="deleteProject(row.id)">{{ $t('admin.common.delete') }}</Button>
            </template>
          </Table>
        </div>
      </TabPane>

      <TabPane :label="$t('admin.project.groupTab')" name="projectGroup">
        <div class="tab-content">
          <div class="toolbar">
            <Button type="primary" @click="showAddProjectGroupModal">
              <Icon type="md-add" />
              {{ $t('admin.project.addGroup') }}
            </Button>
            <Button @click="refreshProjectGroups">
              <Icon type="md-refresh" />
              {{ $t('admin.common.refresh') }}
            </Button>
          </div>

          <Table :columns="projectGroupColumns" :data="projectGroupList" :loading="projectGroupLoading" stripe>
            <template slot-scope="{ row }" slot="actions">
              <Button type="info" size="small" @click="editProjectGroup(row)" style="margin-right: 5px;">{{
                $t('admin.common.edit') }}</Button>
              <Button type="error" size="small" @click="deleteProjectGroup(row.id)">{{ $t('admin.common.delete')
              }}</Button>
            </template>
          </Table>
        </div>
      </TabPane>

      <TabPane :label="$t('admin.assign.projectGroupProjectTab')" name="projectGroupProject">
        <div class="tab-content">
          <div class="toolbar">
            <Button type="primary" @click="showAssignProjectGroupProjectModal">
              <Icon type="md-add" />
              {{ $t('admin.assign.assignProjectToGroup') }}
            </Button>
            <Button @click="refreshProjectGroupProjects">
              <Icon type="md-refresh" />
              {{ $t('admin.common.refresh') }}
            </Button>
          </div>

          <Table :columns="projectGroupProjectColumns" :data="projectGroupProjectList"
            :loading="projectGroupProjectLoading" stripe>
            <template slot-scope="{ row }" slot="actions">
              <Button type="error" size="small" @click="removeProjectGroupProject(row.relationId)">{{
                $t('admin.common.remove') }}</Button>
            </template>
          </Table>
        </div>
      </TabPane>
    </Tabs>

    <Modal v-model="projectModalVisible" :title="projectModalTitle" @on-ok="handleProjectSubmit"
      @on-cancel="resetProjectForm">
      <Form ref="projectForm" :model="projectForm" :rules="projectRules" :label-width="80">
        <FormItem :label="$t('admin.project.projectName')" prop="name">
          <Input v-model="projectForm.name" :placeholder="$t('admin.project.enterProjectName')" />
        </FormItem>
        <FormItem :label="$t('admin.project.projectDisplay')" prop="display">
          <Input v-model="projectForm.display" :placeholder="$t('admin.project.enterProjectDisplay')" />
        </FormItem>
      </Form>
    </Modal>

    <Modal v-model="projectGroupModalVisible" :title="projectGroupModalTitle" @on-ok="handleProjectGroupSubmit"
      @on-cancel="resetProjectGroupForm">
      <Form ref="projectGroupForm" :model="projectGroupForm" :rules="projectGroupRules" :label-width="80">
        <FormItem :label="$t('admin.project.groupName')" prop="name">
          <Input v-model="projectGroupForm.name" :placeholder="$t('admin.project.enterGroupName')" />
        </FormItem>
      </Form>
    </Modal>

    <Modal v-model="projectGroupProjectModalVisible" :title="$t('admin.assign.assignProjectToGroup')"
      @on-ok="handleProjectGroupProjectAssign" @on-cancel="resetProjectGroupProjectForm">
      <Form ref="projectGroupProjectForm" :model="projectGroupProjectForm" :rules="projectGroupProjectRules"
        :label-width="80">
        <FormItem :label="$t('admin.assign.selectProjectGroup')" prop="projectGroupId">
          <Select v-model="projectGroupProjectForm.projectGroupId"
            :placeholder="$t('admin.assign.pleaseSelectProjectGroup')" filterable>
            <Option v-for="group in projectGroupOptions" :key="group.id" :value="group.id">
              {{ group.name }}
            </Option>
          </Select>
        </FormItem>
        <FormItem :label="$t('admin.assign.selectProject')" prop="projectId">
          <Select v-model="projectGroupProjectForm.projectId" :placeholder="$t('admin.assign.pleaseSelectProject')"
            filterable>
            <Option v-for="project in projectOptions" :key="project.id" :value="project.id">
              {{ project.display || project.name }}
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
  name: 'ProjectManage',
  data() {
    return {
      activeTab: 'project',

      projectList: [],
      projectLoading: false,
      projectModalVisible: false,
      projectModalTitle: 'new project',
      projectForm: {
        id: null,
        name: '',
        display: ''
      },
      projectRules: {
        name: [
          { required: true, message: 'projectNameRequired', trigger: 'blur' }
        ],
        display: [
          { required: true, message: 'projectDisplayRequired', trigger: 'blur' }
        ]
      },
      projectColumns: [],

      projectGroupList: [],
      projectGroupLoading: false,
      projectGroupModalVisible: false,
      projectGroupModalTitle: 'new project group',
      projectGroupForm: {
        id: null,
        name: ''
      },
      projectGroupRules: {
        name: [
          { required: true, message: 'groupNameRequired', trigger: 'blur' }
        ]
      },
      projectGroupColumns: [],

      projectGroupOptions: [],
      projectOptions: [],
      projectGroupProjectList: [],
      projectGroupProjectLoading: false,
      projectGroupProjectModalVisible: false,
      projectGroupProjectForm: {
        projectGroupId: null,
        projectId: null
      },
      projectGroupProjectRules: {
        projectGroupId: [{ required: true, type: 'number', message: 'selectGroupRequired', trigger: 'change' }],
        projectId: [{ required: true, type: 'number', message: 'selectProjectRequired', trigger: 'change' }]
      },
      projectGroupProjectColumns: []
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
      this.projectColumns = [
        { title: 'ID', key: 'id', width: 80 },
        { title: this.$t('admin.project.projectName'), key: 'name' },
        { title: this.$t('admin.project.projectDisplay'), key: 'display' },
        { title: this.$t('admin.common.createdAt'), key: 'createdAt' },
        { title: this.$t('admin.common.actions'), slot: 'actions', width: 150, align: 'center' }
      ]
      this.projectGroupColumns = [
        { title: 'ID', key: 'id', width: 80 },
        { title: this.$t('admin.project.groupName'), key: 'name' },
        { title: this.$t('admin.common.createdAt'), key: 'createdAt' },
        { title: this.$t('admin.common.actions'), slot: 'actions', width: 150, align: 'center' }
      ]
      this.projectGroupProjectColumns = [
        { title: 'ID', key: 'relationId', width: 80 },
        { title: this.$t('admin.assign.groupName'), key: 'groupName' },
        { title: this.$t('admin.assign.projectName'), key: 'projectName' },
        { title: this.$t('admin.assign.projectDisplay'), key: 'projectDisplay' },
        { title: this.$t('admin.assign.assignedTime'), key: 'createTime' },
        { title: this.$t('admin.common.actions'), slot: 'actions', width: 100, align: 'center' }
      ]
    },

    goBack() {
      this.$router.push('/admin/dashboard')
    },

    loadData() {
      if (this.activeTab === 'project') {
        this.loadProjects()
      } else if (this.activeTab === 'projectGroup') {
        this.loadProjectGroups()
      } else if (this.activeTab === 'projectGroupProject') {
        this.loadProjectGroupProjects()
      }
    },

    async loadProjects() {
      this.projectLoading = true
      try {
        const res = await adminApi.listProjects({ pageNum: 1, pageSize: 100 })
        if (res.status === 200) {
          this.projectList = res.data || []
        }
      } catch (error) {
        this.$Message.error(this.$t('admin.project.loadProjectsFailed'))
      } finally {
        this.projectLoading = false
      }
    },

    showAddProjectModal() {
      this.projectModalTitle = this.$t('admin.project.addProject')
      this.resetProjectForm()
      this.projectModalVisible = true
    },

    editProject(row) {
      this.projectModalTitle = this.$t('admin.project.editProject')
      this.projectForm = { ...row }
      this.projectModalVisible = true
    },

    async handleProjectSubmit() {
      try {
        await this.$refs.projectForm.validate()
        const isEdit = !!this.projectForm.id
        const apiMethod = isEdit ? adminApi.modifyProject : adminApi.addProject

        const res = await apiMethod(this.projectForm)
        if (res.status === 200) {
          this.$Message.success(isEdit ? this.$t('admin.project.projectUpdateSuccess') : this.$t('admin.project.projectCreateSuccess'))
          this.projectModalVisible = false
          this.loadProjects()
        } else {
          this.$Message.error(res.message || this.$t('admin.common.deleteFailed'))
        }
      } catch (error) {
      }
    },

    async deleteProject(id) {
      this.$Modal.confirm({
        title: this.$t('admin.common.confirmDelete'),
        content: this.$t('admin.project.confirmDeleteProject'),
        onOk: async () => {
          try {
            const res = await adminApi.deleteProject(id)
            if (res.status === 200) {
              this.$Message.success(this.$t('admin.project.projectDeleteSuccess'))
              this.loadProjects()
            } else {
              this.$Message.error(res.message || this.$t('admin.common.deleteFailed'))
            }
          } catch (error) {
            this.$Message.error(this.$t('admin.common.deleteFailed'))
          }
        }
      })
    },

    resetProjectForm() {
      this.projectForm = {
        id: null,
        name: '',
        display: ''
      }
      this.$refs.projectForm && this.$refs.projectForm.resetFields()
    },

    refreshProjects() {
      this.loadProjects()
    },

    async loadProjectGroups() {
      this.projectGroupLoading = true
      try {
        const res = await adminApi.listProjectGroups({ pageNum: 1, pageSize: 100 })
        if (res.status === 200) {
          this.projectGroupList = res.data || []
        }
      } catch (error) {
        this.$Message.error(this.$t('admin.project.loadGroupsFailed'))
      } finally {
        this.projectGroupLoading = false
      }
    },

    showAddProjectGroupModal() {
      this.projectGroupModalTitle = this.$t('admin.project.addGroup')
      this.resetProjectGroupForm()
      this.projectGroupModalVisible = true
    },

    editProjectGroup(row) {
      this.projectGroupModalTitle = this.$t('admin.project.editGroup')
      this.projectGroupForm = { ...row }
      this.projectGroupModalVisible = true
    },

    async handleProjectGroupSubmit() {
      try {
        await this.$refs.projectGroupForm.validate()
        const isEdit = !!this.projectGroupForm.id
        const apiMethod = isEdit ? adminApi.modifyProjectGroup : adminApi.addProjectGroup

        const res = await apiMethod(this.projectGroupForm)
        if (res.status === 200) {
          this.$Message.success(isEdit ? this.$t('admin.project.groupUpdateSuccess') : this.$t('admin.project.groupCreateSuccess'))
          this.projectGroupModalVisible = false
          this.loadProjectGroups()
        } else {
          this.$Message.error(res.message || this.$t('admin.common.deleteFailed'))
        }
      } catch (error) {
      }
    },

    async deleteProjectGroup(id) {
      this.$Modal.confirm({
        title: this.$t('admin.common.confirmDelete'),
        content: this.$t('admin.project.confirmDeleteGroup'),
        onOk: async () => {
          try {
            const res = await adminApi.deleteProjectGroup(id)
            if (res.status === 200) {
              this.$Message.success(this.$t('admin.project.groupDeleteSuccess'))
              this.loadProjectGroups()
            } else {
              this.$Message.error(res.message || this.$t('admin.common.deleteFailed'))
            }
          } catch (error) {
            this.$Message.error(this.$t('admin.common.deleteFailed'))
          }
        }
      })
    },

    resetProjectGroupForm() {
      this.projectGroupForm = {
        id: null,
        name: ''
      }
      this.$refs.projectGroupForm && this.$refs.projectGroupForm.resetFields()
    },

    refreshProjectGroups() {
      this.loadProjectGroups()
    },

    async loadOptions() {
      try {
        const [projectGroups, projects] = await Promise.all([
          adminApi.listProjectGroups({ pageNum: 1, pageSize: 1000 }),
          adminApi.listProjects({ pageNum: 1, pageSize: 1000 })
        ])
        this.projectGroupOptions = projectGroups.data || []
        this.projectOptions = projects.data || []
      } catch (error) {
        this.$Message.error(this.$t('admin.assign.loadOptionsFailed'))
      }
    },

    async loadProjectGroupProjects() {
      this.projectGroupProjectLoading = true
      try {
        const res = await adminApi.listProjectGroupProjectRelations({ pageNum: 1, pageSize: 100 })
        if (res.status === 200) {
          this.projectGroupProjectList = res.data || []
        }
      } catch (error) {
        this.$Message.error(this.$t('admin.assign.loadProjectGroupProjectsFailed'))
      } finally {
        this.projectGroupProjectLoading = false
      }
    },

    showAssignProjectGroupProjectModal() {
      this.resetProjectGroupProjectForm()
      this.loadOptions()
      this.projectGroupProjectModalVisible = true
    },

    async handleProjectGroupProjectAssign() {
      try {
        await this.$refs.projectGroupProjectForm.validate()
        const res = await adminApi.assignProjectToProjectGroup(this.projectGroupProjectForm.projectGroupId, this.projectGroupProjectForm.projectId)
        if (res.status === 200) {
          this.$Message.success(this.$t('admin.assign.projectToGroupSuccess'))
          this.projectGroupProjectModalVisible = false
          this.loadProjectGroupProjects()
        } else {
          this.$Message.error(res.message || this.$t('admin.common.assignFailed'))
        }
      } catch (error) {
      }
    },

    async removeProjectGroupProject(relationId) {
      this.$Modal.confirm({
        title: this.$t('admin.common.confirmRemove'),
        content: this.$t('admin.assign.confirmRemoveProject'),
        onOk: async () => {
          try {
            const res = await adminApi.removeProjectGroupProject(relationId)
            if (res.status === 200) {
              this.$Message.success(this.$t('admin.assign.projectRemoveSuccess'))
              this.loadProjectGroupProjects()
            } else {
              this.$Message.error(res.message || this.$t('admin.common.removeFailed'))
            }
          } catch (error) {
            this.$Message.error(this.$t('admin.common.removeFailed'))
          }
        }
      })
    },

    resetProjectGroupProjectForm() {
      this.projectGroupProjectForm = {
        projectGroupId: null,
        projectId: null
      }
      this.$refs.projectGroupProjectForm && this.$refs.projectGroupProjectForm.resetFields()
    },

    refreshProjectGroupProjects() {
      this.loadProjectGroupProjects()
    }
  }
}
</script>

<style scoped>
.project-manage {
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