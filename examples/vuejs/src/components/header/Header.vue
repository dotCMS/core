<script setup lang="ts">
import { computed, ref } from 'vue';
import { RouterLink, useRoute } from 'vue-router';

import ReorderMenuButton from '@/components/editor/ReorderMenuButton.vue';
import { useIsEditMode } from '@/composables/useIsEditMode';
import { cn } from '@/lib/utils';
import type { NavItem } from '@/types/content';

const props = defineProps<{ navItems?: NavItem[] }>();

const isEditMode = useIsEditMode();
const isMenuOpen = ref(false);
const route = useRoute();

// Prepend a synthetic "Home" entry, matching the React header.
const items = computed(() => [
    { folder: '__home', href: '/', target: '', title: 'Home' },
    ...(props.navItems ?? [])
]);

const linkClass = (href: string) =>
    cn(
        'relative rounded-full px-3 py-2 text-sm font-medium transition-colors',
        "after:absolute after:inset-x-3 after:-bottom-px after:h-0.5 after:origin-left after:scale-x-0 after:rounded-full after:bg-accent after:transition-transform after:duration-300 after:content-['']",
        'hover:text-primary hover:after:scale-x-100',
        route.path === href ? 'text-primary after:scale-x-100' : 'text-ink'
    );
</script>

<template>
    <header
        class="sticky top-0 z-(--z-sticky) border-b border-line bg-bg/85 backdrop-blur-md">
        <div
            class="container mx-auto flex h-16 items-center justify-between gap-4 px-4 sm:px-6">
            <div class="flex shrink-0 items-center gap-3">
                <RouterLink
                    to="/"
                    class="font-display text-2xl font-semibold tracking-tight text-primary-deep">
                    TravelLux
                </RouterLink>
                <ReorderMenuButton v-if="isEditMode" />
            </div>

            <div class="flex shrink-0 items-center gap-3 sm:gap-5">
                <nav aria-label="Primary" class="hidden md:flex">
                    <RouterLink
                        v-for="item in items"
                        :key="item.folder"
                        :to="item.href ?? '/'"
                        :target="item.target || undefined"
                        :class="linkClass(item.href ?? '/')">
                        {{ item.title }}
                    </RouterLink>
                </nav>
                <button
                    type="button"
                    :aria-label="isMenuOpen ? 'Close menu' : 'Open menu'"
                    :aria-expanded="isMenuOpen"
                    class="grid size-10 place-items-center rounded-full text-ink transition-colors hover:bg-surface md:hidden"
                    @click="isMenuOpen = !isMenuOpen">
                    <svg
                        width="22"
                        height="22"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="1.8"
                        stroke-linecap="round"
                        aria-hidden="true">
                        <template v-if="isMenuOpen">
                            <line x1="6" y1="6" x2="18" y2="18" />
                            <line x1="6" y1="18" x2="18" y2="6" />
                        </template>
                        <template v-else>
                            <line x1="3" y1="7" x2="21" y2="7" />
                            <line x1="3" y1="12" x2="21" y2="12" />
                            <line x1="3" y1="17" x2="21" y2="17" />
                        </template>
                    </svg>
                </button>
            </div>
        </div>

        <nav
            v-if="isMenuOpen"
            aria-label="Primary"
            class="flex flex-col gap-1 border-t border-line px-4 py-3 md:hidden">
            <RouterLink
                v-for="item in items"
                :key="item.folder"
                :to="item.href ?? '/'"
                :target="item.target || undefined"
                :class="linkClass(item.href ?? '/')"
                @click="isMenuOpen = false">
                {{ item.title }}
            </RouterLink>
        </nav>
    </header>
</template>
