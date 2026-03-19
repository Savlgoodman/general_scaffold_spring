import { Link, useLocation } from "react-router-dom"
import {
  LayoutDashboard,
  Users,
  Shield,
  Menu,
  Key,
  FileText,
  LogIn,
  AlertCircle,
  Settings,
  Bell,
} from "lucide-react"

import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarSeparator,
} from "@/components/ui/sidebar"

const menuItems = [
  {
    title: "Dashboard",
    icon: LayoutDashboard,
    path: "/",
  },
  {
    title: "系统管理",
    icon: Settings,
    submenu: [
      { title: "用户管理", icon: Users, path: "/system/user" },
      { title: "角色管理", icon: Shield, path: "/system/role" },
      { title: "菜单管理", icon: Menu, path: "/system/menu" },
      { title: "权限管理", icon: Key, path: "/system/permission" },
    ],
  },
  {
    title: "日志监控",
    icon: FileText,
    submenu: [
      { title: "API日志", icon: LogIn, path: "/logs/api" },
      { title: "登录日志", icon: Bell, path: "/logs/login" },
      { title: "操作日志", icon: FileText, path: "/logs/operation" },
      { title: "异常日志", icon: AlertCircle, path: "/logs/error" },
    ],
  },
]

function AppSidebar() {
  const location = useLocation()

  return (
    <Sidebar collapsible="icon">
      <SidebarHeader>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton asChild size="lg">
              <Link to="/">
                <div className="flex aspect-square size-8 items-center justify-center rounded-lg bg-sidebar-primary text-sidebar-primary-foreground">
                  <LayoutDashboard className="size-4" />
                </div>
                <div className="grid flex-1 text-left text-sm leading-tight">
                  <span className="truncate font-semibold">Admin</span>
                  <span className="truncate text-xs">管理系统</span>
                </div>
              </Link>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarHeader>
      <SidebarSeparator />
      <SidebarContent>
        {menuItems.map((item, index) =>
          item.submenu ? (
            <SidebarGroup key={index}>
              <SidebarGroupLabel>
                <item.icon className="size-4" data-icon="inline-start" />
                {item.title}
              </SidebarGroupLabel>
              <SidebarGroupContent>
                <SidebarMenu>
                  {item.submenu.map((subItem, subIndex) => (
                    <SidebarMenuItem key={subIndex}>
                      <SidebarMenuButton
                        asChild
                        isActive={location.pathname === subItem.path}
                        tooltip={subItem.title}
                      >
                        <Link to={subItem.path}>
                          <subItem.icon
                            className="size-4"
                            data-icon="inline-start"
                          />
                          {subItem.title}
                        </Link>
                      </SidebarMenuButton>
                    </SidebarMenuItem>
                  ))}
                </SidebarMenu>
              </SidebarGroupContent>
            </SidebarGroup>
          ) : (
            <SidebarGroup key={index}>
              <SidebarGroupContent>
                <SidebarMenu>
                  <SidebarMenuItem>
                    <SidebarMenuButton
                      asChild
                      isActive={location.pathname === item.path}
                      tooltip={item.title}
                    >
                      <Link to={item.path!}>
                        <item.icon className="size-4" data-icon="inline-start" />
                        {item.title}
                      </Link>
                    </SidebarMenuButton>
                  </SidebarMenuItem>
                </SidebarMenu>
              </SidebarGroupContent>
            </SidebarGroup>
          )
        )}
      </SidebarContent>
      <SidebarFooter>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton tooltip="设置">
              <Settings className="size-4" data-icon="inline-start" />
              设置
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarFooter>
    </Sidebar>
  )
}

export default AppSidebar
