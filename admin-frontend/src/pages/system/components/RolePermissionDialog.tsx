import { useState, useEffect, useCallback } from 'react'
import {
  Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Checkbox } from '@/components/ui/checkbox'
import { useToast } from '@/hooks/use-toast'
import { RefreshCw, ChevronDown, ChevronRight, FolderOpen } from 'lucide-react'
import { getRoles } from '@/api/generated/roles/roles'
import type { RolePermissionFullVO, GroupSection } from '@/api/generated/model'

const rolesApi = getRoles()

interface Props {
  open: boolean
  onOpenChange: (open: boolean) => void
  roleId: number | null
  roleName: string
  onSaved?: () => void
}

const METHOD_COLORS: Record<string, string> = {
  GET: 'bg-emerald-500', POST: 'bg-blue-500', PUT: 'bg-amber-500', DELETE: 'bg-red-500', '*': 'bg-purple-500',
}

export default function RolePermissionDialog({ open, onOpenChange, roleId, roleName, onSaved }: Props) {
  const { toast } = useToast()
  const [data, setData] = useState<RolePermissionFullVO | null>(null)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set())
  const [expandedGroups, setExpandedGroups] = useState<Set<string>>(new Set())

  const fetchData = useCallback(async () => {
    if (!roleId) return
    setLoading(true)
    try {
      const res = await rolesApi.getPermissions(roleId)
      if (res.code === 200 && res.data) {
        setData(res.data)
        const assigned = new Set<number>()
        res.data.groups?.forEach(group => {
          if (group.groupPermission?.assigned && group.groupPermission.id) assigned.add(group.groupPermission.id)
          group.children?.forEach(child => { if (child.assigned && child.id) assigned.add(child.id) })
        })
        setSelectedIds(assigned)
        setExpandedGroups(new Set(res.data.groups?.map(g => g.groupKey!).filter(Boolean) || []))
      }
    } catch {
      toast({ title: '获取权限信息失败', variant: 'destructive' })
    } finally {
      setLoading(false)
    }
  }, [roleId, toast])

  useEffect(() => { if (open && roleId) fetchData() }, [open, roleId, fetchData])

  const isGroupGranted = (group: GroupSection): boolean =>
    group.groupPermission?.id != null && selectedIds.has(group.groupPermission.id)

  const toggleGroup = (group: GroupSection) => {
    const gpId = group.groupPermission?.id
    if (gpId == null) return
    setSelectedIds(prev => {
      const next = new Set(prev)
      if (next.has(gpId)) next.delete(gpId); else next.add(gpId)
      return next
    })
  }

  const toggleChild = (childId: number) => {
    setSelectedIds(prev => {
      const next = new Set(prev)
      if (next.has(childId)) next.delete(childId); else next.add(childId)
      return next
    })
  }

  const toggleExpand = (groupKey: string) => {
    setExpandedGroups(prev => {
      const next = new Set(prev)
      if (next.has(groupKey)) next.delete(groupKey); else next.add(groupKey)
      return next
    })
  }

  const handleSave = async () => {
    if (!roleId) return
    setSaving(true)
    try {
      const permissions = Array.from(selectedIds).map(id => ({ permissionId: id, effect: 'GRANT' }))
      const res = await rolesApi.syncPermissions(roleId, { permissions })
      if (res.code === 200) {
        toast({ title: '权限分配成功' }); onOpenChange(false); onSaved?.()
      } else {
        toast({ title: '权限分配失败', description: res.message, variant: 'destructive' })
      }
    } catch {
      toast({ title: '权限分配失败', variant: 'destructive' })
    } finally {
      setSaving(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-2xl max-h-[80vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle>分配权限 - {roleName}</DialogTitle>
          <DialogDescription>已选 {selectedIds.size}/{data?.summary?.totalPermissions || 0} 个权限。组权限勾选后，子权限自动覆盖。</DialogDescription>
        </DialogHeader>
        {loading ? (
          <div className="flex items-center justify-center py-12"><RefreshCw className="w-6 h-6 animate-spin text-muted-foreground" /></div>
        ) : (
          <div className="flex-1 overflow-y-auto space-y-3 py-2">
            {data?.groups?.map(group => {
              const expanded = expandedGroups.has(group.groupKey!)
              const groupGranted = isGroupGranted(group)
              const gp = group.groupPermission
              return (
                <div key={group.groupKey} className="border rounded-lg">
                  <div className="flex items-center gap-2 p-3 bg-muted/30">
                    <button onClick={() => toggleExpand(group.groupKey!)} className="p-0.5 hover:bg-muted rounded">
                      {expanded ? <ChevronDown className="w-4 h-4" /> : <ChevronRight className="w-4 h-4" />}
                    </button>
                    {gp && <Checkbox checked={groupGranted} onCheckedChange={() => toggleGroup(group)} className="h-4 w-4" />}
                    <FolderOpen className="w-4 h-4 text-muted-foreground" />
                    <span className="font-medium text-sm">{group.groupName}</span>
                    {gp && <Badge variant="outline" className="text-xs ml-1">{gp.method} {gp.path}</Badge>}
                    <span className="text-xs text-muted-foreground ml-auto">{group.assignedCount}/{group.totalCount}</span>
                    {groupGranted && <Badge className="bg-emerald-500 text-xs">组已授权</Badge>}
                  </div>
                  {expanded && (
                    <div className="px-3 pb-3 pt-1 space-y-1.5">
                      {group.children?.map(child => {
                        const covered = groupGranted
                        const checked = covered || selectedIds.has(child.id!)
                        return (
                          <div key={child.id} className="flex items-center gap-2 pl-7 py-1">
                            <Checkbox checked={checked} disabled={covered} onCheckedChange={() => child.id && toggleChild(child.id)} className="h-4 w-4" />
                            <Badge variant="secondary" className={`text-xs text-white ${METHOD_COLORS[child.method || ''] || 'bg-gray-500'}`}>{child.method}</Badge>
                            <span className="text-sm">{child.name}</span>
                            <span className="text-xs text-muted-foreground ml-1">{child.path}</span>
                            {covered && <Badge variant="outline" className="text-xs ml-auto text-emerald-600 border-emerald-300">已被组覆盖</Badge>}
                          </div>
                        )
                      })}
                      {(!group.children || group.children.length === 0) && <div className="text-sm text-muted-foreground pl-7">无子权限</div>}
                    </div>
                  )}
                </div>
              )
            })}
            {(!data?.groups || data.groups.length === 0) && <div className="text-center py-8 text-muted-foreground">暂无权限数据</div>}
          </div>
        )}
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>取消</Button>
          <Button onClick={handleSave} disabled={saving || loading}>{saving ? '保存中...' : '保存'}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
