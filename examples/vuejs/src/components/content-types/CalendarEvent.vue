<script setup lang="ts">
import { computed } from 'vue';

import type { DotCMSImage } from '@/types/content';
import { imageLoader } from '@/utils/imageLoader';

interface CalendarActivity {
    title?: string;
    urlMap?: string;
}

interface CalendarLocation {
    title?: string;
    url?: string;
    activities?: CalendarActivity[];
}

const props = defineProps<{
    image?: DotCMSImage;
    title?: string;
    urlMap: string;
    description?: string;
    location?: CalendarLocation[];
}>();

const extracted = computed(() => {
    const locations: Omit<CalendarLocation, 'activities'>[] = [];
    let activities: CalendarActivity[] = [];

    (props.location ?? []).forEach(({ activities: acts, ...location }) => {
        activities = activities.concat(acts ?? []);
        locations.push(location);
    });

    return { locations, activities };
});

const imageSrc = computed(() =>
    props.image?.identifier ? imageLoader(props.image.identifier, 800) : ''
);
</script>

<template>
    <article
        class="flex w-full flex-col overflow-hidden rounded-2xl border border-line bg-bg shadow-sm sm:flex-row">
        <div class="relative h-56 shrink-0 bg-surface sm:h-auto sm:w-2/5">
            <img
                v-if="imageSrc"
                :src="imageSrc"
                :alt="title ?? ''"
                class="absolute inset-0 h-full w-full object-cover" />
        </div>
        <div class="flex flex-col gap-4 p-6 sm:p-8">
            <h3 class="font-display text-2xl font-semibold leading-snug text-ink">{{ title }}</h3>

            <div
                v-if="extracted.locations.length && extracted.locations[0]?.title"
                class="flex flex-wrap items-center gap-2">
                <span class="text-sm font-semibold text-muted">Locations</span>
                <a
                    v-for="(loc, index) in extracted.locations"
                    :key="index"
                    :href="loc.url ?? ''"
                    class="rounded-full bg-primary-tint px-2.5 py-0.5 text-xs font-medium text-primary transition-colors hover:bg-primary hover:text-bg">
                    {{ loc.title }}
                </a>
            </div>

            <div
                v-if="extracted.activities.length && extracted.activities[0]?.title"
                class="flex flex-wrap items-center gap-2">
                <span class="text-sm font-semibold text-muted">Activities</span>
                <a
                    v-for="(act, index) in extracted.activities.slice(0, 3)"
                    :key="index"
                    :href="act.urlMap ?? ''"
                    class="rounded-full bg-surface px-2.5 py-0.5 text-xs font-medium text-ink transition-colors hover:bg-surface-2">
                    {{ act.title }}
                </a>
            </div>

            <!-- eslint-disable-next-line vue/no-v-html -- renders CMS-authored copy -->
            <div class="line-clamp-3 leading-relaxed text-muted" v-html="description ?? ''" />

            <a
                :href="urlMap"
                class="mt-1 inline-flex w-fit items-center gap-1.5 font-semibold text-primary transition-colors hover:text-primary-deep">
                Learn more
                <span aria-hidden="true">→</span>
            </a>
        </div>
    </article>
</template>
