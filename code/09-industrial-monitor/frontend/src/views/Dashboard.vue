<!-- src/views/Dashboard.vue -->
<template>
  <div class="dashboard" style="padding: 20px;">
    <h2>工业监控仪表盘（HTTP 轮询）</h2>
    <div ref="chartRef" style="width: 100%; height: 400px;"></div>
    <p v-if="loading">正在加载...</p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'

const chartRef = ref<HTMLDivElement | null>(null)
const loading = ref(true)
let chart: any = null
let pollInterval: number | null = null

// 初始化 ECharts
function initChart() {
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['温度 (℃)', '压力 (MPa)'] },
    xAxis: { type: 'category', data: [] },
    yAxis: [
      { name: '℃', min: 50, max: 70 },
      { name: 'MPa', min: 0.6, max: 1.0 }
    ],
    series: [
      { name: '温度 (℃)', type: 'line', yAxisIndex: 0, data: [], smooth: true },
      { name: '压力 (MPa)', type: 'line', yAxisIndex: 1, data: [], smooth: true }
    ]
  })
}

// 从后端获取最新数据
async function fetchLatestData() {
  try {
    const res = await fetch('http://localhost:8080/api/data/latest')
    if (!res.ok) throw new Error('HTTP error')
    const data = await res.json()

    loading.value = false

    // 更新图表
    const now = new Date().toLocaleTimeString()
    const option = chart.getOption()

    option.xAxis[0].data.push(now)
    option.series[0].data.push(data.temperature)
    option.series[1].data.push(data.pressure)

    if (option.xAxis[0].data.length > 20) {
      option.xAxis[0].data.shift()
      option.series[0].data.shift()
      option.series[1].data.shift()
    }

    chart.setOption(option)
  } catch (e) {
    console.error('❌ 获取数据失败:', e)
  }
}

// 启动定时轮询
function startPolling() {
  fetchLatestData() // 立即获取一次
  pollInterval = window.setInterval(fetchLatestData, 2000) // 每 2 秒
}

onMounted(() => {
  initChart()
  startPolling()
})

onUnmounted(() => {
  if (pollInterval) clearInterval(pollInterval)
  if (chart) chart.dispose()
})
</script>

<style scoped>
.dashboard {
  background: #f5f7fa;
}
</style>
