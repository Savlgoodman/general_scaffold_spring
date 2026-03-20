import { BrowserRouter, Routes, Route } from "react-router-dom"
import MainLayout from "@/components/layout/MainLayout"
import Dashboard from "@/pages/Dashboard"
import Hello from "@/pages/Hello"
import Login from "@/pages/Login"
import UserManagement from "@/pages/system/UserManagement"
import RoleManagement from "@/pages/system/RoleManagement"
import PermissionManagement from "@/pages/system/PermissionManagement"
import ProtectedRoute from "@/components/auth/ProtectedRoute"
import { Toaster } from "@/components/ui/toaster"

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<MainLayout />}>
            <Route path="/" element={<Dashboard />} />
            <Route path="/hello" element={<Hello />} />
            <Route path="/system/user" element={<UserManagement />} />
            <Route path="/system/role" element={<RoleManagement />} />
            <Route path="/system/menu" element={<Dashboard />} />
            <Route path="/system/permission" element={<PermissionManagement />} />
            <Route path="/logs/api" element={<Dashboard />} />
            <Route path="/logs/login" element={<Dashboard />} />
            <Route path="/logs/operation" element={<Dashboard />} />
            <Route path="/logs/error" element={<Dashboard />} />
          </Route>
        </Route>
      </Routes>
      <Toaster />
    </BrowserRouter>
  )
}

export default App
