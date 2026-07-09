<script setup lang="ts">
import { computed, markRaw, toRaw, type Component } from 'vue';

import ErrorMessage from './components/ErrorMessage.vue';
import type { DotCMSLayoutBodyProps } from './types';

import { useIsAnalyticsActive } from '../../composables/useIsAnalyticsActive';
import { useIsDevMode } from '../../composables/useIsDevMode';
import { provideDotCMSPageContext } from '../../contexts/dotcms-page.context';
import Row from '../Row/Row.vue';

/**
 * Renders the layout body of a dotCMS page: its rows, columns, containers and
 * contentlets, dispatching each contentlet to the mapped component.
 *
 * @example
 * ```vue
 * <DotCMSLayoutBody :page="pageAsset" :components="pageComponents" />
 * ```
 */
const props = withDefaults(defineProps<DotCMSLayoutBodyProps>(), {
    mode: 'production'
});

// Resolve dev-mode and analytics ONCE at the layout root and share them via the
// context, so the whole tree reads one value instead of each contentlet/container
// re-resolving `getUVEState()` and registering its own analytics listener.
const isDevMode = useIsDevMode(() => props.mode);
const isAnalyticsActive = useIsAnalyticsActive();

// Unwrap each component from the reactive props and mark the map raw so Vue
// never wraps the component definitions in a reactive proxy. Rendering a proxied
// component via `<component :is>` triggers Vue's "Component that was made
// reactive" warning and adds proxy overhead per contentlet. Components are static
// app config, so a raw copy is safe.
const rawComponents = computed(() => {
    const out: Record<string, Component> = {};
    for (const key of Object.keys(props.components)) {
        out[key] = toRaw(props.components[key]);
    }

    return markRaw(out);
});

// Provide the context as a computed so live UVE page updates (a new page asset
// arriving via `uve-set-page-data`) propagate to the whole layout tree.
provideDotCMSPageContext(
    computed(() => ({
        pageAsset: props.page,
        mode: props.mode,
        userComponents: rawComponents.value,
        isDevMode: isDevMode.value,
        isAnalyticsActive: isAnalyticsActive.value
    }))
);

const rows = computed(() => props.page?.layout?.body?.rows);
</script>

<template>
  <template v-if="rows">
    <Row
      v-for="(row, index) in rows"
      :key="index"
      :row="row"
      :index="index + 1"
    />
  </template>
  <ErrorMessage v-else />
</template>
