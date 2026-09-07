<script setup lang="ts">
import { DotCMSLayoutBody, useEditableDotCMSPage } from '@dotcms/vue';
import { computed } from 'vue';

import { pageComponents } from '@/components/content-types';
import Footer from '@/components/footer/Footer.vue';
import Header from '@/components/header/Header.vue';
import { dotCMSMode } from '@/config/dotcms.config';
import type { PageExtraContent } from '@/types/content';
import type { DotCMSPageContent } from '@/utils/pageResponse';

/**
 * Renders a resolved dotCMS page. Kept separate from the fetching view so
 * `useEditableDotCMSPage` receives a concrete page response synchronously in
 * setup (and re-inits the UVE bridge when the parent remounts it per route).
 */
const props = defineProps<{ pageResponse: DotCMSPageContent }>();

// Pass a getter so the composable re-initializes the UVE when the page changes
// (e.g. on route navigation) — no per-route component remount needed.
const page = useEditableDotCMSPage(() => props.pageResponse);

const pageAsset = computed(() => page.value?.pageAsset);
const content = computed(() => (page.value?.content ?? {}) as PageExtraContent);
const navigation = computed(() => content.value.navigation);
</script>

<template>
    <div v-if="pageAsset" class="flex min-h-dvh flex-col bg-bg">
        <Header v-if="pageAsset.layout?.header" :nav-items="navigation?.children" />

        <main class="flex-1">
            <div
                class="container mx-auto flex flex-col gap-16 px-4 py-10 sm:px-6 sm:py-14 md:gap-24">
                <DotCMSLayoutBody
                    :page="pageAsset"
                    :components="pageComponents"
                    :mode="dotCMSMode" />
            </div>
        </main>

        <Footer
            v-if="pageAsset.layout?.footer"
            :blogs="content.blogs"
            :destinations="content.destinations" />
    </div>
</template>
