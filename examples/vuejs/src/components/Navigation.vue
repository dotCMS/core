<template>
  <ul class="flex space-x-4 text-white">
    <li v-for="item in nav" :key="item.href">
      <RouterLink :to="item.href">{{ item.title }}</RouterLink>
    </li>
  </ul>
</template>

<script setup>
import { defineProps } from 'vue'
import { RouterLink } from 'vue-router'

defineProps({
  nav: {
    type: Array,
    required: true
  }
})
</script>
