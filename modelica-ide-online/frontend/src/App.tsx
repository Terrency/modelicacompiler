import { Routes, Route } from 'react-router-dom'
import IDE from './components/IDE'
import ProjectList from './components/ProjectList'
import './App.css'

function App() {
  return (
    <div className="app">
      <Routes>
        <Route path="/" element={<ProjectList />} />
        <Route path="/project/:projectId" element={<IDE />} />
      </Routes>
    </div>
  )
}

export default App