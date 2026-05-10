<script setup>
import { computed } from 'vue'
import { formatTicketTime, ticketStatusClass, ticketStatusText, ticketTypeText } from '../data/content'

const props = defineProps({
  currentUser: Object,
  tickets: Array,
  isLoading: Boolean,
  ticketStats: Object,
})

defineEmits(['refresh', 'go-submit', 'open-profile'])

const normalizedTickets = computed(() => {
  return props.tickets.map((ticket) => ({
    ...ticket,
    title: ticketTypeText(ticket.ticketType),
    statusText: ticketStatusText(ticket.status),
    statusClass: ticketStatusClass(ticket.status),
    formattedCreatedAt: formatTicketTime(ticket.createdAt),
  }))
})
</script>

<template>
  <div class="page active">
    <div class="content-shell">
      <div class="content-hero tickets-hero">
        <div class="content-hero-glow"></div>
        <div class="tickets-header tickets-header-hero">
          <div class="tickets-copy">
            <span class="section-tag">工单中心</span>
            <h2>我的工单</h2>
            <p>查看近期校园网问题处理进度，跟踪当前状态，并随时发起新的问题工单。</p>
          </div>
          <div class="hero-actions">
            <button class="btn btn-outline btn-sm tickets-hero-btn" @click="$emit('open-profile')">个人信息</button>
            <button class="btn btn-primary btn-sm tickets-hero-btn" @click="$emit('go-submit')">＋ 查看问题类型</button>
          </div>
        </div>
      </div>

      <div class="stats-inline">
        <div class="stat-card">
          <div class="stat-label">全部工单</div>
          <div class="stat-val">{{ ticketStats.total }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">待处理</div>
          <div class="stat-val">{{ ticketStats.pending }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">排队中</div>
          <div class="stat-val">{{ ticketStats.queue }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">已完成</div>
          <div class="stat-val">{{ ticketStats.done }}</div>
        </div>
      </div>

      <div class="content-panel tickets-panel">
        <div class="tickets-header" style="margin-bottom:16px">
          <div>
            <h2 style="font-size:18px;font-weight:700">工单列表</h2>
            <p style="color:var(--text2);margin-top:6px">当前账号：{{ currentUser?.name }} / {{ currentUser?.studentId }}</p>
          </div>
          <button class="btn btn-outline btn-sm" @click="$emit('refresh')">
            {{ isLoading ? '刷新中...' : '刷新' }}
          </button>
        </div>

        <div class="ticket-list">
          <div v-if="isLoading" class="ticket-feedback">
            <div class="ticket-feedback-icon">⏳</div>
            <div>
              <h3>正在加载工单</h3>
              <p>正在从后端同步你的最新工单状态，请稍等。</p>
            </div>
          </div>

          <div v-else-if="!normalizedTickets.length" class="ticket-feedback">
            <div class="ticket-feedback-icon">🧾</div>
            <div>
              <h3>还没有提交过工单</h3>
              <p>你提交的新工单会显示在这里，提交成功后可以回来查看处理进度。</p>
            </div>
          </div>

          <div
            v-else
            v-for="ticket in normalizedTickets"
            :key="ticket.ticketNo || ticket.id"
            class="ticket-item"
          >
            <div class="ticket-icon">🧾</div>
            <div class="ticket-info">
              <div class="ticket-title">{{ ticket.title }}</div>
              <div class="ticket-meta">
                <span># {{ ticket.ticketNo || '工单号待生成' }}</span>
                <span>{{ ticket.formattedCreatedAt }}</span>
              </div>
              <div v-if="ticket.resultMessage" class="ticket-note">{{ ticket.resultMessage }}</div>
            </div>
            <div class="ticket-right">
              <div class="status-badge" :class="ticket.statusClass">
                <div class="status-dot"></div>
                {{ ticket.statusText }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
