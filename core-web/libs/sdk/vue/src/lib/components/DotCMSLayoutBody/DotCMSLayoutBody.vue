<script setup lang="ts">
import { computed, withDefaults } from 'vue';

import ErrorMessage from './components/ErrorMessage.vue';
import type { DotCMSLayoutBodyProps } from './types';

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

// Provide the context as a computed so live UVE page updates (a new page asset
// arriving via `uve-set-page-data`) propagate to the whole layout tree.
provideDotCMSPageContext(
    computed(() => ({
        pageAsset: props.page,
        mode: props.mode,
        userComponents: props.components
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
