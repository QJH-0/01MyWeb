/**
 * 路由表与轻量导航守卫：`requiresAuth` / `guestOnly` 仅依据本地 access token 是否存在；
 * 细粒度 RBAC 在页面或接口层校验，避免在路由层重复维护权限列表。
 */
import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import ProfileView from '../views/ProfileView.vue'
import { hasAccessToken } from '../auth/token'

const AboutView = () => import('../views/AboutView.vue')
const ExperienceView = () => import('../views/ExperienceView.vue')
const ContactView = () => import('../views/ContactView.vue')
const ProjectsView = () => import('../views/ProjectsView.vue')
const ProjectDetailView = () => import('../views/ProjectDetailView.vue')
const AdminProjectListView = () => import('../views/projects/admin/ProjectListView.vue')
const AdminProjectEditView = () => import('../views/projects/admin/ProjectEditView.vue')
const BlogView = () => import('../views/blog/BlogView.vue')
const BlogDetailView = () => import('../views/blog/BlogDetailView.vue')
const AdminBlogListView = () => import('../views/blog/admin/BlogListView.vue')
const AdminBlogEditView = () => import('../views/blog/admin/BlogEditView.vue')
const AIView = () => import('../views/AIView.vue')
const SearchView = () => import('../views/SearchView.vue')
const NotFoundView = () => import('../views/NotFoundView.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { guestOnly: true },
    },
    {
      path: '/profile',
      name: 'profile',
      component: ProfileView,
      meta: { requiresAuth: true },
    },
    {
      path: '/about',
      name: 'about',
      component: AboutView,
    },
    {
      path: '/experience',
      name: 'experience',
      component: ExperienceView,
    },
    {
      path: '/contact',
      name: 'contact',
      component: ContactView,
    },
    {
      path: '/projects',
      name: 'projects',
      component: ProjectsView,
    },
    {
      path: '/projects/:id',
      name: 'project-detail',
      component: ProjectDetailView,
    },
    {
      path: '/admin/projects',
      name: 'admin-projects',
      component: AdminProjectListView,
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/projects/new',
      name: 'admin-project-new',
      component: AdminProjectEditView,
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/projects/:id/edit',
      name: 'admin-project-edit',
      component: AdminProjectEditView,
      meta: { requiresAuth: true },
    },
    {
      path: '/blog',
      name: 'blog',
      component: BlogView,
    },
    {
      path: '/blog/:id',
      name: 'blog-detail',
      component: BlogDetailView,
    },
    {
      path: '/admin/blogs',
      name: 'admin-blogs',
      component: AdminBlogListView,
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/blogs/new',
      name: 'admin-blog-new',
      component: AdminBlogEditView,
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/blogs/:id/edit',
      name: 'admin-blog-edit',
      component: AdminBlogEditView,
      meta: { requiresAuth: true },
    },
    {
      path: '/ai',
      name: 'ai',
      component: AIView,
    },
    {
      path: '/search',
      name: 'search',
      component: SearchView,
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: NotFoundView,
    },
  ],
})

router.beforeEach((to) => {
  // 无 token 时拦截受保护路由；登录页带回跳地址，便于深链接场景。
  if (to.meta.requiresAuth && !hasAccessToken()) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.guestOnly && hasAccessToken()) {
    return { name: 'profile' }
  }

  return true
})

export default router
