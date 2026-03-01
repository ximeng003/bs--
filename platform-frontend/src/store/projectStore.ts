import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/api/request'

export interface Project {
  id: number
  name: string
  description: string
  teamId?: number
  role?: string
  createdBy: number
  createdAt: string
  updatedAt: string
}

export const useProjectStore = defineStore('project', () => {
  const currentProject = ref<Project | null>(null)
  const projectList = ref<Project[]>([])
  
  // Try to load from local storage
  const storedProjectId = localStorage.getItem('currentProjectId')
  
  const fetchProjects = async () => {
    try {
      const res: any = await request.get('/projects')
      projectList.value = res || []
      
      const stored = localStorage.getItem('currentProjectId')
      if (stored) {
        const found = projectList.value.find(p => p.id === parseInt(stored))
        if (found) {
          currentProject.value = found
        } else if (projectList.value.length > 0) {
          currentProject.value = projectList.value[0]
        }
      } else if (projectList.value.length > 0) {
        currentProject.value = projectList.value[0]
      }
      
      if (currentProject.value) {
        localStorage.setItem('currentProjectId', currentProject.value.id.toString())
      }
    } catch (error) {
      console.error('Failed to fetch projects', error)
    }
  }

  const setCurrentProject = (project: Project) => {
    currentProject.value = project
    localStorage.setItem('currentProjectId', project.id.toString())
    // Trigger reload to refresh all data with new project ID
    window.location.reload()
  }

  return {
    currentProject,
    projectList,
    fetchProjects,
    setCurrentProject
  }
})
