import { Skeleton } from '@/components/ui/skeleton'
import { Card } from '@/components/ui/card'
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table'

/**
 * 表格骨架屏
 * 适用于 UserManagement、RoleManagement 等表格页面
 */
export function TableSkeleton({ rows = 5, cols = 6 }: { rows?: number; cols?: number }) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          {Array.from({ length: cols }).map((_, i) => (
            <TableHead key={i}>
              <Skeleton className="h-4 w-20" />
            </TableHead>
          ))}
        </TableRow>
      </TableHeader>
      <TableBody>
        {Array.from({ length: rows }).map((_, rowIdx) => (
          <TableRow key={rowIdx}>
            {Array.from({ length: cols }).map((_, colIdx) => (
              <TableCell key={colIdx} className="py-3">
                <Skeleton
                  className={`h-4 ${
                    colIdx === 0 ? 'w-10' :
                    colIdx === cols - 1 ? 'w-24' :
                    'w-16 sm:w-24'
                  }`}
                />
              </TableCell>
            ))}
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}

/**
 * Card 分组骨架屏
 * 适用于 PermissionManagement、MenuManagement 等 Card 分组页面
 */
export function CardGroupSkeleton({ groups = 3, itemsPerGroup = 4 }: { groups?: number; itemsPerGroup?: number }) {
  return (
    <div className="space-y-3">
      {Array.from({ length: groups }).map((_, gIdx) => (
        <Card key={gIdx} className="overflow-hidden">
          {/* Card 头部 */}
          <div className="flex items-center gap-3 px-4 py-3">
            <Skeleton className="size-4" />
            <Skeleton className="size-5" />
            <Skeleton className="h-4 w-24" />
            <Skeleton className="h-5 w-12 rounded-full" />
            <Skeleton className="h-3 w-32 hidden sm:block" />
            <div className="ml-auto">
              <Skeleton className="h-3 w-16" />
            </div>
          </div>
          {/* 展开的子项 */}
          <div className="border-t px-3 py-2 space-y-1">
            {Array.from({ length: itemsPerGroup }).map((_, iIdx) => (
              <div key={iIdx} className="flex items-center gap-2.5 py-2 px-3">
                <Skeleton className="size-4" />
                <Skeleton className="h-5 w-14 rounded" />
                <Skeleton className="h-4 w-20 sm:w-32" />
                <Skeleton className="h-3 w-24 hidden sm:block" />
                <div className="ml-auto flex gap-1">
                  <Skeleton className="size-6" />
                </div>
              </div>
            ))}
          </div>
        </Card>
      ))}
    </div>
  )
}

/**
 * 对话框内分组骨架屏
 * 适用于 RolePermissionDialog、RoleMenuDialog、UserDetailDialog 等
 */
export function DialogGroupSkeleton({ groups = 3, itemsPerGroup = 3 }: { groups?: number; itemsPerGroup?: number }) {
  return (
    <div className="space-y-3 py-2">
      {Array.from({ length: groups }).map((_, gIdx) => (
        <div key={gIdx} className="border rounded-lg">
          {/* 组头 */}
          <div className="flex items-center gap-2.5 px-3 py-2.5 bg-muted/30">
            <Skeleton className="size-4" />
            <Skeleton className="size-4" />
            <Skeleton className="size-4" />
            <Skeleton className="h-4 w-20" />
            <Skeleton className="h-5 w-10 rounded-full" />
            <div className="ml-auto">
              <Skeleton className="h-3 w-10" />
            </div>
          </div>
          {/* 子项 */}
          <div className="px-3 pb-2.5 pt-1 space-y-0.5">
            {Array.from({ length: itemsPerGroup }).map((_, iIdx) => (
              <div key={iIdx} className="flex items-center gap-2 py-1.5 pl-8">
                <Skeleton className="size-4" />
                <Skeleton className="h-4 w-16 sm:w-28" />
                <Skeleton className="h-3 w-20 hidden sm:block" />
                <div className="ml-auto">
                  <Skeleton className="h-5 w-10 rounded-full" />
                </div>
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  )
}
