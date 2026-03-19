import { BrowserRouter, Routes, Route } from "react-router-dom"
import Hello from "@/pages/Hello"

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/hello" element={<Hello />} />
        <Route path="/" element={<Hello />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
