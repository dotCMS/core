<script setup lang="ts">
import { computed } from 'vue';

import EditButton from '@/components/editor/EditButton.vue';
import type { Destination } from '@/types/content';
import { imageLoader } from '@/utils/imageLoader';

const props = defineProps<{ destinations?: Destination[] }>();

const destinations = computed(() => props.destinations ?? []);

const imageOf = (destination: Destination) =>
    typeof destination.image === 'string' ? imageLoader(destination.image, 800) : '';
</script>

<template>
    <p v-if="!destinations.length" class="py-12 text-center text-muted">No destinations yet.</p>

    <section v-else class="flex flex-col gap-10">
        <header class="flex max-w-2xl flex-col gap-3">
            <span class="eyebrow">Where to next</span>
            <h2 class="font-display text-h2 font-semibold text-ink">
                Destinations worth the journey
            </h2>
            <p class="text-lg leading-relaxed text-muted">
                Hand-picked places our writers keep returning to, with the experiences that make
                each one worth the trip.
            </p>
        </header>

        <div class="grid grid-cols-[repeat(auto-fit,minmax(min(100%,18rem),1fr))] gap-6">
            <article
                v-for="destination in destinations"
                :key="destination.identifier"
                class="group relative flex flex-col overflow-hidden rounded-2xl border border-line bg-bg shadow-sm transition-shadow duration-300 hover:shadow-xl hover:shadow-primary-deep/5">
                <EditButton :contentlet="destination" />
                <div class="relative h-60 overflow-hidden">
                    <img
                        v-if="imageOf(destination)"
                        :src="imageOf(destination)"
                        :alt="destination.title"
                        class="absolute inset-0 h-full w-full object-cover transition-transform duration-700 ease-(--ease-out-quart) group-hover:scale-105" />
                    <div
                        aria-hidden="true"
                        class="absolute inset-0 bg-gradient-to-t from-black/40 to-transparent opacity-0 transition-opacity duration-300 group-hover:opacity-100" />
                    <span
                        v-if="destination.selectValue"
                        class="absolute left-4 top-4 rounded-full bg-primary-deep/85 px-3 py-1 text-xs font-semibold uppercase tracking-wide text-bg backdrop-blur-sm">
                        {{ destination.selectValue }}
                    </span>
                </div>

                <div class="flex flex-1 flex-col p-6">
                    <h3 class="font-display text-2xl font-semibold leading-tight text-ink">
                        <a
                            :href="destination.url ?? '#'"
                            class="transition-colors after:absolute after:inset-0 hover:text-primary">
                            {{ destination.title }}
                        </a>
                    </h3>
                    <p class="mt-3 line-clamp-3 leading-relaxed text-muted">
                        {{ destination.shortDescription }}
                    </p>

                    <ul
                        v-if="destination.activities && destination.activities.length"
                        class="mt-5 flex flex-wrap gap-2 border-t border-line pt-5">
                        <li
                            v-for="(activity, index) in destination.activities"
                            :key="index"
                            class="rounded-full bg-surface px-3 py-1 text-xs font-medium text-ink">
                            {{ activity }}
                        </li>
                    </ul>
                </div>
            </article>
        </div>
    </section>
</template>
