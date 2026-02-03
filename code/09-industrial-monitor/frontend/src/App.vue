<template>
  <div id="app">
    <h1>工业监控大屏 - 模拟</h1>
    <Dashboard />
    <AlertToast
      v-for="alert in activeAlerts"
      :key="alert.id"
      :message="alert.message"
      :value="alert.value"
      @close="closeAlert(alert.id)"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import Dashboard from './views/Dashboard.vue'
import AlertToast from './components/AlertToast.vue'

interface AlertItem {
  id: number
  message: string
  value: string
}

const activeAlerts = ref<AlertItem[]>([])
let alertIdCounter = 0

// Poll alerts every 2 seconds
onMounted(() => {
  const pollAlerts = async () => {
    try {
      const res = await fetch('/api/alerts')
      const alerts = await res.json()
      // Show only new alerts
      alerts.forEach((a: any) => {
        const exists = activeAlerts.value.some(item => item.message === a.message && item.value === a.value)
        if (!exists) {
          activeAlerts.value.push({
            id: ++alertIdCounter,
            message: a.message,
            value: a.value
          })
        }
      })
    } catch (e) {
      console.warn('Failed to fetch alerts', e)
    }
    setTimeout(pollAlerts, 2000)
  }
  pollAlerts()
})

const closeAlert = (id: number) => {
  activeAlerts.value = activeAlerts.value.filter(a => a.id !== id)
}
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  background-color: #0f172a;
  color: white;
  padding: 20px;
}
h1 {
  text-align: center;
  margin-bottom: 30px;
}
</style>
