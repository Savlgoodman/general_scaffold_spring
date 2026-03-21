/**
 * 用户权限管理 API 封装
 * 使用自定义 request 客户端，自动处理 token
 */
import { get, post } from '@/lib/request'
import type {
  AssignRolesDTO,
  BatchPermissionOverrideDTO,
  UserGroupPermissionVO,
  UserAvailablePermissionVO,
  RoleBaseVO,
} from '@/api/generated/model'

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageRoleBaseVO {
  records: RoleBaseVO[]
  total: number
  current: number
  size: number
}

// 获取用户角色列表
export async function getUserRoles(userId: number): Promise<ApiResponse<{ roleIds: number[] }>> {
  return get<{ roleIds: number[] }>(`/admin-users/${userId}/roles`)
}

// 分配用户角色
export async function assignUserRoles(
  userId: number,
  data: AssignRolesDTO
): Promise<ApiResponse<null>> {
  return post<null>(`/admin-users/${userId}/roles`, data)
}

// 获取用户有效权限（含分组）
export async function getUserEffectivePermissions(
  userId: number
): Promise<ApiResponse<{ groups: UserGroupPermissionVO[] }>> {
  return get<{ groups: UserGroupPermissionVO[] }>(`/admin-users/${userId}/permissions`)
}

// 获取用户可用权限（用于添加覆盖）
export async function getUserAvailablePermissions(
  userId: number
): Promise<ApiResponse<{ groups: UserAvailablePermissionVO[] }>> {
  return get<{ groups: UserAvailablePermissionVO[] }>(`/admin-users/${userId}/permissions/available`)
}

// 批量设置用户权限覆盖
export async function setUserPermissionOverrides(
  userId: number,
  data: BatchPermissionOverrideDTO
): Promise<ApiResponse<null>> {
  return post<null>(`/admin-users/${userId}/permission-overrides/batch`, data)
}

// 获取所有角色（用于用户角色分配）
export async function listRoles(params: {
  pageNum: number
  pageSize: number
  keyword?: string
}): Promise<ApiResponse<PageRoleBaseVO>> {
  return get<PageRoleBaseVO>('/roles', params)
}
