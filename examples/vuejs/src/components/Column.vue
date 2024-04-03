<script setup>
import { computed } from 'vue'
import Container from './Container.vue'
import { defineProps } from 'vue'

const props = defineProps({
  column: Object
})

const endClassMap = {
  1: 'col-end-1',
  2: 'col-end-2',
  3: 'col-end-3',
  4: 'col-end-4',
  5: 'col-end-5',
  6: 'col-end-6',
  7: 'col-end-7',
  8: 'col-end-8',
  9: 'col-end-9',
  10: 'col-end-10',
  11: 'col-end-11',
  12: 'col-end-12',
  13: 'col-end-13'
}

const startClassMap = {
  1: 'col-start-1',
  2: 'col-start-2',
  3: 'col-start-3',
  4: 'col-start-4',
  5: 'col-start-5',
  6: 'col-start-6',
  7: 'col-start-7',
  8: 'col-start-8',
  9: 'col-start-9',
  10: 'col-start-10',
  11: 'col-start-11',
  12: 'col-start-12'
}

const getPositionStyleClasses = (start, end) => {
  const startClass = startClassMap[start]
  const endClass = endClassMap[end]

  return { startClass, endClass }
}

const { startClass, endClass } = getPositionStyleClasses(
  props.column.leftOffset,
  props.column.width + props.column.leftOffset
)

const combinedClasses = computed(() => {
  return [startClass, endClass, props.column.styleClass].filter(Boolean).join(' ')
})
</script>

<template>
  <div data-dot="column" :class="combinedClasses">
    <Container
      v-for="container in column.containers"
      :key="container.id"
      :containerRef="container"
    />
  </div>
</template>

<style>
.col-start-1 {
  grid-column-start: 1;
}
.col-start-2 {
  grid-column-start: 2;
}
.col-start-3 {
  grid-column-start: 3;
}
.col-start-4 {
  grid-column-start: 4;
}
.col-start-5 {
  grid-column-start: 5;
}
.col-start-6 {
  grid-column-start: 6;
}
.col-start-7 {
  grid-column-start: 7;
}
.col-start-8 {
  grid-column-start: 8;
}
.col-start-9 {
  grid-column-start: 9;
}
.col-start-10 {
  grid-column-start: 10;
}
.col-start-11 {
  grid-column-start: 11;
}
.col-start-12 {
  grid-column-start: 12;
}
.col-end-1 {
  grid-column-end: 1;
}
.col-end-2 {
  grid-column-end: 2;
}
.col-end-3 {
  grid-column-end: 3;
}
.col-end-4 {
  grid-column-end: 4;
}
.col-end-5 {
  grid-column-end: 5;
}
.col-end-6 {
  grid-column-end: 6;
}
.col-end-7 {
  grid-column-end: 7;
}
.col-end-8 {
  grid-column-end: 8;
}
.col-end-9 {
  grid-column-end: 9;
}
.col-end-10 {
  grid-column-end: 10;
}
.col-end-11 {
  grid-column-end: 11;
}
.col-end-12 {
  grid-column-end: 12;
}
.col-end-13 {
  grid-column-end: 13;
}
</style>
