import { createRouter, createWebHistory } from 'vue-router';

/**
 * Routes mirror the Next.js example:
 * - `/blog` — the blog listing
 * - `/blog/post/:slug` — a blog/detail page (rendered from `urlContentMap`)
 * - everything else — a catch-all that resolves the dotCMS page for the path
 *
 * Data fetching happens inside each view's `setup` (there is no SSR here).
 */
export const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/blog',
            name: 'blog-listing',
            component: () => import('@/views/BlogListingView.vue')
        },
        {
            path: '/blog/post/:slug(.*)*',
            name: 'blog-detail',
            component: () => import('@/views/DetailPageView.vue')
        },
        {
            path: '/:pathMatch(.*)*',
            name: 'page',
            component: () => import('@/views/PageView.vue')
        }
    ]
});
