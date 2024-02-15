import { createRouter, createWebHistory } from 'vue-router'
import AllPages from '../views/AllPages.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/:slug',
      name: 'AllRoutes',
      component: AllPages
    }
  ]
})

export default router
