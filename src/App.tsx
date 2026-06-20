import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import DashboardPage from './pages/DashboardPage'
import FenyongPage from './pages/FenyongPage'

const App: React.FC = () => {
  return (
    <Routes>
      <Route path="/" element={<DashboardPage />} />
      <Route path="/fenyong" element={<FenyongPage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
