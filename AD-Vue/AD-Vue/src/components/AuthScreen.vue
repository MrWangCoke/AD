<script setup>
defineProps({
  mode: String,
  formState: Object,
  isLoading: Boolean,
})

defineEmits(['set-mode', 'login', 'register'])
</script>

<template>
  <div class="auth-wrap">
    <div class="auth-shell">
      <section class="auth-showcase">
        <div class="auth-showcase-glow"></div>
        <span class="section-tag">AHUT Campus Network</span>
        <h1>校园网服务平台</h1>
        <p>统一处理校园网账号异常、登录失败、设备解绑和重置问题，让常见故障在几分钟内完成自助处理。</p>

        <div class="auth-feature-list">
          <div class="auth-feature-card">
            <div class="auth-feature-icon">⚡</div>
            <div>
              <strong>快速提交</strong>
              <p>登录后直接发起工单，无需重复进入运营商后台。</p>
            </div>
          </div>
          <div class="auth-feature-card">
            <div class="auth-feature-icon">📡</div>
            <div>
              <strong>统一入口</strong>
              <p>账号绑定、校园网登录和问题处理都在同一平台完成。</p>
            </div>
          </div>
          <div class="auth-feature-card">
            <div class="auth-feature-icon">🔒</div>
            <div>
              <strong>安全验证</strong>
              <p>通过绑定手机号流程保障账号操作安全。</p>
            </div>
          </div>
        </div>
      </section>

      <div class="auth-card" v-if="mode === 'login'">
        <div class="auth-card-head">
          <span class="auth-card-tag">登录</span>
          <div class="auth-title">登录账号</div>
          <div class="auth-sub">使用手机号登录，进入校园网服务平台处理问题。</div>
        </div>

        <div class="form-group">
          <label class="form-label">手机号</label>
          <input class="form-input" v-model="formState.loginPhone" placeholder="请输入手机号">
        </div>
        <div class="form-group">
          <label class="form-label">密码</label>
          <input class="form-input" type="password" v-model="formState.loginPassword" placeholder="请输入密码">
        </div>

        <button class="btn btn-primary auth-submit" @click="$emit('login')">
          {{ isLoading ? '登录中...' : '登录' }}
        </button>
        <div class="auth-switch">还没有账号？<span class="link" @click="$emit('set-mode', 'register')">立即注册</span></div>
      </div>

      <div class="auth-card" v-else>
        <div class="auth-card-head">
          <span class="auth-card-tag">注册</span>
          <div class="auth-title">创建账号</div>
          <div class="auth-sub">注册后即可提交校园网问题工单并查看处理进度。</div>
        </div>

        <div class="form-group">
          <label class="form-label">手机号</label>
          <input class="form-input" v-model="formState.registerPhone" placeholder="11位手机号">
        </div>
        <div class="form-group">
          <label class="form-label">密码</label>
          <input class="form-input" type="password" v-model="formState.registerPassword" placeholder="请输入密码">
        </div>
        <div class="form-group">
          <label class="form-label">确认密码</label>
          <input class="form-input" type="password" v-model="formState.registerConfirmPassword" placeholder="再次输入密码">
        </div>

        <button class="btn btn-primary auth-submit" @click="$emit('register')">
          {{ isLoading ? '注册中...' : '注册' }}
        </button>
        <div class="auth-switch">已有账号？<span class="link" @click="$emit('set-mode', 'login')">返回登录</span></div>
      </div>
    </div>
  </div>
</template>
