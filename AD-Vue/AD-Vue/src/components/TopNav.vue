<script setup>
defineProps({
  items: Array,
  activeView: String,
  currentUser: Object,
  ticketCount: Number,
  avatarMenuOpen: Boolean,
})

defineEmits([
  'select-view',
  'open-profile',
  'logout',
  'toggle-avatar-menu',
])
</script>

<template>
  <nav class="nav">
    <div class="nav-logo">
      <div class="dot"></div>
      校园网服务平台
    </div>

    <div class="nav-tabs">
      <button
        v-for="item in items"
        :key="item.id"
        class="nav-tab"
        :class="{ active: activeView === item.id }"
        @click="$emit('select-view', item.id)"
      >
        {{ item.title }}
      </button>
    </div>

    <div class="nav-right">
      <div class="badge">工单 {{ ticketCount }}</div>
      <div class="avatar-wrap">
        <button class="avatar avatar-button" @click="$emit('toggle-avatar-menu')">
          {{ currentUser?.name?.slice(0, 1) || '用' }}
        </button>
        <div class="avatar-dropdown" :class="{ open: avatarMenuOpen }">
          <div class="avatar-user">
            <div class="avatar-user-pic">{{ currentUser?.name?.slice(0, 1) || '用' }}</div>
            <div class="avatar-user-info">
              <div class="avatar-user-name">{{ currentUser?.name || '未登录用户' }}</div>
              <div class="avatar-user-id">{{ currentUser?.studentId || '-' }}</div>
            </div>
          </div>
          <button class="dropdown-item" @click="$emit('open-profile')">
            <span class="item-icon">👤</span>
            个人信息
          </button>
          <button class="dropdown-item danger" @click="$emit('logout')">
            <span class="item-icon">🚪</span>
            退出登录
          </button>
        </div>
      </div>
    </div>
  </nav>
</template>
