<script setup>
defineProps({
  messageItems: Array,
  supportEntries: Array,
})

defineEmits(['copy-support', 'go-submit'])
</script>

<template>
  <div class="page active">
    <div class="content-shell content-shell-compact">
      <div class="content-hero consult-hero">
        <div class="content-hero-glow"></div>
        <span class="section-tag">在线支持</span>
        <div class="consult-icon">💬</div>
        <h2>在线咨询</h2>
        <p>
          该功能暂时先保留为通知和人工联系入口。<br>
          如果是需要系统处理的问题，请优先查看问题类型并提交对应工单。
        </p>
        <div class="consult-chip">🚧 后续开发</div>
        <button class="btn btn-primary btn-large consult-btn" @click="$emit('go-submit')">
          查看问题类型
        </button>
      </div>

      <div class="content-panel">
        <div class="consult-grid">
          <div class="consult-column">
            <div class="submit-section-header">
              <div>
                <span class="section-tag submit-section-tag">通知面板</span>
                <h3>系统消息</h3>
              </div>
            </div>
            <div class="message-feed">
              <div v-for="message in messageItems" :key="message.id" class="message-feed-item">
                <strong>{{ message.title }}</strong>
                <p>{{ message.detail }}</p>
              </div>
            </div>
          </div>

          <div class="consult-column">
            <div class="submit-section-header">
              <div>
                <span class="section-tag submit-section-tag">人工支持</span>
                <h3>联系方式</h3>
              </div>
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
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
