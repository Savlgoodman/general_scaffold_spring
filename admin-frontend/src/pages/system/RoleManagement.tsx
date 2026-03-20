import { useState, useEffect, useCallback } from 'react'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Badge } from '@/components/ui/badge'
import { useToast } from '@/hooks/use-toast'
import { Plus, Search, Pencil, Trash2, RefreshCw, Shield, Check } from 'lucide-react'
import { Checkbox } from '@/components/ui/checkbox'
import { apiClient } from '@/lib/api-client'
import type { RoleBaseVO, RoleAssignablePermissionVO, AssignableGroupVO } from '@/api/generated/model'

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

interface PageData<T> {
  records: T[]
  total: number
  current: number
  size: number
}

interface RoleFormData {
  name: string
  code: string
  description: string
  status: number
}

const initialFormData: RoleFormData = {
  name: '',
  code: '',
  description: '',
  status: 1,
}

export default function RoleManagement() {
  const { toast } = useToast()
  const [roles, setRoles] = useState<RoleBaseVO[]>([])
  const [loading, setLoading] = useState(false)
  const [total, setTotal] = useState(0)
  const [current, setCurrent] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [searchKeyword, setSearchKeyword] = useState('')

  const [dialogOpen, setDialogOpen] = useState(false)
  const [dialogTitle, setDialogTitle] = useState('创建角色')
  const [formData, setFormData] = useState<RoleFormData>(initialFormData)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [formLoading, setFormLoading] = useState(false)

  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [deletingLoading, setDeletingLoading] = useState(false)

  // 权限分配弹窗
  const [permDialogOpen, setPermDialogOpen] = useState(false)
  const [permDialogTitle, setPermDialogTitle] = useState('分配权限')
  const [currentRoleId, setCurrentRoleId] = useState<number | null>(null)
  const [assignablePermissions, setAssignablePermissions] = useState<AssignableGroupVO[]>([])
  const [permLoading, setPermLoading] = useState(false)
  const [selectedPermissions, setSelectedPermissions] = useState<Set<number>>(new Set())

  const fetchRoles = useCallback(async () => {
    setLoading(true)
    try {
      const params: Record<string, string | number> = {
        current,
        size: pageSize,
      }
      if (searchKeyword) {
        params.keyword = searchKeyword
      }

      const res = await apiClient.get<ApiResponse<PageData<RoleBaseVO>>>('/roles', { params })
      const { code, data } = res.data

      if (code === 200) {
        setRoles(data.records || [])
        setTotal(data.total || 0)
        setCurrent(data.current || 1)
        setPageSize(data.size || 10)
      }
    } catch {
      toast({ title: '获取角色列表失败', description: '请稍后重试', variant: 'destructive' })
    } finally {
      setLoading(false)
    }
  }, [current, pageSize, searchKeyword, toast])

  useEffect(() => {
    fetchRoles()
  }, [fetchRoles])

  const openCreateDialog = () => {
    setDialogTitle('创建角色')
    setFormData(initialFormData)
    setEditingId(null)
    setDialogOpen(true)
  }

  const openEditDialog = async (id: number) => {
    setDialogTitle('编辑角色')
    setEditingId(id)
    setFormLoading(true)
    setDialogOpen(true)

    try {
      const res = await apiClient.get<ApiResponse<RoleBaseVO>>(`/roles/${id}`)
      const { code, data } = res.data

      if (code === 200 && data) {
        setFormData({
          name: data.name || '',
          code: data.code || '',
          description: data.description || '',
          status: data.status || 1,
        })
      }
    } catch {
      toast({ title: '获取角色详情失败', variant: 'destructive' })
      setDialogOpen(false)
    } finally {
      setFormLoading(false)
    }
  }

  const handleSubmit = async () => {
    if (!formData.name) {
      toast({ title: '请填写角色名称', variant: 'destructive' })
      return
    }
    if (!formData.code && !editingId) {
      toast({ title: '请填写角色编码', variant: 'destructive' })
      return
    }

    setFormLoading(true)
    try {
      if (editingId) {
        const res = await apiClient.put<ApiResponse<null>>(`/roles/${editingId}`, {
          name: formData.name,
          description: formData.description,
          status: formData.status,
        })
        const { code, message } = res.data

        if (code === 200) {
          toast({ title: '更新角色成功' })
          setDialogOpen(false)
          fetchRoles()
        } else {
          toast({ title: '更新角色失败', description: message, variant: 'destructive' })
        }
      } else {
        const res = await apiClient.post<ApiResponse<null>>('/roles', {
          name: formData.name,
          code: formData.code,
          description: formData.description,
          status: formData.status,
        })
        const { code, message } = res.data

        if (code === 200) {
          toast({ title: '创建角色成功' })
          setDialogOpen(false)
          fetchRoles()
        } else {
          toast({ title: '创建角色失败', description: message, variant: 'destructive' })
        }
      }
    } catch {
      toast({ title: '操作失败', variant: 'destructive' })
    } finally {
      setFormLoading(false)
    }
  }

  const openDeleteDialog = (id: number) => {
    setDeletingId(id)
    setDeleteDialogOpen(true)
  }

  const handleDelete = async () => {
    if (!deletingId) return

    setDeletingLoading(true)
    try {
      const res = await apiClient.delete<ApiResponse<null>>(`/roles/${deletingId}`)
      const { code, message } = res.data

      if (code === 200) {
        toast({ title: '删除角色成功' })
        setDeleteDialogOpen(false)
        fetchRoles()
      } else {
        toast({ title: '删除角色失败', description: message, variant: 'destructive' })
      }
    } catch {
      toast({ title: '删除失败', variant: 'destructive' })
    } finally {
      setDeletingLoading(false)
    }
  }

  // 打开权限分配弹窗
  const openPermDialog = async (role: RoleBaseVO) => {
    if (!role.id) return

    setCurrentRoleId(role.id)
    setPermDialogTitle(`分配权限 - ${role.name}`)
    setPermDialogOpen(true)
    setPermLoading(true)
    setSelectedPermissions(new Set())

    try {
      // 获取可分配权限
      const assignableRes = await apiClient.get<ApiResponse<RoleAssignablePermissionVO>>(`/roles/${role.id}/permissions/assignable`)

      if (assignableRes.data.code === 200) {
        setAssignablePermissions(assignableRes.data.data.groups || [])

        // 从可分配权限构建选中集合（已分配的）
        const assignedIds = new Set<number>()
        assignableRes.data.data.groups?.forEach(group => {
          if (group.groupPermission?.isAssigned && group.groupPermission.id) {
            assignedIds.add(group.groupPermission.id)
          }
          group.unassignedChildren?.forEach(child => {
            if (child.isAssigned && child.id) {
              assignedIds.add(child.id)
            }
          })
        })
        setSelectedPermissions(assignedIds)
      }
    } catch {
      toast({ title: '获取权限信息失败', variant: 'destructive' })
    } finally {
      setPermLoading(false)
    }
  }

  // 切换权限选中状态
  const togglePermission = (permId: number) => {
    setSelectedPermissions(prev => {
      const newSet = new Set(prev)
      if (newSet.has(permId)) {
        newSet.delete(permId)
      } else {
        newSet.add(permId)
      }
      return newSet
    })
  }

  // 全选/取消全选分组下的子权限
  const toggleGroupChildren = (group: AssignableGroupVO, checked: boolean) => {
    setSelectedPermissions(prev => {
      const newSet = new Set(prev)
      if (checked) {
        // 选中组权限
        if (group.groupPermission?.id) {
          newSet.add(group.groupPermission.id)
        }
        // 选中所有子权限
        group.unassignedChildren?.forEach(child => {
          if (child.id) newSet.add(child.id)
        })
      } else {
        // 取消组权限
        if (group.groupPermission?.id) {
          newSet.delete(group.groupPermission.id)
        }
        // 取消所有子权限
        group.unassignedChildren?.forEach(child => {
          if (child.id) newSet.delete(child.id)
        })
      }
      return newSet
    })
  }

  // 检查分组是否全选
  const isGroupFullySelected = (group: AssignableGroupVO): boolean => {
    if (!group.groupPermission?.id) return false
    const childIds = group.unassignedChildren?.map(c => c.id).filter((id): id is number => id !== undefined) || []
    const allIds: number[] = [group.groupPermission.id, ...childIds]
    return allIds.every(id => selectedPermissions.has(id))
  }

  // 检查分组是否部分选中
  const isGroupPartiallySelected = (group: AssignableGroupVO): boolean => {
    if (isGroupFullySelected(group)) return false
    if (!group.groupPermission?.id) return false
    const childIds = group.unassignedChildren?.map(c => c.id).filter((id): id is number => id !== undefined) || []
    const allIds: number[] = [group.groupPermission.id, ...childIds]
    return allIds.some(id => selectedPermissions.has(id))
  }

  // 保存权限分配
  const savePermissions = async () => {
    if (!currentRoleId) return

    setPermLoading(true)
    try {
      // 构建需要提交的权限列表
      const groupPerms: { permissionId: number; effect: string; priority: number }[] = []
      const childPerms: { permissionId: number; effect: string; priority: number }[] = []

      assignablePermissions.forEach(group => {
        const groupId = group.groupPermission?.id
        const childIds = group.unassignedChildren?.map(c => c.id).filter((id): id is number => id !== undefined) || []

        // 检查组权限
        if (groupId && selectedPermissions.has(groupId)) {
          groupPerms.push({
            permissionId: groupId,
            effect: 'GRANT',
            priority: 0,
          })
        }

        // 检查子权限
        childIds.forEach(childId => {
          if (selectedPermissions.has(childId)) {
            childPerms.push({
              permissionId: childId,
              effect: 'GRANT',
              priority: 0,
            })
          }
        })
      })

      // 分配组权限
      if (groupPerms.length > 0) {
        await apiClient.post(`/roles/${currentRoleId}/permissions/groups`, {
          permissions: groupPerms,
        })
      }

      // 分配子权限
      if (childPerms.length > 0) {
        await apiClient.post(`/roles/${currentRoleId}/permissions/children`, {
          permissions: childPerms,
        })
      }

      toast({ title: '权限分配成功' })
      setPermDialogOpen(false)
      fetchRoles()
    } catch {
      toast({ title: '权限分配失败', variant: 'destructive' })
    } finally {
      setPermLoading(false)
    }
  }

  const totalPages = Math.ceil(total / pageSize)

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">角色管理</h1>
          <p className="text-muted-foreground mt-1">管理系统角色及权限配置</p>
        </div>
        <Button onClick={openCreateDialog}>
          <Plus className="w-4 h-4 mr-2" />
          创建角色
        </Button>
      </div>

      <Card>
        <CardHeader className="pb-4">
          <div className="flex items-center justify-between gap-4">
            <CardTitle className="text-lg">角色列表</CardTitle>
            <div className="flex items-center gap-3">
              <div className="relative w-64">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                <Input
                  placeholder="搜索角色..."
                  value={searchKeyword}
                  onChange={(e) => setSearchKeyword(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && fetchRoles()}
                  className="pl-9 h-9"
                />
              </div>
              <Button variant="outline" size="sm" onClick={fetchRoles} disabled={loading}>
                <RefreshCw className={`w-4 h-4 mr-1.5 ${loading ? 'animate-spin' : ''}`} />
                刷新
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent className="p-0">
          <div className="border rounded-lg overflow-hidden mx-4 mb-4">
            <Table>
              <TableHeader>
                <TableRow className="bg-muted/50">
                  <TableHead className="w-14 text-center h-10">ID</TableHead>
                  <TableHead className="text-center">角色名称</TableHead>
                  <TableHead className="text-center">角色编码</TableHead>
                  <TableHead className="text-center">描述</TableHead>
                  <TableHead className="w-20 text-center">状态</TableHead>
                  <TableHead className="w-24 text-center">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center py-12 text-muted-foreground">
                      加载中...
                    </TableCell>
                  </TableRow>
                ) : roles.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center py-12 text-muted-foreground">
                      暂无数据
                    </TableCell>
                  </TableRow>
                ) : (
                  roles.map((role, index) => (
                    <TableRow key={role.id} className={index % 2 === 0 ? 'bg-background' : 'bg-muted/20'}>
                      <TableCell className="text-center font-mono text-sm py-3">{role.id}</TableCell>
                      <TableCell className="text-center font-medium py-3">{role.name}</TableCell>
                      <TableCell className="text-center font-mono text-sm py-3">{role.code}</TableCell>
                      <TableCell className="text-center py-3">{role.description || '-'}</TableCell>
                      <TableCell className="text-center py-3">
                        {role.status === 1 ? (
                          <Badge variant="default" className="bg-emerald-500 text-xs">正常</Badge>
                        ) : (
                          <Badge variant="destructive" className="text-xs">禁用</Badge>
                        )}
                      </TableCell>
                      <TableCell className="text-center py-3">
                        <div className="flex justify-center gap-1">
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8"
                            title="分配权限"
                            onClick={() => openPermDialog(role)}
                          >
                            <Shield className="w-3.5 h-3.5" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8"
                            onClick={() => role.id && openEditDialog(role.id)}
                          >
                            <Pencil className="w-3.5 h-3.5" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8"
                            onClick={() => role.id && openDeleteDialog(role.id)}
                          >
                            <Trash2 className="w-3.5 h-3.5 text-destructive" />
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>

          {total > 0 && (
            <div className="flex items-center justify-between px-4 pb-4">
              <div className="text-sm text-muted-foreground">
                共 {total} 条，第 {current}/{totalPages} 页
              </div>
              <div className="flex items-center gap-2">
                <Select value={String(pageSize)} onValueChange={(v) => { setPageSize(Number(v)); setCurrent(1); }}>
                  <SelectTrigger className="w-28 h-8">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="10">10条/页</SelectItem>
                    <SelectItem value="20">20条/页</SelectItem>
                    <SelectItem value="50">50条/页</SelectItem>
                  </SelectContent>
                </Select>
                <div className="flex items-center gap-1">
                  <Button variant="outline" size="sm" className="h-8 w-16" onClick={() => setCurrent(c => c - 1)} disabled={current <= 1}>
                    上一页
                  </Button>
                  <span className="text-sm px-2">{current} / {totalPages}</span>
                  <Button variant="outline" size="sm" className="h-8 w-16" onClick={() => setCurrent(c => c + 1)} disabled={current >= totalPages}>
                    下一页
                  </Button>
                </div>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* 创建/编辑对话框 */}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{dialogTitle}</DialogTitle>
            <DialogDescription>
              {editingId ? '修改角色信息' : '填写角色信息创建新角色'}
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid gap-2">
              <Label htmlFor="name">
                角色名称 <span className="text-destructive">*</span>
              </Label>
              <Input
                id="name"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                placeholder="请输入角色名称"
              />
            </div>

            <div className="grid gap-2">
              <Label htmlFor="code">
                角色编码 <span className="text-destructive">*</span>
              </Label>
              <Input
                id="code"
                value={formData.code}
                onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                disabled={!!editingId}
                placeholder="请输入角色编码，如 SYS_ADMIN"
              />
            </div>

            <div className="grid gap-2">
              <Label htmlFor="description">描述</Label>
              <Input
                id="description"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                placeholder="请输入角色描述"
              />
            </div>

            <div className="flex items-center gap-2">
              <Label htmlFor="status">启用状态</Label>
              <Switch
                id="status"
                checked={formData.status === 1}
                onCheckedChange={(checked) => setFormData({ ...formData, status: checked ? 1 : 0 })}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDialogOpen(false)}>取消</Button>
            <Button onClick={handleSubmit} disabled={formLoading}>
              {formLoading ? '提交中...' : editingId ? '保存' : '创建'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 删除确认对话框 */}
      <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>确认删除</DialogTitle>
            <DialogDescription>删除后无法恢复，确定要删除该角色吗？</DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteDialogOpen(false)}>取消</Button>
            <Button variant="destructive" onClick={handleDelete} disabled={deletingLoading}>
              {deletingLoading ? '删除中...' : '确认删除'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 权限分配对话框 */}
      <Dialog open={permDialogOpen} onOpenChange={setPermDialogOpen}>
        <DialogContent className="sm:max-w-2xl max-h-[80vh] overflow-hidden flex flex-col">
          <DialogHeader>
            <DialogTitle>{permDialogTitle}</DialogTitle>
            <DialogDescription>勾选需要分配给该角色的权限</DialogDescription>
          </DialogHeader>

          {permLoading ? (
            <div className="flex items-center justify-center py-8">
              <RefreshCw className="w-6 h-6 animate-spin text-muted-foreground" />
            </div>
          ) : (
            <div className="flex-1 overflow-y-auto py-4">
              <div className="space-y-4">
                {assignablePermissions.map((group) => {
                  const isGroupSelected = isGroupFullySelected(group)
                  const isPartial = isGroupPartiallySelected(group)

                  return (
                    <div key={group.groupKey} className="border rounded-lg p-4">
                      <div className="flex items-center gap-3 mb-3">
                        <Checkbox
                          checked={isGroupSelected}
                          ref={(el) => {
                            if (el) {
                              (el as HTMLButtonElement).dataset.indeterminate = String(isPartial)
                            }
                          }}
                          onCheckedChange={(checked) => toggleGroupChildren(group, !!checked)}
                          className="h-5 w-5"
                        />
                        <div className="flex-1">
                          <div className="font-medium">{group.groupName}</div>
                          <div className="text-sm text-muted-foreground">
                            {group.groupPermission?.path} {group.groupPermission?.method}
                          </div>
                        </div>
                        {group.groupPermission?.isAssigned && (
                          <Badge variant="default" className="bg-emerald-500 text-xs">已分配</Badge>
                        )}
                      </div>

                      <div className="pl-8 space-y-2">
                        {group.unassignedChildren?.map((child) => (
                          <div key={child.id} className="flex items-center gap-3">
                            <Checkbox
                              checked={selectedPermissions.has(child.id!)}
                              onCheckedChange={() => child.id && togglePermission(child.id)}
                              className="h-4 w-4"
                            />
                            <div className="flex-1 text-sm">
                              <span className="font-medium">{child.name}</span>
                              <span className="text-muted-foreground ml-2">
                                {child.method} {child.path}
                              </span>
                            </div>
                            {child.isAssigned && (
                              <Check className="w-4 h-4 text-emerald-500" />
                            )}
                          </div>
                        ))}
                      </div>
                    </div>
                  )
                })}

                {assignablePermissions.length === 0 && (
                  <div className="text-center py-8 text-muted-foreground">
                    暂无可分配的权限
                  </div>
                )}
              </div>
            </div>
          )}

          <DialogFooter>
            <Button variant="outline" onClick={() => setPermDialogOpen(false)}>取消</Button>
            <Button onClick={savePermissions} disabled={permLoading}>
              {permLoading ? '保存中...' : '保存'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
