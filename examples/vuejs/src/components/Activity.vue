<template>
  <article class="p-4 overflow-hidden bg-white rounded shadow-lg">
    <img
      class="w-full"
      v-if="image"
      :src="imageUrl"
      alt="Activity Image"
      width="100"
      height="100"
    />
    <div class="px-6 py-4">
      <p class="mb-2 text-xl font-bold">{{ title }}</p>
      <p class="text-base line-clamp-3">{{ description }}</p>
    </div>
    <div class="px-6 pt-4 pb-2">
      <router-link
        class="inline-block px-4 py-2 font-bold text-white bg-purple-500 rounded-full hover:bg-purple-700"
        :to="`/activities/${urlTitle || '#'}`"
      >
        Link to detail →
      </router-link>
    </div>
  </article>
</template>

<script>
export default {
  props: {
    title: String,
    description: String,
    image: String,
    urlTitle: String
  },
  inject: ['data'], // Assuming 'data' is provided and contains 'viewAs' with 'language'
  computed: {
    languageId() {
      return this.data.viewAs.language.id
    },
    imageUrl() {
      // Access environment variables in the script block
      const host = import.meta.env.VITE_DOTCMS_HOST
      return `${host}${this.image}?language_id=${this.languageId}`
    }
  }
}
</script>
