<script setup lang="ts">
import { enableBlockEditorInline } from '@dotcms/uve';
import {
    DotCMSBlockEditorRenderer,
    DotCMSEditableText,
    useEditableDotCMSPage,
    type CustomRenderer
} from '@dotcms/vue';
import { computed } from 'vue';

import ActivityBlock from '@/components/block-renderers/ActivityBlock.vue';
import DestinationBlock from '@/components/block-renderers/DestinationBlock.vue';
import ProductBlock from '@/components/block-renderers/ProductBlock.vue';
import Footer from '@/components/footer/Footer.vue';
import Header from '@/components/header/Header.vue';
import { useIsEditMode } from '@/composables/useIsEditMode';
import type { PageExtraContent } from '@/types/content';
import { imageLoader } from '@/utils/imageLoader';
import type { PageResponse } from '@/utils/pageResponse';

const props = defineProps<{ pageResponse: PageResponse }>();

const page = useEditableDotCMSPage(props.pageResponse as never);
const isEditMode = useIsEditMode();

const pageAsset = computed(() => page.value?.pageAsset);
const content = computed(() => (page.value?.content ?? {}) as PageExtraContent);
const navigation = computed(() => content.value.navigation);

// The detail contentlet resolved by the URL (a Blog).
const urlContentMap = computed(
    () =>
        pageAsset.value?.urlContentMap as
            | (Record<string, unknown> & {
                  title?: string;
                  image?: { identifier?: string };
                  blogContent?: unknown;
              })
            | undefined
);

const blogContent = computed(() => urlContentMap.value?.blogContent as never);

const blockEditorClass = computed(() =>
    [
        'prose lg:prose-xl prose-a:text-blue-600',
        isEditMode.value ? 'border-2 border-solid border-cyan-400 cursor-pointer' : ''
    ]
        .filter(Boolean)
        .join(' ')
);

const customRenderers: CustomRenderer = {
    Activity: ActivityBlock,
    Product: ProductBlock,
    Destination: DestinationBlock
};

const onBlockClick = () => {
    if (isEditMode.value && urlContentMap.value) {
        enableBlockEditorInline(urlContentMap.value as never, 'blogContent');
    }
};
</script>

<template>
    <div class="flex flex-col gap-6 bg-slate-50">
        <Header v-if="pageAsset?.layout?.header" :nav-items="navigation?.children" />

        <main class="flex flex-col gap-8 m-auto max-w-3xl w-full px-4 py-10">
            <h1 class="text-4xl font-bold">
                <DotCMSEditableText
                    v-if="urlContentMap"
                    :contentlet="(urlContentMap as never)"
                    field-name="title" />
            </h1>

            <div
                v-if="urlContentMap?.image?.identifier"
                class="relative w-full h-80 overflow-hidden rounded-2xl">
                <img
                    class="object-cover w-full h-full"
                    :src="imageLoader(urlContentMap.image.identifier, 1200)"
                    alt="Activity Image" />
            </div>

            <div @click="onBlockClick">
                <DotCMSBlockEditorRenderer
                    :blocks="blogContent"
                    :class-name="blockEditorClass"
                    :custom-renderers="customRenderers" />
            </div>
        </main>

        <Footer
            v-if="pageAsset?.layout?.footer"
            :blogs="content.blogs"
            :destinations="content.destinations" />
    </div>
</template>
