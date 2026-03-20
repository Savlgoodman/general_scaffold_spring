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
import { Plus, Search, Pencil, Trash2, RefreshCw, FolderOpen, FileText } from 'lucide-react'
import { apiClient } from '@/lib/api-client'
import type { PermissionBaseVO } from '@/api/generated/model'

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

interface PermissionFormData {
  name: string
  code: string
  path: string
  method: string
  groupKey: string
  groupName: string
  isGroup: number
  status: number
  sort: number
}

const initialFormData: PermissionFormData = {
  name: '',
  code: '',
  path: '',
  method: '*',
  groupKey: '',
  groupName: '',
  isGroup: 0,
  status: 1,
  sort: 0,
}

export default function PermissionManagement() {
  const { toast } = useToast()
  const [permissions, setPermissions] = useState<PermissionBaseVO[]>([])
  const [loading, setLoading] = useState(false)
  const [total, setTotal] = useState(0)
  const [current, setCurrent] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [searchKeyword, setSearchKeyword] = useState('')

  const [dialogOpen, setDialogOpen] = useState(false)
  const [dialogTitle, setDialogTitle] = useState('创建权限')
  const [formData, setFormData] = useState<PermissionFormData>(initialFormData)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [formLoading, setFormLoading] = useState(false)

  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [deletingLoading, setDeletingLoading] = useState(false)

  const fetchPermissions = useCallback(async () => {
    setLoading(true)
    try {
      const params: Record<string, string | number> = {
        current,
        size: pageSize,
      }
      if (searchKeyword) {
        params.keyword = searchKeyword
      }

      const res = await apiClient.get<ApiResponse<PageData<PermissionBaseVO>>>('/permissions', { params })
      const { code, data } = res.data

      if (code === 200) {
        setPermissions(data.records || [])
        setTotal(data.total || 0)
        setCurrent(data.current || 1)
        setPageSize(data.size || 10)
      }
    } catch {
      toast({ title: '获取权限列表失败', description: '请稍后重试', variant: 'destructive' })
    } finally {
      setLoading(false)
    }
  }, [current, pageSize, searchKeyword, toast])

  useEffect(() => {
    fetchPermissions()
  }, [fetchPermissions])

  const openCreateDialog = () => {
    setDialogTitle('创建权限')
    setFormData(initialFormData)
    setEditingId(null)
    setDialogOpen(true)
  }

  const openEditDialog = async (id: number) => {
    setDialogTitle('编辑权限')
    setEditingId(id)
    setFormLoading(true)
    setDialogOpen(true)

    try {
      const res = await apiClient.get<ApiResponse<PermissionBaseVO>>(`/permissions/${id}`)
      const { code, data } = res.data

      if (code === 200 && data) {
        setFormData({
          name: data.name || '',
          code: data.code || '',
          path: data.path || '',
          method: data.method || '*',
          groupKey: data.groupKey || '',
          groupName: data.groupName || '',
          isGroup: data.isGroup ? 1 : 0,
          status: data.status || 1,
          sort: data.sort || 0,
        })
      }
    } catch {
      toast({ title: '获取权限详情失败', variant: 'destructive' })
      setDialogOpen(false)
    } finally {
      setFormLoading(false)
    }
  }

  const handleSubmit = async () => {
    if (!formData.name) {
      toast({ title: '请填写权限名称', variant: 'destructive' })
      return
    }
    if (!formData.code && !editingId) {
      toast({ title: '请填写权限编码', variant: 'destructive' })
      return
    }
    if (!formData.isGroup && !formData.path) {
      toast({ title: '请填写接口路径', variant: 'destructive' })
      return
    }

    setFormLoading(true)
    try {
      if (editingId) {
        const res = await apiClient.put<ApiResponse<null>>(`/permissions/${editingId}`, {
          name: formData.name,
          path: formData.path,
          method: formData.method,
          groupKey: formData.groupKey,
          groupName: formData.groupName,
          isGroup: formData.isGroup,
          status: formData.status,
          sort: formData.sort,
        })
        const { code, message } = res.data

        if (code === 200) {
          toast({ title: '更新权限成功' })
          setDialogOpen(false)
          fetchPermissions()
        } else {
          toast({ title: '更新权限失败', description: message, variant: 'destructive' })
        }
      } else {
        const res = await apiClient.post<ApiResponse<null>>('/permissions', {
          name: formData.name,
          code: formData.code,
          path: formData.path,
          method: formData.method,
          groupKey: formData.groupKey,
          groupName: formData.groupName,
          isGroup: formData.isGroup,
          status: formData.status,
          sort: formData.sort,
        })
        const { code, message } = res.data

        if (code === 200) {
          toast({ title: '创建权限成功' })
          setDialogOpen(false)
          fetchPermissions()
        } else {
          toast({ title: '创建权限失败', description: message, variant: 'destructive' })
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
      const res = await apiClient.delete<ApiResponse<null>>(`/permissions/${deletingId}`)
      const { code, message } = res.data

      if (code === 200) {
        toast({ title: '删除权限成功' })
        setDeleteDialogOpen(false)
        fetchPermissions()
      } else {
        toast({ title: '删除权限失败', description: message, variant: 'destructive' })
      }
    } catch {
      toast({ title: '删除失败', variant: 'destructive' })
    } finally {
      setDeletingLoading(false)
    }
  }

  const totalPages = Math.ceil(total / pageSize)

  // 按分组聚合权限
  const groupedPermissions = permissions.reduce((acc, perm) => {
    const key = perm.groupKey || '未分组'
    if (!acc[key]) {
      acc[key] = {
        groupKey: key,
        groupName: perm.groupName || key,
        permissions: [],
      }
    }
    acc[key].permissions.push(perm)
    return acc
  }, {} as Record<string, { groupKey: string; groupName: string; permissions: PermissionBaseVO[] }>)

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">权限管理</h1>
          <p className="text-muted-foreground mt-1">管理系统权限资源</p>
        </div>
        <Button onClick={openCreateDialog}>
          <Plus className="w-4 h-4 mr-2" />
          创建权限
        </Button>
      </div>

      <Card>
        <CardHeader className="pb-4">
          <div className="flex items-center justify-between gap-4">
            <CardTitle className="text-lg">权限列表</CardTitle>
            <div className="flex items-center gap-3">
              <div className="relative w-64">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                <Input
                  placeholder="搜索权限..."
                  value={searchKeyword}
                  onChange={(e) => setSearchKeyword(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && fetchPermissions()}
                  className="pl-9 h-9"
                />
              </div>
              <Button variant="outline" size="sm" onClick={fetchPermissions} disabled={loading}>
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
                  <TableHead className="text-center">权限名称</TableHead>
                  <TableHead className="text-center">权限编码</TableHead>
                  <TableHead className="text-center">路径</TableHead>
                  <TableHead className="w-20 text-center">方法</TableHead>
                  <TableHead className="w-24 text-center">分组</TableHead>
                  <TableHead className="w-20 text-center">状态</TableHead>
                  <TableHead className="w-24 text-center">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={8} className="text-center py-12 text-muted-foreground">
                      加载中...
                    </TableCell>
                  </TableRow>
                ) : permissions.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={8} className="text-center py-12 text-muted-foreground">
                      暂无数据
                    </TableCell>
                  </TableRow>
                ) : (
                  Object.values(groupedPermissions).map((group) =>
                    group.permissions.map((perm, index) => (
                      <TableRow
                        key={perm.id}
                        className={index === 0 ? (index % 2 === 0 ? 'bg-background' : 'bg-muted/20') : ''}
                      >
                        {index === 0 && (
                          <TableCell
                            rowSpan={group.permissions.length}
                            className="text-center font-mono text-sm py-3 align-top"
                          >
                            {perm.id}
                          </TableCell>
                        )}
                        <TableCell className="text-center py-3">
                          <div className="flex items-center justify-center gap-2">
                            {perm.isGroup ? (
                              <FolderOpen className="w-4 h-4 text-amber-500" />
                            ) : (
                              <FileText className="w-4 h-4 text-blue-500" />
                            )}
                            <span className="font-medium">{perm.name}</span>
                            {perm.isGroup && (
                              <Badge variant="secondary" className="text-xs">组</Badge>
                            )}
                          </div>
                        </TableCell>
                        <TableCell className="text-center font-mono text-sm py-3">{perm.code}</TableCell>
                        <TableCell className="text-center text-sm py-3 max-w-xs truncate" title={perm.path}>
                          {perm.path || '-'}
                        </TableCell>
                        <TableCell className="text-center py-3">
                          {perm.method && perm.method !== '*' ? (
                            <Badge
                              variant="secondary"
                              className={`text-xs ${
                                perm.method === 'GET' ? 'bg-green-100 text-green-700' :
                                perm.method === 'POST' ? 'bg-blue-100 text-blue-700' :
                                perm.method === 'PUT' ? 'bg-orange-100 text-orange-700' :
                                perm.method === 'DELETE' ? 'bg-red-100 text-red-700' :
                                ''
                              }`}
                            >
                              {perm.method}
                            </Badge>
                          ) : (
                            <Badge variant="secondary" className="text-xs">全部</Badge>
                          )}
                        </TableCell>
                        <TableCell className="text-center text-sm py-3">
                          {perm.groupName || '-'}
                        </TableCell>
                        <TableCell className="text-center py-3">
                          {perm.status === 1 ? (
                            <Badge variant="default" className="bg-emerald-500 text-xs">启用</Badge>
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
                              onClick={() => perm.id && openEditDialog(perm.id)}
                            >
                              <Pencil className="w-3.5 h-3.5" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-8 w-8"
                              onClick={() => perm.id && openDeleteDialog(perm.id)}
                            >
                              <Trash2 className="w-3.5 h-3.5 text-destructive" />
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    ))
                  )
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
              {editingId ? '修改权限信息' : '填写权限信息创建新权限'}
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid gap-2">
              <Label htmlFor="name">
                权限名称 <span className="text-destructive">*</span>
              </Label>
              <Input
                id="name"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                placeholder="如：用户列表"
              />
            </div>

            <div className="grid gap-2">
              <Label htmlFor="code">权限编码</Label>
              <Input
                id="code"
                value={formData.code}
                onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                disabled={!!editingId}
                placeholder="如：admin_users_list"
              />
            </div>

            <div className="grid gap-2">
              <Label htmlFor="method">请求方法</Label>
              <Select value={formData.method} onValueChange={(v) => setFormData({ ...formData, method: v })}>
                <SelectTrigger id="method">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="*">全部</SelectItem>
                  <SelectItem value="GET">GET</SelectItem>
                  <SelectItem value="POST">POST</SelectItem>
                  <SelectItem value="PUT">PUT</SelectItem>
                  <SelectItem value="DELETE">DELETE</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="grid gap-2">
              <Label htmlFor="path">接口路径</Label>
              <Input
                id="path"
                value={formData.path}
                onChange={(e) => setFormData({ ...formData, path: e.target.value })}
                placeholder="如：/api/admin/admin-users"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-2">
                <Label htmlFor="groupKey">分组标识</Label>
                <Input
                  id="groupKey"
                  value={formData.groupKey}
                  onChange={(e) => setFormData({ ...formData, groupKey: e.target.value })}
                  placeholder="如：admin_users"
                />
              </div>

              <div className="grid gap-2">
                <Label htmlFor="groupName">分组名称</Label>
                <Input
                  id="groupName"
                  value={formData.groupName}
                  onChange={(e) => setFormData({ ...formData, groupName: e.target.value })}
                  placeholder="如：用户管理"
                />
              </div>
            </div>

            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Label htmlFor="isGroup">是否组权限</Label>
                <Switch
                  id="isGroup"
                  checked={formData.isGroup === 1}
                  onCheckedChange={(checked) => setFormData({ ...formData, isGroup: checked ? 1 : 0 })}
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

            <div className="grid gap-2">
              <Label htmlFor="sort">排序</Label>
              <Input
                id="sort"
                type="number"
                value={formData.sort}
                onChange={(e) => setFormData({ ...formData, sort: Number(e.target.value) })}
                placeholder="数值越小越靠前"
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
            <DialogDescription>删除后无法恢复，确定要删除该权限吗？</DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteDialogOpen(false)}>取消</Button>
            <Button variant="destructive" onClick={handleDelete} disabled={deletingLoading}>
              {deletingLoading ? '删除中...' : '确认删除'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
