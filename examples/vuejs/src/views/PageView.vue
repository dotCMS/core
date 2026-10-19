<script setup lang="ts">
import { computed, shallowRef, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import PageRenderer from '@/components/PageRenderer.vue';
import { getDotCMSPage } from '@/utils/getDotCMSPage';
import {
    DEV_ERROR_COPY,
    getErrorDetails,
    isPageError,
    type DotCMSPageContent,
    type ErrorDetails
} from '@/utils/pageResponse';

const route = useRoute();
const router = useRouter();

// shallowRef: keep the page response a plain object. A deep `ref` would wrap it
// (and everything nested) in reactive Proxies, which the UVE bridge cannot
// structured-clone when it posts the page to the editor.
const pageResponse = shallowRef<DotCMSPageContent | null>(null);
const notFound = ref(false);
// Development only: getErrorDetails leaves `message` unset in production.
const errorDetails = shallowRef<ErrorDetails>({});
// A failure other than a 404 is not a missing page, so with a detail to show it gets its own copy.
const loadError = computed(() => Boolean(errorDetails.value.message) && errorDetails.value.status !== 404);
const loading = ref(true);

const buildPath = () => {
    const match = route.params.pathMatch;
    const segments = Array.isArray(match) ? match : match ? [match] : [];

    return segments.length ? `/${segments.join('/')}` : '/';
};

// `onCleanup` runs when the route changes again before the fetch resolves, so a
// slower earlier request can't overwrite state for the newer route.
const loadPage = async (_path: string, _prev: string | undefined, onCleanup: (fn: () => void) => void) => {
    let cancelled = false;
    onCleanup(() => {
        cancelled = true;
    });

    loading.value = true;
    notFound.value = false;
    errorDetails.value = {};
    pageResponse.value = null;

    const response = await getDotCMSPage(buildPath());
    if (cancelled) {
        return;
    }

    if (isPageError(response)) {
        notFound.value = true;
        errorDetails.value = getErrorDetails(response.error);
        loading.value = false;

        return;
    }

    const vanityUrl = response.pageAsset?.vanityUrl;
    if (vanityUrl?.action && vanityUrl.action > 200 && vanityUrl.forwardTo) {
        router.replace(vanityUrl.forwardTo);

        return;
    }

    if (!response.pageAsset) {
        notFound.value = true;
        loading.value = false;

        return;
    }

    pageResponse.value = response;
    loading.value = false;
};

watch(() => route.fullPath, loadPage, { immediate: true });
</script>

<template>
    <PageRenderer v-if="pageResponse" :page-response="pageResponse" />

    <div
        v-else-if="notFound"
        class="flex min-h-dvh items-center justify-center bg-bg p-8">
        <div class="text-center">
            <template v-if="loadError">
                <h1 class="text-h2 font-display">{{ DEV_ERROR_COPY.heading }}</h1>
                <p class="mt-2 text-muted">{{ DEV_ERROR_COPY.body }}</p>
            </template>
            <template v-else>
                <h1 class="text-h2 font-display">Page not found</h1>
                <p class="mt-2 text-muted">We couldn't find the page you were looking for.</p>
            </template>
            <pre
                v-if="errorDetails.message"
                class="mx-auto mt-6 max-w-xl whitespace-pre-wrap break-words rounded-lg border border-primary/30 p-4 text-left font-mono text-sm leading-relaxed text-ink">{{ errorDetails.message }}</pre>
            <RouterLink to="/" class="mt-6 inline-block text-primary underline">
                Back home
            </RouterLink>
        </div>
    </div>

    <div v-else-if="loading" class="flex min-h-dvh items-center justify-center bg-bg">
        <p class="text-muted">Loading…</p>
    </div>
</template>
