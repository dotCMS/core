<script>
import Row from '../components/Row.vue'

export default {
  components: {
    Row
  },
  inject: ['data']
}
</script>

<template>
  <Row v-for="row in data?.layout.body.rows" :key="row.id" :row="row">{{ row.title }}</Row>
</template>
