import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Shield, Users, Settings, Activity } from "lucide-react"

export default function Hello() {
  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-4">
      <div className="w-full max-w-2xl space-y-8">
        {/* Header */}
        <div className="text-center space-y-2">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-primary text-primary-foreground mb-4">
            <Shield className="w-8 h-8" />
          </div>
          <h1 className="text-4xl font-bold tracking-tight">Admin Scaffold</h1>
          <p className="text-muted-foreground text-lg">
            后台管理系统脚手架 · React + shadcn/ui
          </p>
        </div>

        {/* Feature Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <Card>
            <CardHeader className="pb-2">
              <Users className="w-5 h-5 text-primary mb-2" />
              <CardTitle className="text-base">RBAC 权限</CardTitle>
              <CardDescription>
                用户、角色、权限三层管理
              </CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-xs text-muted-foreground">
                支持动态菜单、权限覆盖、通配符匹配
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-2">
              <Settings className="w-5 h-5 text-primary mb-2" />
              <CardTitle className="text-base">系统管理</CardTitle>
              <CardDescription>
                用户、角色、菜单、权限
              </CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-xs text-muted-foreground">
                完整 CRUD、树形结构、批量操作
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-2">
              <Activity className="w-5 h-5 text-primary mb-2" />
              <CardTitle className="text-base">日志监控</CardTitle>
              <CardDescription>
                API、登录、操作、异常日志
              </CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-xs text-muted-foreground">
                AOP 自动采集，系统资源实时监控
              </p>
            </CardContent>
          </Card>
        </div>

        {/* Tech Stack */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">技术栈</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex flex-wrap gap-2">
              {["React 18", "TypeScript", "Tailwind CSS", "shadcn/ui", "Zustand", "Axios", "React Router"].map(
                (tech) => (
                  <span
                    key={tech}
                    className="inline-flex items-center rounded-md border px-2.5 py-0.5 text-xs font-semibold transition-colors"
                  >
                    {tech}
                  </span>
                )
              )}
            </div>
          </CardContent>
        </Card>

        {/* Action */}
        <div className="text-center">
          <Button size="lg">开始使用</Button>
        </div>
      </div>
    </div>
  )
}
