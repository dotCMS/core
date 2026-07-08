<script setup lang="ts">
import { computed, getCurrentInstance, type Component } from 'vue';

import type { DotCMSBasicContentlet } from '@dotcms/types';
import {
    CUSTOM_NO_COMPONENT,
    getAnalyticsContentletAttributes,
    getDotContentletAttributes
} from '@dotcms/uve/internal';

import { useCheckVisibleContent } from '../../composables/useCheckVisibleContent';
import { useIsAnalyticsActive } from '../../composables/useIsAnalyticsActive';
import { useIsDevMode } from '../../composables/useIsDevMode';
import { useDotCMSPageContext } from '../../contexts/dotcms-page.context';
import { CONTENTLET_CLASS } from './constants';

import FallbackComponent from '../FallbackComponent/FallbackComponent.vue';

/**
 * @internal
 * Renders a single contentlet: resolves the user component for its content type
 * (falling back to a dev-only placeholder) and emits the editor `data-dot-*`
 * metadata only when in development/edit mode.
 */
const props = defineProps<{
    contentlet: DotCMSBasicContentlet;
    /** JSON-serialized container data the contentlet belongs to. */
    container: string;
}>();

// Do not destructure: reading `ctx.userComponents` inside computeds keeps the
// component mapping reactive to live UVE updates.
const ctx = useDotCMSPageContext();
const isDevMode = useIsDevMode();
const isAnalyticsActive = useIsAnalyticsActive();

// Measure via the component's own root element instead of a template ref.
// The wrapper spreads `dotAttributes`, and attaching any ref there triggers
// Vue's "ref cannot be used on hoisted vnodes" warning when the compiler hoists
// the vnode; reading `$el` on mount avoids the ref entirely.
const instance = getCurrentInstance();
const haveContent = useCheckVisibleContent(
    () => instance?.proxy?.$el as Element | null | undefined
);

// In edit mode we emit the full editor metadata. In live mode we strip it,
// keeping only the minimal set Analytics needs (and only while it is active).
const dotAttributes = computed<Record<string, unknown>>(() => {
    if (isDevMode.value) {
        return {
            ...getDotContentletAttributes(props.contentlet, props.container),
            'data-dot-object': 'contentlet'
        };
    }

    if (isAnalyticsActive.value) {
        return getAnalyticsContentletAttributes(props.contentlet);
    }

    return {};
});

const style = computed(() =>
    isDevMode.value ? { minHeight: haveContent.value ? undefined : '4rem' } : {}
);

const userComponent = computed<Component | undefined>(
    () => ctx.userComponents[props.contentlet?.contentType]
);
const noComponent = computed<Component | undefined>(() => ctx.userComponents[CUSTOM_NO_COMPONENT]);
</script>

<template>
  <div
    v-bind="dotAttributes"
    :class="CONTENTLET_CLASS"
    :style="style"
  >
    <component
      :is="userComponent"
      v-if="userComponent"
      v-bind="contentlet"
    />
    <FallbackComponent
      v-else
      :user-no-component="noComponent"
      :contentlet="contentlet"
    />
  </div>
</template>
