/** 全局 UI 元数据（如站点标题），与构建时注入的 `VITE_*` 对齐。 */
import { defineStore } from 'pinia'

/** 应用全局状态管理 */
export const useAppStore = defineStore('app', {
  state: () => ({
    /** 站点标题，从环境变量读取 */
    title: import.meta.env.VITE_APP_TITLE || 'MyWeb',
  }),
})
