import { BrowserRouter, Routes, Route } from "react-router-dom"
import MainLayout from "@/components/layout/MainLayout"
import Dashboard from "@/pages/Dashboard"
import Hello from "@/pages/Hello"

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<MainLayout />}>
          <Route path="/" element={<Dashboard />} />
          <Route path="/hello" element={<Hello />} />
          <Route path="/system/user" element={<Dashboard />} />
          <Route path="/system/role" element={<Dashboard />} />
          <Route path="/system/menu" element={<Dashboard />} />
          <Route path="/system/permission" element={<Dashboard />} />
          <Route path="/logs/api" element={<Dashboard />} />
          <Route path="/logs/login" element={<Dashboard />} />
          <Route path="/logs/operation" element={<Dashboard />} />
          <Route path="/logs/error" element={<Dashboard />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

export default App
