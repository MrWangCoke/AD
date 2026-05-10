<script setup>
defineProps({
  issueTypes: Array,
  selectedIssue: Object,
  supportEntries: Array,
  currentUser: Object,
  type3SmsContent: String,
  isSubmittingType3: Boolean,
})

defineEmits([
  'select-issue',
  'copy-support',
  'go-home',
  'go-auth',
  'update:type3SmsContent',
  'submit-type3',
])
</script>

<template>
  <div class="page active">
    <div class="content-shell">
      <div class="content-hero">
        <div class="content-hero-glow"></div>
        <div class="breadcrumb page-breadcrumb">
          <span @click="$emit('go-home')">首页</span>
          <span style="color:var(--border)">›</span>
          <span style="color:var(--text)">提交工单</span>
        </div>
        <div class="content-hero-copy">
          <span class="section-tag">自助处理</span>
          <h2>问题分类处理</h2>
          <p>内容逻辑和 App 保持一致。需要人工协助的问题在这里先看说明，涉及账号绑定的问题继续回首页提交。</p>
        </div>
      </div>

      <div class="content-panel">
        <div class="submit-layout">
          <section class="submit-section">
            <div class="submit-section-header">
              <div>
                <span class="section-tag submit-section-tag">查看问题</span>
                <h3>8 类常见问题入口</h3>
              </div>
              <p>点击左侧卡片切换右侧说明。这里先承接 App 的问题分类和处理文案，不直接改后端数据。</p>
            </div>

            <div class="submit-type-grid submit-type-grid-info">
              <button
                v-for="issue in issueTypes"
                :key="issue.id"
                type="button"
                class="type-card submit-type-card submit-type-card-info"
                :class="{ selected: selectedIssue?.id === issue.id }"
                @click="$emit('select-issue', issue.id)"
              >
                <div class="type-card-top">
                  <div class="type-card-icon">{{ issue.icon }}</div>
                  <span class="type-card-badge">
                    {{
                      issue.mode === 'bind'
                        ? '去绑定'
                        : issue.mode === 'reset'
                          ? '提交工单'
                          : '查看说明'
                    }}
                  </span>
                </div>
                <h4>{{ issue.name }}</h4>
                <p>{{ issue.description }}</p>
              </button>
            </div>
          </section>

          <section class="submit-section submit-section-info">
            <div class="submit-section-header">
              <div>
                <span class="section-tag submit-section-tag">处理说明</span>
                <h3>{{ selectedIssue?.name }}</h3>
              </div>
              <p>桌面端沿用移动端内容，但用更适合网页的双栏卡片方式展示。</p>
            </div>

            <div class="instruction-panel">
              <div class="instruction-panel-title">说明框</div>
              <div class="instruction-box">{{ selectedIssue?.method }}</div>
            </div>

            <div v-if="selectedIssue?.mode === 'reset'" class="type3-form">
              <label class="form-label">粘贴短信内容</label>
              <textarea
                :value="type3SmsContent"
                class="form-input type3-textarea"
                rows="5"
                placeholder="把 10000 号返回的完整短信粘贴到这里"
                @input="$emit('update:type3SmsContent', $event.target.value)"
              />
              <button
                class="btn btn-primary"
                :disabled="!type3SmsContent?.trim() || isSubmittingType3"
                @click="$emit('submit-type3')"
              >
                {{ isSubmittingType3 ? '提交中...' : '提交' }}
              </button>
            </div>

            <div class="form-actions" style="margin-top:20px">
              <button
                v-if="selectedIssue?.mode === 'bind'"
                class="btn btn-primary"
                @click="$emit('go-home')"
              >
                去首页绑定
              </button>
              <button
                v-else-if="!currentUser"
                class="btn btn-outline"
                @click="$emit('go-auth')"
              >
                登录后查看工单
              </button>
            </div>
          </section>

          <section class="submit-section submit-section-info">
            <div class="submit-section-header">
              <div>
                <span class="section-tag submit-section-tag">售后支持</span>
                <h3>人工处理入口</h3>
              </div>
              <p>当问题不在自助范围，或者已经按说明操作仍未解决时，可以直接复制联系方式。</p>
            </div>

            <div class="support-stack">
              <div v-for="entry in supportEntries" :key="entry.id" class="support-card">
                <div>
                  <div class="support-card__label">{{ entry.label }}</div>
                  <div class="support-card__value">{{ entry.value }}</div>
                </div>
                <button class="btn btn-outline btn-sm" @click="$emit('copy-support', entry)">复制</button>
              </div>
            </div>
          </section>
        </div>
      </div>
    </div>
  </div>
</template>
