import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    title: import.meta.env.VITE_APP_TITLE || 'MyWeb',
  }),
})
