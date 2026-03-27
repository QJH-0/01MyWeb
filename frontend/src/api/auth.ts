import http, { type ApiResponse } from './http'

export interface RegisterRequest {
  username: string
  password: string
  captchaToken?: string
}

export interface RegisterResponse {
  userId: number
  username: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface AuthTokenResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

export interface MeResponse {
  userId: number
  username: string
  roles: string[]
}

export async function register(payload: RegisterRequest) {
  const response = await http.post<ApiResponse<RegisterResponse>>('/api/auth/register', payload)
  return response.data
}

export async function login(payload: LoginRequest) {
  const response = await http.post<ApiResponse<AuthTokenResponse>>('/api/auth/login', payload)
  return response.data
}

export async function me() {
  const response = await http.get<ApiResponse<MeResponse>>('/api/auth/me')
  return response.data
}

export async function refreshToken(refreshToken: string) {
  const response = await http.post<ApiResponse<AuthTokenResponse>>('/api/auth/refresh', { refreshToken })
  return response.data
}

export async function logout(refreshToken: string) {
  const response = await http.post<ApiResponse<null>>('/api/auth/logout', { refreshToken })
  return response.data
}
