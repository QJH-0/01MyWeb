import http, { type ApiResponse } from './http'

export interface ContentPage {
  title: string
  summary: string
  sections: unknown[]
  updatedAt: string
}

export async function fetchHomeContent() {
  const response = await http.get<ApiResponse<ContentPage>>('/api/content/home')
  return response.data
}

export async function fetchAboutContent() {
  const response = await http.get<ApiResponse<ContentPage>>('/api/content/about')
  return response.data
}

export async function fetchExperienceContent() {
  const response = await http.get<ApiResponse<ContentPage>>('/api/content/experience')
  return response.data
}

