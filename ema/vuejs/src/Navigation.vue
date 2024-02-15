<script>
import { RouterLink } from 'vue-router'

export default {
  props: {
    nav: {
      type: Array,
      required: true
    }
  },
  components: {
    RouterLink
  }
}
</script>

<template>
  <ul className="flex space-x-4 text-white">
    <li v-for="item in nav" :key="item.href">
      <RouterLink :to="item.href">{{ item.title }}</RouterLink>
    </li>
  </ul>
</template>
