/**
 * 角色管理 API 封装
 * 使用自定义 request 客户端，自动处理 token
 */
import { get, post, put, del } from '@/lib/request'
import type {
  RoleBaseVO,
  CreateRoleDTO,
  UpdateRoleDTO,
  RoleAssignablePermissionVO,
  BatchRolePermissionDTO,
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

// 分页获取角色列表
export async function listRoles(params: {
  pageNum: number
  pageSize: number
  keyword?: string
}): Promise<ApiResponse<PageRoleBaseVO>> {
  return get<PageRoleBaseVO>('/roles', params)
}

// 获取角色详情
export async function getRoleDetail(id: number): Promise<ApiResponse<RoleBaseVO>> {
  return get<RoleBaseVO>(`/roles/${id}`)
}

// 创建角色
export async function createRole(data: CreateRoleDTO): Promise<ApiResponse<null>> {
  return post<null>('/roles', data)
}

// 更新角色
export async function updateRole(id: number, data: UpdateRoleDTO): Promise<ApiResponse<null>> {
  return put<null>(`/roles/${id}`, data)
}

// 删除角色
export async function deleteRole(id: number): Promise<ApiResponse<null>> {
  return del<null>(`/roles/${id}`)
}

// 获取角色可分配权限
export async function getRoleAssignablePermissions(id: number): Promise<ApiResponse<RoleAssignablePermissionVO>> {
  return get<RoleAssignablePermissionVO>(`/roles/${id}/permissions/assignable`)
}

// 分配组权限
export async function assignRoleGroupPermissions(
  id: number,
  data: BatchRolePermissionDTO
): Promise<ApiResponse<null>> {
  return post<null>(`/roles/${id}/permissions/groups`, data)
}

// 分配子权限
export async function assignRoleChildPermissions(
  id: number,
  data: BatchRolePermissionDTO
): Promise<ApiResponse<null>> {
  return post<null>(`/roles/${id}/permissions/children`, data)
}
