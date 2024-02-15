<script setup>
import { RouterLink, RouterView } from 'vue-router'
import { ref } from 'vue'
import { client } from './utils/client'

const nav = ref(null)

client.nav.get({ path: '/', depth: '2' }).then(({ entity }) => {
  nav.value = entity.children
})
</script>

<template>
  <header>
    <div class="wrapper">
      <nav>
        <!-- Render a RouterLink for each nav item -->
        <template v-if="nav">
          <RouterLink v-for="item in nav" :key="item.href" :to="item.href">{{
            item.title
          }}</RouterLink>
        </template>
      </nav>
    </div>
  </header>

  <RouterView />
</template>

<style scoped></style>
