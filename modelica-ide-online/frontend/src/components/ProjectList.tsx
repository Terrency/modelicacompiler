import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../services/api'
import './ProjectList.css'

interface Project {
  id: string
  name: string
  description?: string
  createdAt: number
  updatedAt: number
}

export default function ProjectList() {
  const navigate = useNavigate()
  const [projects, setProjects] = useState<Project[]>([])
  const [loading, setLoading] = useState(true)
  const [showNewProject, setShowNewProject] = useState(false)
  const [newProjectName, setNewProjectName] = useState('')

  useEffect(() => {
    loadProjects()
  }, [])

  const loadProjects = async () => {
    try {
      const data = await api.getProjects()
      setProjects(data)
    } catch (error) {
      console.error('Failed to load projects:', error)
    } finally {
      setLoading(false)
    }
  }

  const createProject = async () => {
    if (!newProjectName.trim()) return

    try {
      const project = await api.createProject({
        name: newProjectName,
        description: ''
      })
      navigate(`/project/${project.id}`)
    } catch (error) {
      console.error('Failed to create project:', error)
    }
  }

  const deleteProject = async (id: string) => {
    if (!confirm('Are you sure you want to delete this project?')) return

    try {
      await api.deleteProject(id)
      setProjects(projects.filter(p => p.id !== id))
    } catch (error) {
      console.error('Failed to delete project:', error)
    }
  }

  if (loading) {
    return <div className="loading">Loading...</div>
  }

  return (
    <div className="project-list-container">
      <header className="page-header">
        <h1>Modelica IDE Online</h1>
        <button
          className="btn primary"
          onClick={() => setShowNewProject(true)}
        >
          + New Project
        </button>
      </header>

      {showNewProject && (
        <div className="modal-overlay">
          <div className="modal">
            <h2>Create New Project</h2>
            <input
              type="text"
              placeholder="Project name"
              value={newProjectName}
              onChange={e => setNewProjectName(e.target.value)}
              autoFocus
            />
            <div className="modal-actions">
              <button className="btn" onClick={() => setShowNewProject(false)}>
                Cancel
              </button>
              <button className="btn primary" onClick={createProject}>
                Create
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="projects-grid">
        {projects.map(project => (
          <div key={project.id} className="project-card">
            <div
              className="project-info"
              onClick={() => navigate(`/project/${project.id}`)}
            >
              <h3>{project.name}</h3>
              <p>{project.description || 'No description'}</p>
              <span className="project-date">
                Updated: {new Date(project.updatedAt).toLocaleDateString()}
              </span>
            </div>
            <div className="project-actions">
              <button
                className="btn icon"
                onClick={() => navigate(`/project/${project.id}`)}
                title="Open"
              >
                📂
              </button>
              <button
                className="btn icon danger"
                onClick={() => deleteProject(project.id)}
                title="Delete"
              >
                🗑️
              </button>
            </div>
          </div>
        ))}
      </div>

      {projects.length === 0 && (
        <div className="empty-state">
          <h2>No projects yet</h2>
          <p>Create your first Modelica project to get started!</p>
          <button className="btn primary" onClick={() => setShowNewProject(true)}>
            Create Project
          </button>
        </div>
      )}
    </div>
  )
}