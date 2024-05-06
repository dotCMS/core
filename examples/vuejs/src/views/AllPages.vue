<template>
  <Row v-for="row in data?.layout.body.rows" :key="row.id" :row="row" />
</template>

<script setup>
import { inject } from 'vue'
import Row from '../components/Row.vue'

const data = inject('data')
</script>
