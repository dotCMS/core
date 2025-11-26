<template>
  <div class="row" data-dot="row" :class="combinedClasses">
    <Column v-for="(column, index) in row.columns" :key="index" :column="column" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import Column from './Column.vue' // Ensure the import path matches your file structure
import { defineProps } from 'vue'

const props = defineProps({
  row: Object
})

const combinedClasses = computed(() => {
  return [props.row.styleClass].filter(Boolean).join(' ')
})
</script>

<style>
.row {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  gap: 1rem;
}
</style>
