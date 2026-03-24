import { createRouter, createWebHistory } from 'vue-router'

const HomeView = () => import('../views/HomeView.vue')
const AboutView = () => import('../views/AboutView.vue')
const ExperienceView = () => import('../views/ExperienceView.vue')
const ContactView = () => import('../views/ContactView.vue')
const ProjectsView = () => import('../views/ProjectsView.vue')
const AdminProjectsView = () => import('../views/AdminProjectsView.vue')
const BlogView = () => import('../views/BlogView.vue')
const BlogDetailView = () => import('../views/BlogDetailView.vue')
const AdminBlogsView = () => import('../views/AdminBlogsView.vue')
const SearchView = () => import('../views/SearchView.vue')
const AiAssistantView = () => import('../views/AiAssistantView.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/about', name: 'about', component: AboutView },
    { path: '/experience', name: 'experience', component: ExperienceView },
    { path: '/contact', name: 'contact', component: ContactView },
    { path: '/projects', name: 'projects', component: ProjectsView },
    { path: '/admin/projects', name: 'adminProjects', component: AdminProjectsView },
    { path: '/blog', name: 'blog', component: BlogView },
    { path: '/blog/:id', name: 'blogDetail', component: BlogDetailView },
    { path: '/admin/blogs', name: 'adminBlogs', component: AdminBlogsView },
    { path: '/search', name: 'search', component: SearchView },
    { path: '/ai', name: 'ai', component: AiAssistantView }
  ],
  scrollBehavior() {
    return { top: 0 }
  }
})

export default router

