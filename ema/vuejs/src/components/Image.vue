<template>
  <div class="relative overflow-hidden bg-white rounded shadow-lg group">
    <div class="relative w-full bg-gray-200 h-96">
      <img class="object-cover w-full h-full" :src="imageUrl" :alt="title" />
    </div>
    <div
      class="absolute bottom-0 w-full px-6 py-8 text-white transition-transform duration-300 translate-y-full bg-orange-500 bg-opacity-80 group-hover:translate-y-0"
    >
      <div class="mb-2 text-2xl font-bold">{{ title }}</div>
      <p class="text-base">{{ description }}</p>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    fileAsset: String,
    title: String,
    description: String
  },
  inject: ['data'], // Assuming 'data' is provided and contains 'viewAs' with 'language'
  computed: {
    languageId() {
      return this.data.viewAs.language.id
    },
    imageUrl() {
      // Access environment variables in the script block
      const host = import.meta.env.VITE_DOTCMS_HOST
      return `${host}${this.fileAsset}?language_id=${this.languageId}`
    }
  }
}
</script>
