import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '@/store/auth'

const API_BASE_URL = '/api/admin'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

// 创建axios实例
const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器：添加token
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const { accessToken } = useAuthStore.getState()
    if (accessToken && config.headers) {
      config.headers.Authorization = `Bearer ${accessToken}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器：处理错误和自动退出
request.interceptors.response.use(
  (response) => {
    return response
  },
  async (error: AxiosError) => {
    // 401 或 403 未登录 都自动退出
    if (error.response?.status === 401 || error.response?.status === 403) {
      // 检查是否是真的未登录（message为"未登录"）
      const data = error.response.data as { code?: number; message?: string } | undefined
      if (error.response.status === 401 || data?.message === '未登录') {
        useAuthStore.getState().logout()
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

// 封装的GET请求
export async function get<T = any>(
  url: string,
  params?: Record<string, any>
): Promise<ApiResponse<T>> {
  const response = await request.get<ApiResponse<T>>(url, { params })
  return response.data
}

// 封装的POST请求
export async function post<T = any>(
  url: string,
  data?: any
): Promise<ApiResponse<T>> {
  const response = await request.post<ApiResponse<T>>(url, data)
  return response.data
}

// 封装的PUT请求
export async function put<T = any>(
  url: string,
  data?: any
): Promise<ApiResponse<T>> {
  const response = await request.put<ApiResponse<T>>(url, data)
  return response.data
}

// 封装的DELETE请求
export async function del<T = any>(
  url: string,
  data?: any
): Promise<ApiResponse<T>> {
  const response = await request.delete<ApiResponse<T>>(url, { data })
  return response.data
}

export { request, API_BASE_URL }
