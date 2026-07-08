<script setup lang="ts">
import { computed } from 'vue';

import type { DotCMSColumnContainer } from '@dotcms/types';
import {
    getContainersData,
    getContentletsInContainer,
    getDotContainerAttributes
} from '@dotcms/uve/internal';

import ContainerNotFound from './ContainerNotFound.vue';
import EmptyContainer from './EmptyContainer.vue';

import { useIsDevMode } from '../../composables/useIsDevMode';
import { useDotCMSPageContext } from '../../contexts/dotcms-page.context';
import Contentlet from '../Contentlet/Contentlet.vue';

/**
 * @internal
 * Renders a dotCMS container and the contentlets placed inside it. Container
 * metadata (`data-dot-*`) is editor-only and stripped from live output.
 */
const props = defineProps<{ container: DotCMSColumnContainer }>();

// ctx is a ComputedRef; reading `ctx.value.pageAsset` inside computeds keeps the
// layout reactive to live UVE page updates.
const ctx = useDotCMSPageContext();
const isDevMode = useIsDevMode();

const containerData = computed(() => getContainersData(ctx.value.pageAsset, props.container));
const contentlets = computed(() =>
    getContentletsInContainer(ctx.value.pageAsset, props.container)
);
const isEmpty = computed(() => contentlets.value.length === 0);

const dotAttributes = computed(() =>
    isDevMode.value && containerData.value ? getDotContainerAttributes(containerData.value) : {}
);

const serializedContainer = computed(() => JSON.stringify(containerData.value));
</script>

<template>
  <ContainerNotFound
    v-if="!containerData"
    :identifier="container.identifier"
  />
  <EmptyContainer
    v-else-if="isEmpty"
    v-bind="dotAttributes"
  />
  <div
    v-else
    v-bind="dotAttributes"
  >
    <Contentlet
      v-for="contentlet in contentlets"
      :key="contentlet.identifier"
      :contentlet="contentlet"
      :container="serializedContainer"
    />
  </div>
</template>
