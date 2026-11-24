<script setup lang="ts">
import { computed, getCurrentInstance, type Component } from 'vue';

import type { DotCMSBasicContentlet } from '@dotcms/types';
import {
    CUSTOM_NO_COMPONENT,
    getAnalyticsContentletAttributes,
    getDotContentletAttributes
} from '@dotcms/uve/internal';

import { useCheckVisibleContent } from '../../composables/useCheckVisibleContent';
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

// ctx is a ComputedRef; dev-mode and analytics are resolved once at the layout
// root and shared here, so this contentlet does no per-instance re-resolution.
const ctx = useDotCMSPageContext();
const isDevMode = computed(() => ctx.value.isDevMode);

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
    if (ctx.value.isDevMode) {
        return {
            ...getDotContentletAttributes(props.contentlet, props.container),
            'data-dot-object': 'contentlet'
        };
    }

    if (ctx.value.isAnalyticsActive) {
        return getAnalyticsContentletAttributes(props.contentlet);
    }

    return {};
});

const style = computed(() =>
    isDevMode.value ? { minHeight: haveContent.value ? undefined : '4rem' } : {}
);

// A per-contentlet named slot on DotCMSLayoutBody (`#contentlet-<identifier>`)
// wins over the mapped component — the Vue analog of the React SDK's `slots`.
// Wrap the slot in a functional component so it can be rendered via
// `<component :is>` alongside the user-component branch; the contentlet is passed
// as the slot's scope prop (`v-slot="{ contentlet }"`).
const contentletSlot = computed<Component | undefined>(() => {
    const identifier = props.contentlet?.identifier;
    // `slots` is optional-guarded: a consumer may provide the context directly
    // with an older shape that predates per-contentlet slots.
    const slot = identifier ? ctx.value.slots?.[identifier] : undefined;

    if (!slot) {
        return undefined;
    }

    return () => slot({ contentlet: props.contentlet });
});

const userComponent = computed<Component | undefined>(
    () => ctx.value.userComponents[props.contentlet?.contentType]
);
const noComponent = computed<Component | undefined>(
    () => ctx.value.userComponents[CUSTOM_NO_COMPONENT]
);
</script>

<template>
  <div
    v-bind="dotAttributes"
    :class="CONTENTLET_CLASS"
    :style="style"
  >
    <component
      :is="contentletSlot"
      v-if="contentletSlot"
    />
    <component
      :is="userComponent"
      v-else-if="userComponent"
      v-bind="contentlet"
    />
    <FallbackComponent
      v-else
      :user-no-component="noComponent"
      :contentlet="contentlet"
    />
  </div>
</template>
