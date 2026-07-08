<script setup lang="ts">
import { UVE_MODE } from '@dotcms/types';

import { useDotCMSShowWhen } from '../../composables/useDotCMSShowWhen';

/**
 * `DotCMSShow` conditionally renders its slot content based on the current
 * Universal Visual Editor (UVE) mode.
 *
 * @example
 * ```vue
 * <DotCMSShow :when="UVE_MODE.EDIT">
 *   <button @click="editContentlet(contentlet)">Edit</button>
 * </DotCMSShow>
 * ```
 */
const props = withDefaults(
    defineProps<{
        /** The UVE mode in which the slot content should be rendered. */
        when?: UVE_MODE;
    }>(),
    {
        when: UVE_MODE.EDIT
    }
);

const show = useDotCMSShowWhen(props.when);
</script>

<template>
    <slot v-if="show" />
</template>
