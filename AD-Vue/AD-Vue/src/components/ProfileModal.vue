<script setup>
defineProps({
  visible: Boolean,
  currentUser: Object,
  editForm: Object,
  isSaving: Boolean,
})

defineEmits(['close', 'save'])
</script>

<template>
  <div class="modal-overlay" :class="{ open: visible }" @click.self="$emit('close')">
    <div class="modal">
      <div class="modal-header">
        <h3>个人信息</h3>
        <button class="modal-close" @click="$emit('close')">×</button>
      </div>
      <div class="modal-body">
        <div class="avatar-picker">
          <div class="avatar-large">{{ currentUser?.name?.slice(0, 1) || '用' }}</div>
        </div>
        <div class="form-group">
          <label class="form-label">昵称</label>
          <input class="form-input" v-model="editForm.name" placeholder="设置你的昵称">
        </div>
        <div class="form-group">
          <label class="form-label">学号 / 工号</label>
          <input class="form-input" v-model="editForm.studentId" placeholder="学号或工号">
        </div>
        <div class="form-group">
          <label class="form-label">手机号</label>
          <input class="form-input" v-model="editForm.phone" placeholder="绑定手机号">
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-ghost btn-sm" @click="$emit('close')">关闭</button>
        <button class="btn btn-primary btn-sm" style="width:auto" @click="$emit('save')">
          {{ isSaving ? '保存中...' : '保存资料' }}
        </button>
      </div>
    </div>
  </div>
</template>
