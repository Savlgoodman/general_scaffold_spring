import {
  Users,
  Shield,
  LogIn,
  AlertCircle,
  TrendingUp,
  TrendingDown,
  Activity,
} from "lucide-react"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Separator } from "@/components/ui/separator"

const stats = [
  {
    title: "总用户数",
    value: "1,234",
    change: "+12.5%",
    trend: "up",
    icon: Users,
    description: "较上月增长",
  },
  {
    title: "在线用户",
    value: "56",
    change: "-3.2%",
    trend: "down",
    icon: Activity,
    description: "当前在线",
  },
  {
    title: "系统角色",
    value: "12",
    change: "+2",
    trend: "up",
    icon: Shield,
    description: "角色数量",
  },
  {
    title: "今日登录",
    value: "89",
    change: "+23.1%",
    trend: "up",
    icon: LogIn,
    description: "今日访问",
  },
]

const recentActivities = [
  {
    user: "张三",
    action: "登录系统",
    time: "2分钟前",
    avatar: "ZS",
  },
  {
    user: "李四",
    action: "更新了用户权限",
    time: "15分钟前",
    avatar: "LS",
  },
  {
    user: "王五",
    action: "修改了菜单配置",
    time: "1小时前",
    avatar: "WW",
  },
  {
    user: "赵六",
    action: "导出系统日志",
    time: "2小时前",
    avatar: "ZL",
  },
]

const systemStatus = [
  { name: "API服务", status: "正常运行", variant: "default" as const },
  { name: "数据库", status: "正常运行", variant: "default" as const },
  { name: "缓存服务", status: "正常运行", variant: "default" as const },
  { name: "文件存储", status: "正常运行", variant: "default" as const },
]

function Dashboard() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">仪表盘</h1>
        <p className="text-muted-foreground">
          欢迎回来！以下是系统概览。
        </p>
      </div>

      {/* Stats Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {stats.map((stat) => (
          <Card key={stat.title}>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">
                {stat.title}
              </CardTitle>
              <stat.icon className="size-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{stat.value}</div>
              <p className="text-xs text-muted-foreground">
                <span
                  className={
                    stat.trend === "up" ? "text-emerald-500" : "text-red-500"
                  }
                >
                  {stat.trend === "up" ? (
                    <TrendingUp className="inline size-3" />
                  ) : (
                    <TrendingDown className="inline size-3" />
                  )}
                  {stat.change}
                </span>{" "}
                {stat.description}
              </p>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Main Content */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-7">
        {/* Recent Activities */}
        <Card className="col-span-4">
          <CardHeader>
            <CardTitle>最近活动</CardTitle>
            <CardDescription>系统最近的操作用户行为</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {recentActivities.map((activity, index) => (
                <div key={index} className="flex items-center gap-4">
                  <Avatar className="size-8">
                    <AvatarImage src="" />
                    <AvatarFallback className="bg-muted text-xs">
                      {activity.avatar}
                    </AvatarFallback>
                  </Avatar>
                  <div className="flex flex-1 items-center justify-between">
                    <div className="space-y-1">
                      <p className="text-sm font-medium">{activity.user}</p>
                      <p className="text-xs text-muted-foreground">
                        {activity.action}
                      </p>
                    </div>
                    <span className="text-xs text-muted-foreground">
                      {activity.time}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* System Status */}
        <Card className="col-span-3">
          <CardHeader>
            <CardTitle>系统状态</CardTitle>
            <CardDescription>当前各服务运行状态</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {systemStatus.map((item, index) => (
                <div key={index} className="flex items-center justify-between">
                  <span className="text-sm">{item.name}</span>
                  <Badge variant={item.variant}>{item.status}</Badge>
                </div>
              ))}
            </div>
            <Separator className="my-4" />
            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              <AlertCircle className="size-3" />
              <span>最后检查: 1分钟前</span>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

export default Dashboard
