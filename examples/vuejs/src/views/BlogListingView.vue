<script setup lang="ts">
import { shallowRef, ref, onMounted } from 'vue';

import BlogListingRenderer from '@/components/BlogListingRenderer.vue';
import { getDotCMSPage } from '@/utils/getDotCMSPage';
import { isPageError, type PageResponse } from '@/utils/pageResponse';

// shallowRef: keep the page response plain so the UVE bridge can clone it.
const pageResponse = shallowRef<PageResponse | null>(null);
const loading = ref(true);

onMounted(async () => {
    const response = await getDotCMSPage('/blog');
    pageResponse.value = isPageError(response) ? null : response;
    loading.value = false;
});
</script>

<template>
    <BlogListingRenderer v-if="pageResponse" :page-response="pageResponse" />
    <div v-else-if="loading" class="flex min-h-dvh items-center justify-center bg-slate-50">
        <p class="text-muted">Loading…</p>
    </div>
    <div v-else class="flex min-h-dvh items-center justify-center bg-slate-50 p-8">
        <p class="text-muted">The blog is unavailable right now.</p>
    </div>
</template>
