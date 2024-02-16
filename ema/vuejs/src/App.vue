<script setup>
import { ref, watch, onMounted, provide } from 'vue'
import { useRoute, RouterView } from 'vue-router'
import { client } from './utils/client'
import MyHeader from './components/MyHeader.vue'
import MyFooter from './components/MyFooter.vue'
import Navigation from './components/Navigation.vue'

const route = useRoute()
const data = ref(null)
const nav = ref(null)

async function fetchData(routePath) {
  const data = await client.page
    .get({
      path: routePath || 'index',
      language_id: 1
    })
    .then(({ entity }) => entity)

  return data
}

onMounted(async () => {
  nav.value = await client.nav.get({ path: '/', depth: '2' }).then(({ entity }) => entity.children)
})

watch(
  () => route.path,
  async (newPath) => {
    data.value = await fetchData(newPath)
  }
)

// Provides the page api response (data) object to child components.
provide('data', data)
</script>

<template>
  <div class="flex flex-col min-h-screen gap-6 bg-lime-50">
    <MyHeader v-if="data?.layout.header">
      <Navigation v-if="nav" :nav="nav" />
    </MyHeader>

    <main class="container flex flex-col gap-8 m-auto">
      <RouterView />
    </main>

    <MyFooter v-if="data?.layout.footer" />
  </div>
</template>
