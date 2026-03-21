/**
 * 用户管理 API 封装
 * 使用自定义 request 客户端，自动处理 token
 */
import { get, post, put, del } from '@/lib/request'
import type {
  AdminUserVO,
  CreateAdminUserDTO,
  UpdateAdminUserDTO,
} from '@/api/generated/model'

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageAdminUserVO {
  records: AdminUserVO[]
  total: number
  current: number
  size: number
}

// 分页获取用户列表
export async function listAdminUsers(params: {
  pageNum: number
  pageSize: number
  keyword?: string
}): Promise<ApiResponse<PageAdminUserVO>> {
  return get<PageAdminUserVO>('/admin-users', params)
}

// 获取用户详情
export async function getAdminUserDetail(id: number): Promise<ApiResponse<AdminUserVO>> {
  return get<AdminUserVO>(`/admin-users/${id}`)
}

// 创建用户
export async function createAdminUser(data: CreateAdminUserDTO): Promise<ApiResponse<null>> {
  return post<null>('/admin-users', data)
}

// 更新用户
export async function updateAdminUser(id: number, data: UpdateAdminUserDTO): Promise<ApiResponse<null>> {
  return put<null>(`/admin-users/${id}`, data)
}

// 删除用户
export async function deleteAdminUser(id: number): Promise<ApiResponse<null>> {
  return del<null>(`/admin-users/${id}`)
}
