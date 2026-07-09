<script setup lang="ts">
import { computed } from 'vue';

import type { BlockEditorNode } from '@dotcms/types';

import BlockEditorBlock from '../BlockEditorBlock.vue';

import type { CustomRenderer } from '../../types';

/**
 * Renders a table block. Cell type — not row position — decides `<th>` vs `<td>`
 * (`node.type === 'tableHeader'`), matching the VTL renderer, so column headers
 * in any row keep their semantic wrapper and `scope`.
 */
const props = defineProps<{
    content: BlockEditorNode[];
    attrs?: BlockEditorNode['attrs'];
    customRenderers?: CustomRenderer;
    isDevMode?: boolean;
}>();

const caption = computed(() => props.attrs?.caption || undefined);
const ariaLabel = computed(() => props.attrs?.ariaLabel || undefined);
const ariaLabelledby = computed(() => props.attrs?.ariaLabelledby || undefined);

const colSpan = (node: BlockEditorNode) => Number(node.attrs?.colspan || 1);
const rowSpan = (node: BlockEditorNode) => Number(node.attrs?.rowspan || 1);
</script>

<template>
  <table
    :aria-label="ariaLabel"
    :aria-labelledby="ariaLabelledby"
  >
    <caption v-if="caption">
      {{ caption }}
    </caption>
    <tbody>
      <tr
        v-for="(rowNode, rowIndex) in content"
        :key="`row-${rowIndex}`"
      >
        <template
          v-for="(cellNode, cellIndex) in rowNode.content ?? []"
          :key="`cell-${cellIndex}`"
        >
          <th
            v-if="cellNode.type === 'tableHeader'"
            :colspan="colSpan(cellNode)"
            :rowspan="rowSpan(cellNode)"
            :scope="cellNode.attrs?.scope || undefined"
          >
            <BlockEditorBlock
              :content="cellNode.content ?? []"
              :custom-renderers="customRenderers"
              :is-dev-mode="isDevMode"
            />
          </th>
          <td
            v-else
            :colspan="colSpan(cellNode)"
            :rowspan="rowSpan(cellNode)"
          >
            <BlockEditorBlock
              :content="cellNode.content ?? []"
              :custom-renderers="customRenderers"
              :is-dev-mode="isDevMode"
            />
          </td>
        </template>
      </tr>
    </tbody>
  </table>
</template>
