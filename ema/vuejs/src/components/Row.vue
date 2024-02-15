<script>
import { computed } from 'vue'
import Column from './Column.vue' // Adjust the import path as necessary

export default {
  components: {
    Column
  },
  props: {
    row: Object
  },
  setup(props) {
    const combinedClasses = computed(() => {
      return [props.row.styleClass].filter(Boolean).join(' ')
    })

    return { combinedClasses }
  }
}
</script>

<template>
  <div class="row" data-dot="row">
    <Column v-for="(column, index) in row.columns" :key="index" :column="column" />
  </div>
</template>

<style>
.row {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  gap: 1rem;
}
</style>
