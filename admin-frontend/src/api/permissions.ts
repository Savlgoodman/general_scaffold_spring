/**
 * 权限管理 API 封装
 * 使用自定义 request 客户端，自动处理 token
 */
import { get, post } from '@/lib/request'
import type {
  PermissionBaseVO,
} from '@/api/generated/model'

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PagePermissionBaseVO {
  records: PermissionBaseVO[]
  total: number
  current: number
  size: number
}

// 分页获取权限列表
export async function listPermissions(params: {
  pageNum: number
  pageSize: number
  keyword?: string
}): Promise<ApiResponse<PagePermissionBaseVO>> {
  return get<PagePermissionBaseVO>('/permissions/list', params)
}

// 获取权限详情
export async function getPermissionDetail(id: number): Promise<ApiResponse<PermissionBaseVO>> {
  return get<PermissionBaseVO>(`/permissions/detail/${id}`)
}

// 创建权限
export async function createPermission(data: {
  name: string
  code?: string
  path?: string
  method?: string
  groupKey?: string
  groupName?: string
  isGroup?: boolean
  status?: number
  sort?: number
}): Promise<ApiResponse<null>> {
  return post<null>('/permissions/create', data)
}

// 更新权限
export async function updatePermission(
  id: number,
  data: {
    name?: string
    path?: string
    method?: string
    groupKey?: string
    groupName?: string
    isGroup?: boolean
    status?: number
    sort?: number
  }
): Promise<ApiResponse<null>> {
  return post<null>(`/permissions/update/${id}`, data)
}

// 删除权限
export async function deletePermission(id: number): Promise<ApiResponse<null>> {
  return post<null>(`/permissions/delete/${id}`)
}
