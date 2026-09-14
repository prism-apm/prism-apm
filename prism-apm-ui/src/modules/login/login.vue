<template>
  <div class="login-container">
    <div class="login-form">
      <h2 class="form-title">{{ $t('login.title') }}</h2>
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label for="username">{{ $t('login.username') }}:</label>
          <input id="username" v-model="loginForm.username" type="text" :placeholder="$t('login.usernamePlaceholder')"
            required />
        </div>
        <div class="form-group">
          <label for="password">{{ $t('login.password') }}:</label>
          <input id="password" v-model="loginForm.password" type="password"
            :placeholder="$t('login.passwordPlaceholder')" required />
        </div>
        <div class="form-group">
          <button type="submit" :disabled="loading">
            {{ loading ? $t('login.submitting') : $t('login.submit') }}
          </button>
        </div>
      </form>
      <div v-if="errorMessage" class="error-message">
        {{ errorMessage }}
      </div>
    </div>
  </div>
</template>

<script>
import { reqLogin } from '@/api/common/req'

export default {
  name: 'Login',
  data() {
    return {
      loginForm: {
        username: '',
        password: ''
      },
      loading: false,
      errorMessage: ''
    }
  },
  methods: {
    async handleLogin() {
      this.loading = true
      this.errorMessage = ''
      try {
        const response = await reqLogin({
          username: this.loginForm.username,
          password: this.loginForm.password
        })

        console.debug('response', response)
        if (response.success || response.status === 200 || response.code === 200) {
          if (response.token) {
            localStorage.setItem('apm_token', response.token)
          }
          const redirectPath = localStorage.getItem('redirectPath')
          localStorage.removeItem('redirectPath')
          if (redirectPath && redirectPath !== '#/login' && redirectPath !== '/login') {
            if (redirectPath.startsWith('#/')) {
              this.$router.push(redirectPath.substring(2))
            } else if (redirectPath.startsWith('/')) {
              this.$router.push(redirectPath)
            } else {
              this.$router.push('/overview/application')
            }
          } else {
            this.$router.push('/overview/application')
          }
        } else {
          this.errorMessage = response.message || this.$t('login.loginFailed')
        }
      } catch (error) {
        this.errorMessage = error.message || this.$t('login.loginFailedCheck')
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f5f5;
}

.login-form {
  background: white;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  width: 400px;
  max-width: 90%;
}

.form-title {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  color: #555;
  font-weight: 500;
}

.form-group input {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-group input:focus {
  outline: none;
  border-color: #007bff;
}

.form-group button {
  width: 100%;
  padding: 12px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.form-group button:hover:not(:disabled) {
  background-color: #0056b3;
}

.form-group button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.error-message {
  color: #dc3545;
  text-align: center;
  margin-top: 10px;
  padding: 10px;
  background-color: #f8d7da;
  border-radius: 4px;
}
</style>
