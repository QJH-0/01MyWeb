import { createRouter, createWebHistory } from 'vue-router'

import HomeView from '../views/HomeView.vue'
import AboutView from '../views/AboutView.vue'
import ExperienceView from '../views/ExperienceView.vue'
import ContactView from '../views/ContactView.vue'
import ProjectsView from '../views/ProjectsView.vue'
import AdminProjectsView from '../views/AdminProjectsView.vue'
import BlogView from '../views/BlogView.vue'
import BlogDetailView from '../views/BlogDetailView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/about', name: 'about', component: AboutView },
    { path: '/experience', name: 'experience', component: ExperienceView },
    { path: '/contact', name: 'contact', component: ContactView },
    // M2/M3 占位，避免首页入口跳转后产生 404
    { path: '/projects', name: 'projects', component: ProjectsView },
    { path: '/admin/projects', name: 'adminProjects', component: AdminProjectsView },
    { path: '/blog', name: 'blog', component: BlogView },
    { path: '/blog/:id', name: 'blogDetail', component: BlogDetailView }
  ]
})

export default router

