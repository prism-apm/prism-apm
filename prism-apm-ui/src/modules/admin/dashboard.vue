<template>
  <div class="admin-dashboard">
    <div class="header">
      <h1>{{ $t('admin.dashboard.title') }}</h1>
      <p>{{ $t('admin.dashboard.subtitle') }}</p>
    </div>

    <div class="content">
      <div class="management-grid">
        <div class="management-card" @click="navigateTo('/admin/permission')">
          <div class="card-icon">
            <i class="ivu-icon ivu-icon-ios-person"></i>
          </div>
          <div class="card-content">
            <h3>{{ $t('admin.dashboard.permissionTitle') }}</h3>
            <p>{{ $t('admin.dashboard.permissionDesc') }}</p>
            <ul>
              <li>{{ $t('admin.dashboard.userManage') }}</li>
              <li>{{ $t('admin.dashboard.roleManage') }}</li>
              <li>{{ $t('admin.dashboard.permManage') }}</li>
            </ul>
          </div>
        </div>

        <div class="management-card" @click="navigateTo('/admin/assign')">
          <div class="card-icon">
            <i class="ivu-icon ivu-icon-ios-link"></i>
          </div>
          <div class="card-content">
            <h3>{{ $t('admin.dashboard.assignTitle') }}</h3>
            <p>{{ $t('admin.dashboard.assignDesc') }}</p>
            <ul>
              <li>{{ $t('admin.dashboard.userRoleAssign') }}</li>
              <li>{{ $t('admin.dashboard.rolePermAssign') }}</li>
              <li>{{ $t('admin.dashboard.projectGroupManage') }}</li>
            </ul>
          </div>
        </div>

        <div class="management-card" @click="navigateTo('/admin/project')">
          <div class="card-icon">
            <i class="ivu-icon ivu-icon-ios-briefcase"></i>
          </div>
          <div class="card-content">
            <h3>{{ $t('admin.dashboard.projectTitle') }}</h3>
            <p>{{ $t('admin.dashboard.projectDesc') }}</p>
            <ul>
              <li>{{ $t('admin.dashboard.projManage') }}</li>
              <li>{{ $t('admin.dashboard.projGroupManage') }}</li>
              <li>{{ $t('admin.dashboard.projBelongManage') }}</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AdminDashboard',
  methods: {
    navigateTo(path) {
      this.$router.push(path)
    }
  }
}
</script>

<style scoped>
.admin-dashboard {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 20px;
}

.header {
  text-align: center;
  color: white;
  margin-bottom: 60px;
}

.header h1 {
  font-size: 2.5em;
  margin-bottom: 10px;
  font-weight: 300;
}

.header p {
  font-size: 1.1em;
  opacity: 0.9;
}

.content {
  max-width: 1200px;
  margin: 0 auto;
}

.management-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 30px;
  padding: 0 20px;
}

.management-card {
  background: white;
  border-radius: 15px;
  padding: 30px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.management-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  border-color: #667eea;
}

.card-icon {
  text-align: center;
  margin-bottom: 20px;
}

.card-icon i {
  font-size: 3em;
  color: #667eea;
}

.card-content h3 {
  color: #333;
  font-size: 1.5em;
  margin-bottom: 10px;
  text-align: center;
}

.card-content p {
  color: #666;
  text-align: center;
  margin-bottom: 20px;
  font-size: 1em;
}

.card-content ul {
  list-style: none;
  padding: 0;
}

.card-content li {
  color: #777;
  padding: 5px 0;
  border-left: 3px solid #667eea;
  padding-left: 15px;
  margin-bottom: 8px;
  background: #f8f9fa;
  border-radius: 0 5px 5px 0;
  font-size: 0.9em;
}

@media (max-width: 768px) {
  .management-grid {
    grid-template-columns: 1fr;
    padding: 0 10px;
  }

  .header h1 {
    font-size: 2em;
  }

  .management-card {
    padding: 25px;
  }
}
</style>