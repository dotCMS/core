<script setup lang="ts">
/** Dev-only warning shown when a contentlet block has no matching custom renderer. */
defineProps<{ contentType?: string }>();

const boxStyle = {
    backgroundColor: '#fffaf0',
    color: '#333',
    padding: '1rem',
    borderRadius: '0.5rem',
    marginBottom: '1rem',
    marginTop: '1rem',
    border: '1px solid #ed8936'
};

const accent = { color: '#c05621' };
</script>

<template>
  <div
    data-testid="no-component-provided"
    :style="boxStyle"
  >
    <strong :style="accent">Dev Warning</strong>
    : No component or custom renderer provided for content type
    <strong :style="accent">{{ contentType || 'Unknown' }}</strong>
    .
    <br>
    Please refer to the
    <a
      href="https://dev.dotcms.com/docs/block-editor"
      target="_blank"
      rel="noopener noreferrer"
      :style="accent"
    >
      Block Editor Custom Renderers Documentation
    </a>
    for guidance.
  </div>
</template>
