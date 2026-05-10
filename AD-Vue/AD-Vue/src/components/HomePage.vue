<script setup>
defineProps({
  homeForm: Object,
  isBinding: Boolean,
  latestSubmission: Object,
  connectionSteps: Array,
})

defineEmits(['submit-bind', 'go-submit', 'go-tickets'])
</script>

<template>
  <div class="page active">
    <div class="home-shell">
      <section class="home-section">
        <div class="home-hero-panel">
          <div class="home-hero-glow"></div>
          <div class="home-hero-copy">
            <span class="home-eyebrow">校园网服务平台</span>
            <h1>校园网服务，<span>一站解决</span></h1>
            <p>从首次绑定到校园网登录，首页直接给你最短路径。遇到无法上网、账号异常或密码问题时，也能快速进入自助处理流程。</p>
            <div class="hero-actions">
              <button class="btn btn-primary btn-large" @click="$emit('go-submit')">查看问题类型</button>
              <button class="btn btn-outline btn-large" @click="$emit('go-tickets')">查看我的工单</button>
            </div>
          </div>
        </div>
      </section>

      <section class="home-section">
        <div class="home-notice">
          <span class="home-notice-icon">ℹ️</span>
          <span>
            <strong style="color:var(--text)">使用前提：</strong>
            请先正确连接校园网，再使用学号和智慧校园相同密码登录。若页面提示账号、设备或套餐异常，可直接提交工单处理。
          </span>
        </div>
      </section>

      <section class="home-section">
        <div class="bind-panel">
          <div class="bind-copy">
            <span class="section-tag">新用户引导</span>
            <h2>新用户？绑定你的账号</h2>
            <p>首次使用校园网前，请先绑定学号和新办的电信校园卡手机号。提交成功后，这条工单会同步到“我的工单”里，后续由自动化程序继续处理。</p>
          </div>

          <div class="bind-form-wrap">
            <div class="bind-form">
              <input v-model="homeForm.studentId" class="form-input" placeholder="输入学号">
              <input v-model="homeForm.campusPhone" class="form-input" placeholder="输入新办的电信校园卡手机号">
              <button class="btn btn-primary bind-submit" :disabled="isBinding" @click="$emit('submit-bind')">
                {{ isBinding ? '提交中...' : '立即绑定' }}
              </button>
            </div>
            <div v-if="latestSubmission.ticketNo" class="bind-feedback">
              <span>✓</span>
              <span>最近提交工单：{{ latestSubmission.ticketNo }}</span>
            </div>
          </div>
        </div>
      </section>

      <section class="home-section steps-section">
        <div class="steps-header">
          <span class="section-tag">连接指南</span>
          <h2>校园网连接步骤</h2>
          <p>按照以下 3 步完成校园网连接。</p>
        </div>

        <div class="steps-grid">
          <div class="steps-line"></div>

          <article
            v-for="step in connectionSteps"
            :key="step.id"
            class="step-card"
            :class="{
              'step-card-emphasis': step.id === '第二步',
              'step-card-success': step.id === '第三步',
            }"
          >
            <div class="step-icon">{{ step.icon }}</div>
            <span class="step-badge">{{ step.badge }}</span>
            <h3>{{ step.title }}</h3>
            <p class="step-highlight">{{ step.highlight }}</p>
            <p>{{ step.description }}</p>
          </article>
        </div>
      </section>
    </div>
  </div>
</template>
