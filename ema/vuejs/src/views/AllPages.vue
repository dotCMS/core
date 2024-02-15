<script>
import { dotcmsClient } from '@dotcms/client'

// Define fetchData as a standalone function
async function fetchData(routePath, callback) {
  const client = dotcmsClient.init({
    dotcmsUrl: import.meta.env.VITE_DOTCMS_HOST,
    authToken: import.meta.env.VITE_DOTCMS_TOKEN,
    requestOptions: {
      cache: 'no-cache'
    }
  })

  const data = await client.page
    .get({
      path: routePath || 'index', // Adjust based on your routing logic
      language_id: 1
    })
    .then(({ entity }) => {
      return entity
    })

  callback(data)
}

export default {
  name: 'AllPages',
  data() {
    return {
      data: null
    }
  },
  beforeRouteEnter(to, from, next) {
    // Directly use fetchData function
    fetchData(to.href, (data) => {
      next((vm) => (vm.data = data))
    })
  },
  beforeRouteUpdate(to, from, next) {
    // Use fetchData within the component context
    fetchData(to.href, (data) => {
      this.data = data
      next()
    })
  }
}
</script>

<template>
  <main>
    <p v-if="data">{{ data.page.friendlyName }}</p>
  </main>
</template>
