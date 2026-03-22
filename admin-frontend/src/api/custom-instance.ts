/**
 * Orval 自定义 Axios 实例
 * orval 生成的代码会调用此文件导出的 customInstance 函数
 * 生成的 URL 已包含完整路径（如 /api/admin/roles），所以 baseURL 留空
 */
import Axios, { AxiosRequestConfig, AxiosError, InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '@/store/auth'

export const AXIOS_INSTANCE = Axios.create({
  baseURL: '',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器：注入 token
AXIOS_INSTANCE.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const { accessToken } = useAuthStore.getState()
    if (accessToken && config.headers) {
      config.headers.Authorization = `Bearer ${accessToken}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

// 响应拦截器：401/403 自动退出
AXIOS_INSTANCE.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      const data = error.response.data as { code?: number; message?: string } | undefined
      if (error.response.status === 401 || data?.message === '未登录') {
        useAuthStore.getState().logout()
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  },
)

/**
 * orval 生成代码调用的自定义请求函数
 * 去掉 responseType: 'blob'，返回 JSON 响应体
 */
export const customInstance = <T>(
  config: AxiosRequestConfig,
  options?: AxiosRequestConfig,
): Promise<T> => {
  // 去掉 orval 生成的 responseType: 'blob'
  const { responseType, ...restConfig } = config
  return AXIOS_INSTANCE({
    ...restConfig,
    ...options,
  }).then(({ data }) => data)
}

export type ErrorType<Error> = AxiosError<Error>
export type BodyType<BodyData> = BodyData
