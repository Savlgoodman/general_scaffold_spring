import { useState, useEffect, useCallback, useMemo } from 'react'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { useToast } from '@/hooks/use-toast'
import {
  Search, RefreshCw, FolderOpen, FileText, ChevronDown, ChevronRight,
} from 'lucide-react'
import { getPermissions } from '@/api/generated/permissions/permissions'
import type { PermissionGroupVO, PermissionBaseVO } from '@/api/generated/model'

const permissionsApi = getPermissions()

const METHOD_COLORS: Record<string, string> = {
  GET: 'bg-emerald-500 text-white',
  POST: 'bg-blue-500 text-white',
  PUT: 'bg-amber-500 text-white',
  DELETE: 'bg-red-500 text-white',
  '*': 'bg-purple-500 text-white',
}

export default function PermissionManagement() {
  const { toast } = useToast()
  const [groups, setGroups] = useState<PermissionGroupVO[]>([])
  const [loading, setLoading] = useState(false)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [expandedGroups, setExpandedGroups] = useState<Set<string>>(new Set())

  const fetchGroups = useCallback(async () => {
    setLoading(true)
    try {
      const res = await permissionsApi.getGroups()
      if (res.code === 200 && res.data) {
        setGroups(res.data)
        // 默认展开所有组
        setExpandedGroups(new Set(res.data.map((g: PermissionGroupVO) => g.groupKey!).filter(Boolean)))
      }
    } catch {
      toast({ title: '获取权限列表失败', variant: 'destructive' })
    } finally {
      setLoading(false)
    }
  }, [toast])

  useEffect(() => { fetchGroups() }, [fetchGroups])

  const toggleExpand = (key: string) => {
    setExpandedGroups(prev => {
      const next = new Set(prev)
      if (next.has(key)) next.delete(key); else next.add(key)
      return next
    })
  }

  const expandAll = () => setExpandedGroups(new Set(groups.map(g => g.groupKey!).filter(Boolean)))
  const collapseAll = () => setExpandedGroups(new Set())

  // 搜索过滤
  const filteredGroups = useMemo(() => {
    if (!searchKeyword.trim()) return groups
    const kw = searchKeyword.toLowerCase()
    return groups
      .map(group => {
        const groupMatch = group.groupName?.toLowerCase().includes(kw) || group.groupKey?.toLowerCase().includes(kw)
        const matchedChildren = (group.children || []).filter(
          c => c.name?.toLowerCase().includes(kw) || c.path?.toLowerCase().includes(kw) || c.code?.toLowerCase().includes(kw)
        )
        const gpMatch = group.groupPermission &&
          (group.groupPermission.name?.toLowerCase().includes(kw) || group.groupPermission.path?.toLowerCase().includes(kw))

        if (groupMatch) return group
        if (gpMatch || matchedChildren.length > 0) {
          return { ...group, children: matchedChildren.length > 0 ? matchedChildren : group.children }
        }
        return null
      })
      .filter(Boolean) as PermissionGroupVO[]
  }, [groups, searchKeyword])

  // 统计
  const totalGroups = groups.length
  const totalPerms = groups.reduce((sum, g) => sum + (g.children?.length || 0) + (g.groupPermission ? 1 : 0), 0)

  const renderPermRow = (perm: PermissionBaseVO, isGroupPerm: boolean) => (
    <div key={perm.id} className={`flex items-center gap-2 py-1.5 px-3 rounded hover:bg-muted/40 ${isGroupPerm ? 'bg-muted/20' : ''}`}>
      {isGroupPerm
        ? <FolderOpen className="w-4 h-4 text-amber-500 shrink-0" />
        : <FileText className="w-4 h-4 text-blue-400 shrink-0" />
      }
      <span className="text-sm font-medium">{perm.name}</span>
      {isGroupPerm && <Badge variant="outline" className="text-xs">组权限</Badge>}
      <Badge variant="secondary" className={`text-xs shrink-0 ${METHOD_COLORS[perm.method || '*'] || 'bg-gray-500 text-white'}`}>
        {perm.method || '*'}
      </Badge>
      <span className="text-xs text-muted-foreground truncate">{perm.path}</span>
      <div className="ml-auto flex items-center gap-1.5 shrink-0">
        {perm.status === 1
          ? <Badge className="bg-emerald-500 text-xs">启用</Badge>
          : <Badge variant="destructive" className="text-xs">禁用</Badge>
        }
        <span className="text-xs text-muted-foreground font-mono">{perm.code}</span>
      </div>
    </div>
  )

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">权限管理</h1>
          <p className="text-muted-foreground mt-1">
            查看系统权限资源 &mdash; {totalGroups} 个分组，{totalPerms} 个权限
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="sm" onClick={expandAll}>全部展开</Button>
          <Button variant="outline" size="sm" onClick={collapseAll}>全部折叠</Button>
          <Button variant="outline" size="sm" onClick={fetchGroups} disabled={loading}>
            <RefreshCw className={`w-4 h-4 mr-1.5 ${loading ? 'animate-spin' : ''}`} />刷新
          </Button>
        </div>
      </div>

      {/* 搜索 */}
      <div className="relative w-80">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
        <Input
          placeholder="搜索权限名称、路径、编码..."
          value={searchKeyword}
          onChange={(e) => setSearchKeyword(e.target.value)}
          className="pl-9 h-9"
        />
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <RefreshCw className="w-8 h-8 animate-spin text-muted-foreground" />
        </div>
      ) : filteredGroups.length === 0 ? (
        <Card>
          <CardContent className="py-20 text-center text-muted-foreground">
            {searchKeyword ? '没有匹配的权限' : '暂无权限数据，请运行同步脚本'}
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-3">
          {filteredGroups.map(group => {
            const expanded = expandedGroups.has(group.groupKey!)
            const childCount = group.children?.length || 0
            const gp = group.groupPermission

            return (
              <Card key={group.groupKey} className="overflow-hidden">
                {/* 组头 */}
                <button
                  className="flex items-center gap-3 w-full px-4 py-3 text-left hover:bg-muted/30 transition-colors"
                  onClick={() => toggleExpand(group.groupKey!)}
                >
                  {expanded
                    ? <ChevronDown className="w-4 h-4 shrink-0" />
                    : <ChevronRight className="w-4 h-4 shrink-0" />
                  }
                  <FolderOpen className="w-5 h-5 text-amber-500 shrink-0" />
                  <span className="font-semibold">{group.groupName}</span>
                  <Badge variant="outline" className="text-xs font-mono">{group.groupKey}</Badge>
                  {gp && (
                    <span className="text-xs text-muted-foreground">{gp.path}</span>
                  )}
                  <span className="ml-auto text-xs text-muted-foreground">
                    {childCount} 个接口{gp ? ' + 1 组权限' : ''}
                  </span>
                </button>

                {/* 子权限列表 */}
                {expanded && (
                  <div className="border-t px-2 py-2 space-y-0.5">
                    {gp && renderPermRow(gp, true)}
                    {group.children?.map(child => renderPermRow(child, false))}
                    {childCount === 0 && !gp && (
                      <div className="text-sm text-muted-foreground text-center py-4">该分组下暂无权限</div>
                    )}
                  </div>
                )}
              </Card>
            )
          })}
        </div>
      )}
    </div>
  )
}
