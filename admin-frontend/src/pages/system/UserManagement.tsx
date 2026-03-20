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
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { useToast } from '@/hooks/use-toast'
import { Plus, Search, Pencil, Trash2, RefreshCw, UserCog, Shield, Check, X } from 'lucide-react'
import { Checkbox } from '@/components/ui/checkbox'
import { apiClient } from '@/lib/api-client'
import type {
  AdminUserVO,
  CreateAdminUserDTO,
  UpdateAdminUserDTO,
  RoleBaseVO,
  UserPermissionVO,
  UserAvailablePermissionVO,
} from '@/api/generated/model'

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

interface UserFormData {
  username: string
  password: string
  nickname: string
  email: string
  phone: string
  avatar: string
  isSuperuser: number
  status: number
}

const initialFormData: UserFormData = {
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  avatar: '',
  isSuperuser: 0,
  status: 1,
}

export default function UserManagement() {
  const { toast } = useToast()
  const [users, setUsers] = useState<AdminUserVO[]>([])
  const [loading, setLoading] = useState(false)
  const [total, setTotal] = useState(0)
  const [current, setCurrent] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [searchKeyword, setSearchKeyword] = useState('')

  const [dialogOpen, setDialogOpen] = useState(false)
  const [dialogTitle, setDialogTitle] = useState('创建用户')
  const [formData, setFormData] = useState<UserFormData>(initialFormData)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [formLoading, setFormLoading] = useState(false)

  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [deletingLoading, setDeletingLoading] = useState(false)

  // 角色分配弹窗
  const [roleDialogOpen, setRoleDialogOpen] = useState(false)
  const [currentUserId, setCurrentUserId] = useState<number | null>(null)
  const [currentUsername, setCurrentUsername] = useState('')
  const [allRoles, setAllRoles] = useState<RoleBaseVO[]>([])
  const [userRoles, setUserRoles] = useState<Set<number>>(new Set())
  const [roleLoading, setRoleLoading] = useState(false)

  // 权限覆盖弹窗
  const [permDialogOpen, setPermDialogOpen] = useState(false)
  const [permUserId, setPermUserId] = useState<number | null>(null)
  const [permUsername, setPermUsername] = useState('')
  const [userEffectivePerms, setUserEffectivePerms] = useState<UserPermissionVO | null>(null)
  const [userAvailablePerms, setUserAvailablePerms] = useState<UserAvailablePermissionVO | null>(null)
  const [permLoading, setPermLoading] = useState(false)
  const [selectedOverridePerms, setSelectedOverridePerms] = useState<Set<number>>(new Set())

  const fetchUsers = useCallback(async () => {
    setLoading(true)
    try {
      const params: Record<string, string | number> = {
        current,
        size: pageSize,
      }
      if (searchKeyword) {
        params.keyword = searchKeyword
      }

      const res = await apiClient.get<ApiResponse<PageData<AdminUserVO>>>('/admin-users', { params })
      const { code, data } = res.data

      if (code === 200) {
        setUsers(data.records || [])
        setTotal(data.total || 0)
        setCurrent(data.current || 1)
        setPageSize(data.size || 10)
      }
    } catch {
      toast({ title: '获取用户列表失败', description: '请稍后重试', variant: 'destructive' })
    } finally {
      setLoading(false)
    }
  }, [current, pageSize, searchKeyword, toast])

  useEffect(() => {
    fetchUsers()
  }, [fetchUsers])

  const openCreateDialog = () => {
    setDialogTitle('创建用户')
    setFormData(initialFormData)
    setEditingId(null)
    setDialogOpen(true)
  }

  const openEditDialog = async (id: number) => {
    setDialogTitle('编辑用户')
    setEditingId(id)
    setFormLoading(true)
    setDialogOpen(true)

    try {
      const res = await apiClient.get<ApiResponse<AdminUserVO>>(`/admin-users/${id}`)
      const { code, data } = res.data

      if (code === 200 && data) {
        setFormData({
          username: data.username || '',
          password: '',
          nickname: data.nickname || '',
          email: data.email || '',
          phone: data.phone || '',
          avatar: data.avatar || '',
          isSuperuser: data.isSuperuser || 0,
          status: data.status || 1,
        })
      }
    } catch {
      toast({ title: '获取用户详情失败', variant: 'destructive' })
      setDialogOpen(false)
    } finally {
      setFormLoading(false)
    }
  }

  const handleSubmit = async () => {
    if (!formData.username && !editingId) {
      toast({ title: '请填写用户名', variant: 'destructive' })
      return
    }
    if (!editingId && !formData.password) {
      toast({ title: '请填写密码', variant: 'destructive' })
      return
    }

    setFormLoading(true)
    try {
      if (editingId) {
        const updateData: UpdateAdminUserDTO = {}
        if (formData.nickname) updateData.nickname = formData.nickname
        if (formData.email) updateData.email = formData.email
        if (formData.phone) updateData.phone = formData.phone
        if (formData.avatar) updateData.avatar = formData.avatar
        if (formData.isSuperuser !== undefined) updateData.isSuperuser = formData.isSuperuser
        if (formData.status !== undefined) updateData.status = formData.status
        if (formData.password) updateData.password = formData.password

        const res = await apiClient.put<ApiResponse<null>>(`/admin-users/${editingId}`, updateData)
        const { code, message } = res.data

        if (code === 200) {
          toast({ title: '更新用户成功' })
          setDialogOpen(false)
          fetchUsers()
        } else {
          toast({ title: '更新用户失败', description: message, variant: 'destructive' })
        }
      } else {
        const createData: CreateAdminUserDTO = {
          username: formData.username,
          password: formData.password,
          nickname: formData.nickname || undefined,
          email: formData.email || undefined,
          phone: formData.phone || undefined,
          avatar: formData.avatar || undefined,
          isSuperuser: formData.isSuperuser,
          status: formData.status,
        }

        const res = await apiClient.post<ApiResponse<null>>('/admin-users', createData)
        const { code, message } = res.data

        if (code === 200) {
          toast({ title: '创建用户成功' })
          setDialogOpen(false)
          fetchUsers()
        } else {
          toast({ title: '创建用户失败', description: message, variant: 'destructive' })
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
      const res = await apiClient.delete<ApiResponse<null>>(`/admin-users/${deletingId}`)
      const { code, message } = res.data

      if (code === 200) {
        toast({ title: '删除用户成功' })
        setDeleteDialogOpen(false)
        fetchUsers()
      } else {
        toast({ title: '删除用户失败', description: message, variant: 'destructive' })
      }
    } catch {
      toast({ title: '删除失败', variant: 'destructive' })
    } finally {
      setDeletingLoading(false)
    }
  }

  // 打开角色分配弹窗
  const openRoleDialog = async (user: AdminUserVO) => {
    if (!user.id) return

    setCurrentUserId(user.id)
    setCurrentUsername(user.username || '')
    setRoleDialogOpen(true)
    setRoleLoading(true)

    try {
      // 获取所有角色
      const rolesRes = await apiClient.get<ApiResponse<PageData<RoleBaseVO>>>('/roles', { params: { current: 1, size: 100 } })
      if (rolesRes.data.code === 200) {
        setAllRoles(rolesRes.data.data.records || [])
      }

      // 获取用户已有角色
      const userRolesRes = await apiClient.get<ApiResponse<{ roles: RoleBaseVO[] }>>(`/admin-users/${user.id}/roles`)
      if (userRolesRes.data.code === 200) {
        const roleIds = new Set(userRolesRes.data.data.roles?.map(r => r.id).filter((id): id is number => id !== undefined) || [])
        setUserRoles(roleIds)
      }
    } catch {
      toast({ title: '获取角色信息失败', variant: 'destructive' })
    } finally {
      setRoleLoading(false)
    }
  }

  // 切换角色选中
  const toggleRole = (roleId: number) => {
    setUserRoles(prev => {
      const newSet = new Set(prev)
      if (newSet.has(roleId)) {
        newSet.delete(roleId)
      } else {
        newSet.add(roleId)
      }
      return newSet
    })
  }

  // 保存角色分配
  const saveRoles = async () => {
    if (!currentUserId) return

    setRoleLoading(true)
    try {
      const roleIds = Array.from(userRoles)

      // 分配角色
      await apiClient.post(`/admin-users/${currentUserId}/roles`, { roleIds })

      toast({ title: '角色分配成功' })
      setRoleDialogOpen(false)
      fetchUsers()
    } catch {
      toast({ title: '角色分配失败', variant: 'destructive' })
    } finally {
      setRoleLoading(false)
    }
  }

  // 打开权限覆盖弹窗
  const openPermDialog = async (user: AdminUserVO) => {
    if (!user.id) return

    setPermUserId(user.id)
    setPermUsername(user.username || '')
    setPermDialogOpen(true)
    setPermLoading(true)
    setSelectedOverridePerms(new Set())

    try {
      // 获取用户有效权限
      const effectiveRes = await apiClient.get<ApiResponse<UserPermissionVO>>(`/admin-users/${user.id}/permissions`)
      if (effectiveRes.data.code === 200) {
        setUserEffectivePerms(effectiveRes.data.data)
      }

      // 获取用户可用（未拥有）的权限
      const availableRes = await apiClient.get<ApiResponse<UserAvailablePermissionVO>>(`/admin-users/${user.id}/permissions/available`)
      if (availableRes.data.code === 200) {
        setUserAvailablePerms(availableRes.data.data)
      }
    } catch {
      toast({ title: '获取权限信息失败', variant: 'destructive' })
    } finally {
      setPermLoading(false)
    }
  }

  // 切换权限覆盖选中
  const toggleOverridePerm = (permId: number) => {
    setSelectedOverridePerms(prev => {
      const newSet = new Set(prev)
      if (newSet.has(permId)) {
        newSet.delete(permId)
      } else {
        newSet.add(permId)
      }
      return newSet
    })
  }

  // 保存权限覆盖
  const savePermissionOverrides = async () => {
    if (!permUserId) return

    setPermLoading(true)
    try {
      for (const permId of selectedOverridePerms) {
        await apiClient.post(`/admin-users/${permUserId}/permission-overrides`, {
          permissionId: permId,
          effect: 'GRANT',
        })
      }

      toast({ title: '权限覆盖设置成功' })
      setPermDialogOpen(false)
    } catch {
      toast({ title: '权限覆盖设置失败', variant: 'destructive' })
    } finally {
      setPermLoading(false)
    }
  }

  const totalPages = Math.ceil(total / pageSize)

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">用户管理</h1>
          <p className="text-muted-foreground mt-1">管理系统用户账户</p>
        </div>
        <Button onClick={openCreateDialog}>
          <Plus className="w-4 h-4 mr-2" />
          创建用户
        </Button>
      </div>

      <Card>
        <CardHeader className="pb-4">
          <div className="flex items-center justify-between gap-4">
            <CardTitle className="text-lg">用户列表</CardTitle>
            <div className="flex items-center gap-3">
              <div className="relative w-64">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                <Input
                  placeholder="搜索用户..."
                  value={searchKeyword}
                  onChange={(e) => setSearchKeyword(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && fetchUsers()}
                  className="pl-9 h-9"
                />
              </div>
              <Button variant="outline" size="sm" onClick={fetchUsers} disabled={loading}>
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
                  <TableHead className="text-center">用户名</TableHead>
                  <TableHead className="text-center">昵称</TableHead>
                  <TableHead className="text-center">邮箱</TableHead>
                  <TableHead className="text-center">手机号</TableHead>
                  <TableHead className="w-24 text-center">超级管理员</TableHead>
                  <TableHead className="w-20 text-center">状态</TableHead>
                  <TableHead className="w-32 text-center">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={8} className="text-center py-12 text-muted-foreground">
                      加载中...
                    </TableCell>
                  </TableRow>
                ) : users.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={8} className="text-center py-12 text-muted-foreground">
                      暂无数据
                    </TableCell>
                  </TableRow>
                ) : (
                  users.map((user, index) => (
                    <TableRow key={user.id} className={index % 2 === 0 ? 'bg-background' : 'bg-muted/20'}>
                      <TableCell className="text-center font-mono text-sm py-3">{user.id}</TableCell>
                      <TableCell className="text-center font-medium py-3">{user.username}</TableCell>
                      <TableCell className="text-center py-3">{user.nickname || '-'}</TableCell>
                      <TableCell className="text-center py-3">{user.email || '-'}</TableCell>
                      <TableCell className="text-center py-3">{user.phone || '-'}</TableCell>
                      <TableCell className="text-center py-3">
                        {user.isSuperuser === 1 ? (
                          <Badge variant="default" className="text-xs">是</Badge>
                        ) : (
                          <Badge variant="secondary" className="text-xs">否</Badge>
                        )}
                      </TableCell>
                      <TableCell className="text-center py-3">
                        {user.status === 1 ? (
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
                            title="分配角色"
                            onClick={() => openRoleDialog(user)}
                          >
                            <UserCog className="w-3.5 h-3.5" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8"
                            title="权限覆盖"
                            onClick={() => openPermDialog(user)}
                          >
                            <Shield className="w-3.5 h-3.5" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8"
                            onClick={() => user.id && openEditDialog(user.id)}
                          >
                            <Pencil className="w-3.5 h-3.5" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8"
                            onClick={() => user.id && openDeleteDialog(user.id)}
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
              {editingId ? '修改用户信息' : '填写用户信息创建新账户'}
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid gap-2">
              <Label htmlFor="username">
                用户名 <span className="text-destructive">*</span>
              </Label>
              <Input
                id="username"
                value={formData.username}
                onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                disabled={!!editingId}
                placeholder="请输入用户名"
              />
            </div>

            <div className="grid gap-2">
              <Label htmlFor="password">{editingId ? '密码（留空则不修改）' : '密码'}</Label>
              <Input
                id="password"
                type="password"
                value={formData.password}
                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                placeholder={editingId ? '留空则不修改密码' : '请输入密码'}
              />
            </div>

            <div className="grid gap-2">
              <Label htmlFor="nickname">昵称</Label>
              <Input
                id="nickname"
                value={formData.nickname}
                onChange={(e) => setFormData({ ...formData, nickname: e.target.value })}
                placeholder="请输入昵称"
              />
            </div>

            <div className="grid gap-2">
              <Label htmlFor="email">邮箱</Label>
              <Input
                id="email"
                type="email"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                placeholder="请输入邮箱"
              />
            </div>

            <div className="grid gap-2">
              <Label htmlFor="phone">手机号</Label>
              <Input
                id="phone"
                value={formData.phone}
                onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                placeholder="请输入手机号"
              />
            </div>

            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Label htmlFor="isSuperuser">超级管理员</Label>
                <Switch
                  id="isSuperuser"
                  checked={formData.isSuperuser === 1}
                  onCheckedChange={(checked) => setFormData({ ...formData, isSuperuser: checked ? 1 : 0 })}
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
            <DialogDescription>删除后无法恢复，确定要删除该用户吗？</DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteDialogOpen(false)}>取消</Button>
            <Button variant="destructive" onClick={handleDelete} disabled={deletingLoading}>
              {deletingLoading ? '删除中...' : '确认删除'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 角色分配对话框 */}
      <Dialog open={roleDialogOpen} onOpenChange={setRoleDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>分配角色 - {currentUsername}</DialogTitle>
            <DialogDescription>勾选该用户拥有的角色</DialogDescription>
          </DialogHeader>

          {roleLoading ? (
            <div className="flex items-center justify-center py-8">
              <RefreshCw className="w-6 h-6 animate-spin text-muted-foreground" />
            </div>
          ) : (
            <div className="py-4 max-h-80 overflow-y-auto space-y-2">
              {allRoles.map((role) => (
                <div key={role.id} className="flex items-center gap-3 p-2 rounded hover:bg-muted/50">
                  <Checkbox
                    checked={userRoles.has(role.id!)}
                    onCheckedChange={() => role.id && toggleRole(role.id)}
                  />
                  <div className="flex-1">
                    <div className="font-medium text-sm">{role.name}</div>
                    <div className="text-xs text-muted-foreground">{role.code}</div>
                  </div>
                  {role.status === 1 ? (
                    <Badge variant="default" className="bg-emerald-500 text-xs">正常</Badge>
                  ) : (
                    <Badge variant="secondary" className="text-xs">禁用</Badge>
                  )}
                </div>
              ))}

              {allRoles.length === 0 && (
                <div className="text-center py-8 text-muted-foreground">
                  暂无可分配的角色
                </div>
              )}
            </div>
          )}

          <DialogFooter>
            <Button variant="outline" onClick={() => setRoleDialogOpen(false)}>取消</Button>
            <Button onClick={saveRoles} disabled={roleLoading}>
              {roleLoading ? '保存中...' : '保存'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 权限覆盖对话框 */}
      <Dialog open={permDialogOpen} onOpenChange={setPermDialogOpen}>
        <DialogContent className="sm:max-w-2xl max-h-[80vh] overflow-hidden flex flex-col">
          <DialogHeader>
            <DialogTitle>权限覆盖 - {permUsername}</DialogTitle>
            <DialogDescription>为用户设置额外的权限覆盖（可临时授予未分配的权限）</DialogDescription>
          </DialogHeader>

          {permLoading ? (
            <div className="flex items-center justify-center py-8">
              <RefreshCw className="w-6 h-6 animate-spin text-muted-foreground" />
            </div>
          ) : (
            <Tabs defaultValue="effective" className="flex-1 overflow-hidden flex flex-col">
              <TabsList className="w-full justify-start">
                <TabsTrigger value="effective">有效权限</TabsTrigger>
                <TabsTrigger value="available">可添加覆盖</TabsTrigger>
              </TabsList>

              <TabsContent value="effective" className="flex-1 overflow-y-auto">
                {userEffectivePerms?.groupedPermissions?.map((group) => (
                  <div key={group.groupKey} className="border-b last:border-b-0 py-3">
                    <div className="font-medium text-sm mb-2">{group.groupName}</div>
                    <div className="space-y-1 pl-2">
                      {group.children?.map((perm) => (
                        <div key={perm.permissionId} className="flex items-center gap-2 text-sm">
                          {perm.effect === 'GRANT' ? (
                            <Check className="w-3.5 h-3.5 text-emerald-500" />
                          ) : (
                            <X className="w-3.5 h-3.5 text-red-500" />
                          )}
                          <span>{perm.name}</span>
                          <Badge variant="outline" className="text-xs ml-auto">
                            {perm.source}
                          </Badge>
                        </div>
                      ))}
                    </div>
                  </div>
                ))}

                {(!userEffectivePerms?.groupedPermissions || userEffectivePerms.groupedPermissions.length === 0) && (
                  <div className="text-center py-8 text-muted-foreground">
                    暂无有效权限
                  </div>
                )}
              </TabsContent>

              <TabsContent value="available" className="flex-1 overflow-y-auto">
                {userAvailablePerms?.unassignedPermissions?.map((group) => (
                  <div key={group.groupKey} className="border-b last:border-b-0 py-3">
                    <div className="font-medium text-sm mb-2">{group.groupName}</div>
                    <div className="space-y-1 pl-2">
                      {group.permissions?.map((perm) => (
                        <div key={perm.id} className="flex items-center gap-2 text-sm">
                          <Checkbox
                            checked={selectedOverridePerms.has(perm.id!)}
                            onCheckedChange={() => perm.id && toggleOverridePerm(perm.id)}
                          />
                          <span>{perm.name}</span>
                          <Badge variant="destructive" className="text-xs ml-auto">{perm.reason}</Badge>
                        </div>
                      ))}
                    </div>
                  </div>
                ))}

                {(!userAvailablePerms?.unassignedPermissions || userAvailablePerms.unassignedPermissions.length === 0) && (
                  <div className="text-center py-8 text-muted-foreground">
                    暂无可添加覆盖的权限
                  </div>
                )}
              </TabsContent>
            </Tabs>
          )}

          <DialogFooter>
            <Button variant="outline" onClick={() => setPermDialogOpen(false)}>关闭</Button>
            <Button onClick={savePermissionOverrides} disabled={permLoading || selectedOverridePerms.size === 0}>
              {permLoading ? '保存中...' : `添加覆盖 (${selectedOverridePerms.size})`}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
