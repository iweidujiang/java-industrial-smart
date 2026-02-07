import { defineStore } from 'pinia'

export interface DataPoint {
  time: string
  value: number
}

export const useDataStore = defineStore('data', {
  state: () => ({
    latestValues: {} as Record<string, number>,
    history: {
      temperature: [] as DataPoint[],
      pressure: [] as DataPoint[]
    }
  }),

  actions: {
    async fetchLatest(deviceId: string) {
      try {
        const res = await fetch(`/api/data/latest/${deviceId}`)
        if (!res.ok) return

        const data = await res.json()
        this.latestValues = data

        const now = new Date().toLocaleTimeString('zh-CN', { hour12: false, second: '2-digit' })

        this.history.temperature.push({ time: now, value: data.温度 })
        this.history.pressure.push({ time: now, value: data.压力 })

        if (this.history.temperature.length > 60) {
          this.history.temperature.shift()
          this.history.pressure.shift()
        }
      } catch (e) {
        console.error('Fetch data failed', e)
      }
    }
  }
})
