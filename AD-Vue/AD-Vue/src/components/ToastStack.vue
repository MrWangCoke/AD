<script setup>
defineProps({
  items: {
    type: Array,
    required: true,
  },
})

defineEmits(['dismiss'])
</script>

<template>
  <div class="toast-stack">
    <article
      v-for="item in items"
      :key="item.id"
      class="toast-item"
      :class="`toast-item--${item.tone}`"
    >
      <p>{{ item.message }}</p>
      <button type="button" @click="$emit('dismiss', item.id)">关闭</button>
    </article>
  </div>
</template>
