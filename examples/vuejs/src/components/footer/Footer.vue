<script setup lang="ts">
import { RouterLink } from 'vue-router';

import FooterBlogs from './FooterBlogs.vue';
import FooterDestinations from './FooterDestinations.vue';

import type { Blog, Destination } from '@/types/content';

defineProps<{ blogs?: Blog[]; destinations?: Destination[] }>();

const currentYear = new Date().getFullYear();
</script>

<template>
    <footer class="bg-primary-deep text-bg">
        <div
            class="container mx-auto grid gap-12 px-4 py-16 sm:px-6 md:grid-cols-[1.4fr_1fr_1fr] md:py-20 lg:gap-16">
            <div class="flex flex-col gap-5">
                <RouterLink to="/" class="font-display text-3xl font-semibold tracking-tight">
                    TravelLux
                </RouterLink>
                <p class="max-w-sm text-[0.95rem] leading-relaxed text-bg/75">
                    A travel journal of places worth the journey. Our writers and photographers
                    share first-hand guides to where to go next, and how to make the most of it
                    once you arrive.
                </p>
            </div>

            <FooterBlogs :blogs="blogs" />
            <FooterDestinations :destinations="destinations" />
        </div>

        <div class="border-t border-bg/15">
            <div
                class="container mx-auto flex flex-col items-center justify-between gap-2 px-4 py-6 text-sm text-bg/60 sm:flex-row sm:px-6">
                <p>&copy; {{ currentYear }} TravelLux. All rights reserved.</p>
                <p>Built with dotCMS &amp; Vue</p>
            </div>
        </div>
    </footer>
</template>
