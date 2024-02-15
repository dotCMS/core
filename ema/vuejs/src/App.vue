<script>
import { RouterView } from 'vue-router'
import { ref, watch, provide, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { client } from './utils/client'
import MyHeader from './components/MyHeader.vue'
import MyFooter from './components/MyFooter.vue'
import Navigation from './components/Navigation.vue'

async function fetchData(routePath) {
  const data = await client.page
    .get({
      path: routePath || 'index',
      language_id: 1
    })
    .then(({ entity }) => entity)

  return data
}

export default {
  components: {
    MyHeader,
    RouterView,
    Navigation,
    MyFooter
  },
  inject: ['data'],
  setup() {
    const data = ref(null)
    const nav = ref(null)
    const route = useRoute()

    provide('data', data)

    onMounted(() => {
      client.nav.get({ path: '/', depth: '2' }).then(({ entity }) => {
        nav.value = entity.children
      })
    })

    watch(
      () => route.path,
      async (newPath) => {
        // React to route changes
        const fetchedData = await fetchData(newPath)
        data.value = fetchedData
      }
    )

    return { data, nav }
  }
}
</script>

<template>
  <div className="flex flex-col min-h-screen gap-6 bg-lime-50">
    <MyHeader v-if="data?.layout.header">
      <Navigation v-if="nav" :nav="nav" />
    </MyHeader>

    <main className="container flex flex-col gap-8 m-auto">
      <RouterView />
    </main>

    <MyFooter v-if="data?.layout.footer" />
  </div>
</template>
