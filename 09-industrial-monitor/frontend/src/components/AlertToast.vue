<template>
  <div class="alert-toast">
    <span>⚠️ {{ message }}：{{ value }}</span>
    <button @click="$emit('close')">×</button>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  message: string
  value: string
}>()
defineEmits(['close'])
</script>

<style scoped>
.alert-toast {
  position: fixed;
  top: 20px;
  right: 20px;
  background: #dc2626;
  color: white;
  padding: 12px 20px;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.3);
  display: flex;
  align-items: center;
  gap: 10px;
  z-index: 1000;
  animation: slideIn 0.3s ease-out;
}
.alert-toast button {
  background: none;
  border: none;
  color: white;
  font-size: 1.2em;
  cursor: pointer;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}
@keyframes slideIn {
  from { transform: translateX(100%); opacity: 0; }
  to { transform: translateX(0); opacity: 1; }
}
</style>
