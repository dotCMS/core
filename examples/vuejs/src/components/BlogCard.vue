<script setup lang="ts">
import type { DotCMSBasicContentlet } from '@dotcms/types';
import { editContentlet } from '@dotcms/uve';
import { toPlain } from '@dotcms/vue';

import { useIsEditMode } from '@/composables/useIsEditMode';
import type { Blog } from '@/types/content';
import { formatDate } from '@/utils/formatDate';
import { imageLoader } from '@/utils/imageLoader';

const props = defineProps<{ blog: Blog }>();

const isEditMode = useIsEditMode();

const onEdit = () => editContentlet(toPlain(props.blog) as unknown as DotCMSBasicContentlet);
</script>

<template>
    <article
        class="group relative flex h-full flex-col overflow-hidden rounded-2xl border border-line bg-bg shadow-sm transition-shadow duration-300 hover:shadow-xl hover:shadow-primary-deep/5">
        <button
            v-if="isEditMode"
            type="button"
            class="absolute right-3 top-3 z-10 rounded-full bg-primary px-4 py-1.5 text-sm font-semibold text-bg shadow-md transition-colors hover:bg-primary-deep"
            @click="onEdit">
            Edit
        </button>

        <div class="relative aspect-[3/2] w-full overflow-hidden bg-surface">
            <img
                v-if="blog.inode"
                :src="imageLoader(blog.inode, 800)"
                :alt="blog.urlTitle || blog.title"
                class="absolute inset-0 h-full w-full object-cover transition-transform duration-700 ease-(--ease-out-quart) group-hover:scale-105" />
            <div v-else class="grid h-full place-items-center text-sm text-muted">No image</div>
        </div>

        <div class="flex grow flex-col p-5">
            <h3 class="font-display text-xl font-semibold leading-snug text-ink">
                <a
                    :href="blog.urlMap"
                    class="transition-colors after:absolute after:inset-0 hover:text-primary">
                    {{ blog.title }}
                </a>
            </h3>

            <p v-if="blog.teaser" class="mt-2 line-clamp-2 leading-relaxed text-muted">
                {{ blog.teaser }}
            </p>

            <time v-if="blog.modDate" class="mt-auto pt-4 text-sm text-muted">
                {{ formatDate(blog.modDate) }}
            </time>
        </div>
    </article>
</template>
