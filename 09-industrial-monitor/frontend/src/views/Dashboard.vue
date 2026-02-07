<template>
  <div class="dashboard">
    <div class="metrics">
      <div class="metric-card">
        <h3>当前温度</h3>
        <p class="value">{{ latestValues?.温度?.toFixed(1) || '--' }} ℃</p>
      </div>
      <div class="metric-card">
        <h3>当前压力</h3>
        <p class="value">{{ latestValues?.压力?.toFixed(2) || '--' }} MPa</p>
      </div>
    </div>
    <div ref="chartContainer" class="chart"></div>
  </div>
</template>

<script setup lang="ts">
import {ref, onMounted, onUnmounted, computed} from 'vue'
import * as echarts from 'echarts'
import { useDataStore } from '../stores/dataStore'

// 获取 store 并解构 state
const store = useDataStore()
const latestValues = computed(() => store.latestValues)

const chartContainer = ref<HTMLDivElement | null>(null)
let chart: echarts.ECharts | null = null
let timer: number | null = null

// 告警状态
interface Alert {
  id: string
  message: string
  value: string
}
const currentAlert = ref<Alert | null>(null)
const shownAlertIds = new Set<string>()

onMounted(() => {
  initChart()
  startPolling()
  console.log('🚀 初始化最新值:', store.latestValues)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (chart) chart.dispose()
})

function initChart() {
  if (!chartContainer.value) return
  chart = echarts.init(chartContainer.value)

  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['温度 (℃)', '压力 (MPa)'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: [] as string[]
    },
    yAxis: [
      { type: 'value', name: '温度 (℃)', min: 55, max: 70, position: 'left' },
      { type: 'value', name: '压力 (MPa)', min: 0.75, max: 0.9, position: 'right' }
    ],
    series: [
      {
        name: '温度 (℃)',
        type: 'line',
        yAxisIndex: 0,
        data: [] as number[],
        smooth: true,
        lineStyle: { width: 2, color: '#f87171' },
        symbol: 'none'
      },
      {
        name: '压力 (MPa)',
        type: 'line',
        yAxisIndex: 1,
        data: [] as number[],
        smooth: true,
        lineStyle: { width: 2, color: '#60a5fa' },
        symbol: 'none'
      }
    ]
  }
  chart.setOption(option)
}

// 获取最新告警
async function fetchAlerts() {
  try {
    const res = await fetch('/api/alerts/recent')
    if (res.ok) {
      const alerts: Alert[] = await res.json()
      // 查找未展示的新告警
      for (const alert of alerts) {
        if (!shownAlertIds.has(alert.id)) {
          shownAlertIds.add(alert.id)
          currentAlert.value = alert
          // 5秒后自动关闭
          setTimeout(() => {
            if (currentAlert.value?.id === alert.id) {
              closeAlert()
            }
          }, 5000)
          break // 只显示最新一条
        }
      }
    }
  } catch (err) {
    console.error('❌ 获取告警失败:', err)
  }
}

// 关闭告警
function closeAlert() {
  currentAlert.value = null
}

function startPolling() {
  const poll = () => {
    store.fetchLatest('mock-boiler')
    updateChart()
    fetchAlerts()
  }
  poll() // immediate
  timer = window.setInterval(poll, 1000)
}

function updateChart() {
  if (!chart) return

  const tempData = store.history.temperature.map(p => p.value)
  const pressData = store.history.pressure.map(p => p.value)
  const timeData = store.history.temperature.map(p => p.time)

  chart.setOption({
    xAxis: { data: timeData },
    series: [
      { data: tempData },
      { data: pressData }
    ]
  })
}
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.metrics {
  display: flex;
  gap: 20px;
  justify-content: center;
}
.metric-card {
  background: #1e293b;
  padding: 20px;
  border-radius: 8px;
  text-align: center;
  min-width: 180px;
}
.value {
  font-size: 2em;
  margin: 10px 0;
  color: #f87171;
}
.chart {
  height: 400px;
  background: #1e293b;
  border-radius: 8px;
  padding: 10px;
}
</style>
