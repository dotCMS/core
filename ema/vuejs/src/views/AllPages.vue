<script>
import { dotcmsClient } from '@dotcms/client'

export default {
  name: 'AllPages',
  data() {
    return {
      data: null
    }
  },
  created() {
    // watch the params of the route to fetch the data again
    this.$watch(
      () => this.$route.params,
      () => {
        this.fetchData()
      },
      // fetch the data when the view is created and the data is
      // already being observed
      { immediate: true }
    )
  },
  methods: {
    async fetchData() {
      const client = dotcmsClient.init({
        dotcmsUrl: import.meta.env.VITE_DOTCMS_HOST,
        authToken: import.meta.env.VITE_DOTCMS_TOKEN,
        requestOptions: {
          // In production you might want to deal with this differently
          cache: 'no-cache'
        }
      })

      this.data = await client.page
        .get({
          path: 'index',
          language_id: 1
        })
        .then(({ entity }) => {
          return entity
        })
    }
  }
}
</script>

<template>
  <main>
    <!-- <h1>Hello {{ entity.page.title }}</h1> -->
    <p v-if="data">{{ data.page.friendlyName }}</p>
  </main>
</template>
