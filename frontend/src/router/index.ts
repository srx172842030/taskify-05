import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/pages/home/HomeView.vue'),
    },
    {
      path: '/projects',
      name: 'projects',
      component: () => import('@/pages/projects/ProjectListPage.vue'),
    },
    {
      path: '/projects/:id',
      name: 'project-detail',
      component: () => import('@/pages/projects/ProjectDetailPage.vue'),
    },
    {
      path: '/design',
      name: 'design',
      component: () => import('@/pages/design/DesignGallery.vue'),
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/pages/error/NotFound.vue'),
    },
  ],
})

export default router
