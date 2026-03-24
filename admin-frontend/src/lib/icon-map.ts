import { type LucideIcon } from "lucide-react"
import {
  // 基础
  LayoutDashboard, Home, Settings, Globe, Folder, FolderOpen, Menu, Search,
  // 用户/权限
  Users, User, UserCog, Shield, Key, Lock,
  // 日志/文件
  FileText, File, LogIn, AlertCircle, Clipboard, List,
  // 通知/消息
  Bell, Mail, MessageSquare, Send, Inbox,
  // 系统/监控
  Activity, Monitor, Cpu, Server, HardDrive, Database, Terminal, Wifi, Cloud,
  // 数据/图表
  BarChart, BarChart3, PieChart, TrendingUp, Percent,
  // 业务
  ShoppingCart, CreditCard, Wallet, Package, Ticket, BriefcaseBusiness, Building,
  // 内容/编辑
  BookOpen, PenLine, Image, Upload, Archive, Tag, Layers, Blocks,
  // 工具
  Wrench, Cog, Plug, Code, GitBranch, Zap, Box,
  // 其他
  Calendar, Clock, Map, MapPin, Phone, Link, Info, Star, Heart, Trophy,
  Check, ChevronRight, CircleDot, Table, Trash2,
} from "lucide-react"

/**
 * 图标名称 → Lucide 组件映射
 * 菜单管理和侧边栏共用
 */
export const iconMap: Record<string, LucideIcon> = {
  // 基础
  LayoutDashboard, Home, Settings, Globe, Folder, FolderOpen, Menu, Search,
  // 用户/权限
  Users, User, UserCog, Shield, Key, Lock,
  // 日志/文件
  FileText, File, LogIn, AlertCircle, Clipboard, List,
  // 通知/消息
  Bell, Mail, MessageSquare, Send, Inbox,
  // 系统/监控
  Activity, Monitor, Cpu, Server, HardDrive, Database, Terminal, Wifi, Cloud,
  // 数据/图表
  BarChart, BarChart3, PieChart, TrendingUp, Percent,
  // 业务
  ShoppingCart, CreditCard, Wallet, Package, Ticket, BriefcaseBusiness, Building,
  // 内容/编辑
  BookOpen, PenLine, Image, Upload, Archive, Tag, Layers, Blocks,
  // 工具
  Wrench, Cog, Plug, Code, GitBranch, Zap, Box,
  // 其他
  Calendar, Clock, Map, MapPin, Phone, Link, Info, Star, Heart, Trophy,
  Check, ChevronRight, CircleDot, Table, Trash2,
}

export function getIcon(iconName?: string): LucideIcon {
  if (!iconName) return Folder
  return iconMap[iconName] ?? Folder
}

export const iconOptions = Object.keys(iconMap)
