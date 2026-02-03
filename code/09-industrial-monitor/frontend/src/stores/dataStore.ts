import { defineStore } from 'pinia'

export const useDataStore = defineStore('data', {
  state: () => ({
    latestValues: {} as Record<string, number>,
    history: {
      temperature: [] as { time: string; value: number }[],
      pressure: [] as { time: string; value: number }[]
    }
  }),

  actions: {
    updateData(payload: any) {
      this.latestValues = payload.values || {}
      const now = new Date().toLocaleTimeString('zh-CN', { hour12: false })

      // 更新历史（保留最近 60 点）
      if (payload.values?.温度 !== undefined) {
        this.history.temperature.push({ time: now, value: payload.values.温度 })
      }
      if (payload.values?.压力 !== undefined) {
        this.history.pressure.push({ time: now, value: payload.values.压力 })
      }

      if (this.history.temperature.length > 60) {
        this.history.temperature.shift()
        this.history.pressure.shift()
      }
    }
  }
})
