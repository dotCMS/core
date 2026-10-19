<script setup lang="ts">
import { computed, shallowRef, ref, onMounted } from 'vue';

import BlogListingRenderer from '@/components/BlogListingRenderer.vue';
import { getDotCMSPage } from '@/utils/getDotCMSPage';
import {
    DEV_ERROR_COPY,
    getErrorDetails,
    isPageError,
    type DotCMSPageContent,
    type ErrorDetails
} from '@/utils/pageResponse';

// shallowRef: keep the page response plain so the UVE bridge can clone it.
const pageResponse = shallowRef<DotCMSPageContent | null>(null);
// Development only: getErrorDetails leaves `message` unset in production.
const errorDetails = shallowRef<ErrorDetails>({});
const loadError = computed(() => Boolean(errorDetails.value.message) && errorDetails.value.status !== 404);
const loading = ref(true);

onMounted(async () => {
    const response = await getDotCMSPage('/blog');
    if (isPageError(response)) {
        errorDetails.value = getErrorDetails(response.error);
    } else {
        pageResponse.value = response;
    }
    loading.value = false;
});
</script>

<template>
    <BlogListingRenderer v-if="pageResponse" :page-response="pageResponse" />
    <div v-else-if="loading" class="flex min-h-dvh items-center justify-center bg-slate-50">
        <p class="text-muted">Loading…</p>
    </div>
    <div v-else class="flex min-h-dvh flex-col items-center justify-center bg-slate-50 p-8">
        <template v-if="loadError">
            <h1 class="text-center text-h2 font-display">{{ DEV_ERROR_COPY.heading }}</h1>
            <p class="mt-2 text-center text-muted">{{ DEV_ERROR_COPY.body }}</p>
        </template>
        <p v-else class="text-muted">The blog is unavailable right now.</p>
        <pre
            v-if="errorDetails.message"
            class="mt-6 max-w-xl whitespace-pre-wrap break-words rounded-lg border border-primary/30 p-4 text-left font-mono text-sm leading-relaxed text-ink">{{ errorDetails.message }}</pre>
    </div>
</template>
