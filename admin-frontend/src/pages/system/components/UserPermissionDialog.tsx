import { useState, useEffect, useCallback } from 'react'
import {
  Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { useToast } from '@/hooks/use-toast'
import {
  RefreshCw, Check, X, ChevronDown, ChevronRight, Plus, Pencil, Trash2,
} from 'lucide-react'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import { getAdminUsersPermission } from '@/api/generated/admin-users-permission/admin-users-permission'
import type { UserPermissionOverviewVO, PermissionRow } from '@/api/generated/model'

const permApi = getAdminUsersPermission()

interface Props {
  open: boolean
  onOpenChange: (open: boolean) => void
  userId: number | null
  username: string
}

const METHOD_COLORS: Record<string, string> = {
  GET: 'bg-emerald-500', POST: 'bg-blue-500', PUT: 'bg-amber-500', DELETE: 'bg-red-500', '*': 'bg-purple-500',
}

export default function UserPermissionDialog({ open, onOpenChange, userId, username }: Props) {
  const { toast } = useToast()
  const [data, setData] = useState<UserPermissionOverviewVO | null>(null)
  const [loading, setLoading] = useState(false)
  const [expandedGroups, setExpandedGroups] = useState<Set<string>>(new Set())
  const [editingPermId, setEditingPermId] = useState<number | null>(null)
  const [editingEffect, setEditingEffect] = useState<string>('GRANT')

  const fetchData = useCallback(async () => {
    if (!userId) return
    setLoading(true)
    try {
      const res = await permApi.getUserPermissions(userId)
      if (res.code === 200 && res.data) {
        setData(res.data)
        setExpandedGroups(new Set(res.data.groups?.map(g => g.groupKey!).filter(Boolean) || []))
      }
    } catch {
      toast({ title: '获取权限信息失败', variant: 'destructive' })
    } finally {
      setLoading(false)
    }
  }, [userId, toast])

  useEffect(() => {
    if (open && userId) { fetchData(); setEditingPermId(null) }
  }, [open, userId, fetchData])

  const toggleExpand = (key: string) => {
    setExpandedGroups(prev => {
      const next = new Set(prev)
      if (next.has(key)) next.delete(key); else next.add(key)
      return next
    })
  }

  const startEdit = (permId: number, currentEffect?: string | null) => {
    setEditingPermId(permId)
    setEditingEffect(currentEffect || 'GRANT')
  }

  const saveOverride = async (permId: number) => {
    if (!userId) return
    try {
      const currentOverrides = (data?.overrides || [])
        .filter(o => o.permissionId !== permId)
        .map(o => ({ permissionId: o.permissionId!, effect: o.effect! }))
      currentOverrides.push({ permissionId: permId, effect: editingEffect })
      await permApi.syncUserOverrides(userId, { overrides: currentOverrides })
      toast({ title: '覆盖设置成功' })
      setEditingPermId(null)
      fetchData()
    } catch {
      toast({ title: '覆盖设置失败', variant: 'destructive' })
    }
  }

  const handleDeleteOverride = async (overrideId: number) => {
    if (!userId) return
    try {
      await permApi.removeUserPermissionOverride(userId, overrideId)
      toast({ title: '覆盖已删除' })
      fetchData()
    } catch {
      toast({ title: '删除覆盖失败', variant: 'destructive' })
    }
  }

  const renderEffectIcon = (effect?: string | null) => {
    if (effect === 'GRANT') return <Check className="w-4 h-4 text-emerald-500" />
    if (effect === 'DENY') return <X className="w-4 h-4 text-red-500" />
    return <div className="w-4 h-4 rounded-full border-2 border-muted-foreground/30" />
  }

  const renderSourceBadge = (row: PermissionRow) => {
    if (row.source === 'SUPER_USER') return <Badge className="bg-purple-500 text-xs">超管</Badge>
    if (row.source === 'OVERRIDE') return <Badge variant="destructive" className="text-xs">覆盖: {row.overrideEffect}</Badge>
    if (row.source === 'ROLE' && row.sourceRoles && row.sourceRoles.length > 0) {
      return <Badge variant="secondary" className="text-xs">角色: {row.sourceRoles.join(', ')}</Badge>
    }
    return <Badge variant="outline" className="text-xs">未分配</Badge>
  }

  const renderActions = (row: PermissionRow) => {
    if (data?.superuser) return null
    if (editingPermId === row.permissionId) {
      return (
        <div className="flex items-center gap-1">
          <Select value={editingEffect} onValueChange={setEditingEffect}>
            <SelectTrigger className="h-6 w-20 text-xs"><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="GRANT">GRANT</SelectItem>
              <SelectItem value="DENY">DENY</SelectItem>
            </SelectContent>
          </Select>
          <Button size="sm" variant="ghost" className="h-6 px-2 text-xs" onClick={() => saveOverride(row.permissionId!)}><Check className="w-3 h-3" /></Button>
          <Button size="sm" variant="ghost" className="h-6 px-2 text-xs" onClick={() => setEditingPermId(null)}><X className="w-3 h-3" /></Button>
        </div>
      )
    }
    if (row.hasOverride) {
      return (
        <div className="flex items-center gap-0.5">
          <Button size="sm" variant="ghost" className="h-6 px-1.5" title="编辑覆盖" onClick={() => startEdit(row.permissionId!, row.overrideEffect)}><Pencil className="w-3 h-3" /></Button>
          <Button size="sm" variant="ghost" className="h-6 px-1.5 text-destructive" title="删除覆盖" onClick={() => row.overrideId && handleDeleteOverride(row.overrideId)}><Trash2 className="w-3 h-3" /></Button>
        </div>
      )
    }
    return <Button size="sm" variant="ghost" className="h-6 px-1.5" title="添加覆盖" onClick={() => startEdit(row.permissionId!)}><Plus className="w-3 h-3" /></Button>
  }

  const summary = data?.summary

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-3xl max-h-[85vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle>权限管理 - {username}</DialogTitle>
          <DialogDescription>查看用户权限来源，添加/编辑/删除覆盖</DialogDescription>
        </DialogHeader>
        {loading ? (
          <div className="flex items-center justify-center py-12"><RefreshCw className="w-6 h-6 animate-spin text-muted-foreground" /></div>
        ) : data ? (
          <div className="flex-1 overflow-y-auto space-y-4 py-2">
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-sm font-medium">角色:</span>
              {data.roles?.map(r => <Badge key={r.id} variant="secondary" className="text-xs">{r.name}</Badge>)}
              {data.superuser && <Badge className="bg-purple-500 text-xs">超级管理员</Badge>}
            </div>
            {summary && (
              <div className="flex gap-3 text-xs">
                <span>总计 <strong>{summary.totalPermissions}</strong></span>
                <span className="text-emerald-600">授权 <strong>{summary.grantedCount}</strong></span>
                <span className="text-red-600">拒绝 <strong>{summary.deniedCount}</strong></span>
                <span className="text-muted-foreground">未分配 <strong>{summary.unassignedCount}</strong></span>
                <span className="text-blue-600">覆盖 <strong>{summary.overrideCount}</strong></span>
              </div>
            )}
            {data.groups?.map(group => {
              const expanded = expandedGroups.has(group.groupKey!)
              return (
                <div key={group.groupKey} className="border rounded-lg">
                  <button className="flex items-center gap-2 w-full p-2.5 text-left hover:bg-muted/30" onClick={() => toggleExpand(group.groupKey!)}>
                    {expanded ? <ChevronDown className="w-4 h-4" /> : <ChevronRight className="w-4 h-4" />}
                    <span className="font-medium text-sm">{group.groupName}</span>
                    <span className="text-xs text-muted-foreground">({group.children?.length || 0})</span>
                  </button>
                  {expanded && (
                    <div className="px-2 pb-2 space-y-1">
                      {group.children?.map(row => (
                        <div key={row.permissionId} className="flex items-center gap-2 pl-4 py-1 rounded hover:bg-muted/20">
                          {renderEffectIcon(row.finalEffect)}
                          <Badge variant="secondary" className={`text-xs text-white shrink-0 ${METHOD_COLORS[row.method || ''] || 'bg-gray-500'}`}>{row.method}</Badge>
                          <span className="text-sm truncate">{row.name}</span>
                          <div className="ml-auto flex items-center gap-2 shrink-0">
                            {renderSourceBadge(row)}
                            {renderActions(row)}
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )
            })}
            {data.overrides && data.overrides.length > 0 && (
              <div className="border rounded-lg p-3">
                <div className="font-medium text-sm mb-2">当前覆盖记录 ({data.overrides.length})</div>
                <div className="space-y-1">
                  {data.overrides.map(ov => (
                    <div key={ov.overrideId} className="flex items-center gap-2 text-sm py-1">
                      {ov.effect === 'GRANT' ? <Check className="w-3.5 h-3.5 text-emerald-500" /> : <X className="w-3.5 h-3.5 text-red-500" />}
                      <span>{ov.permissionName}</span>
                      <Badge variant={ov.effect === 'GRANT' ? 'default' : 'destructive'} className="text-xs">{ov.effect}</Badge>
                      <span className="text-xs text-muted-foreground ml-auto">{ov.createTime}</span>
                      <Button size="sm" variant="ghost" className="h-6 px-1.5 text-destructive" onClick={() => ov.overrideId && handleDeleteOverride(ov.overrideId)}><Trash2 className="w-3 h-3" /></Button>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        ) : null}
        <DialogFooter><Button variant="outline" onClick={() => onOpenChange(false)}>关闭</Button></DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
