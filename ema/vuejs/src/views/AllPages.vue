<script>
import { ref, provide } from 'vue'
import { client } from '../utils/client'
import Row from '../components/Row.vue'

async function fetchData(routePath, callback) {
  const data = await client.page
    .get({
      path: routePath || 'index',
      language_id: 1
    })
    .then(({ entity }) => entity)

  callback(data)
}

export default {
  name: 'AllPages',
  components: {
    Row
  },
  setup() {
    const data = ref(null)

    provide('data', data)

    // Function to update data
    const updateData = (newData) => {
      data.value = newData
    }

    // Expose updateData so it can be used in lifecycle hooks
    return { data, updateData }
  },
  beforeRouteEnter(to, from, next) {
    fetchData(to.path, (data) => {
      next((vm) => {
        vm.updateData(data) // Use the exposed method to update data
      })
    })
  },
  beforeRouteUpdate(to, from, next) {
    fetchData(to.path, (data) => {
      this.updateData(data) // Directly use the method within the component instance
      next()
    })
  }
}
</script>

<template>
  <main>
    <p v-if="data">{{ data.page.friendlyName }}</p>
    <Row />
  </main>
</template>
