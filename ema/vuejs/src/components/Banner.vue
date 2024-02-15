<script>
export default {
  props: {
    title: String,
    image: String,
    caption: String,
    buttonText: String,
    link: String
  },
  inject: ['data'],
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

<template>
  <div class="relative w-full p-4 bg-gray-200 h-96">
    <img class="object-cover w-full h-full" :src="imageUrl" :alt="title" />
    <div
      class="absolute inset-0 flex flex-col items-center justify-center p-4 text-center text-white"
    >
      <h2 class="mb-2 text-6xl font-bold text-shadow">{{ title }}</h2>
      <p class="mb-4 text-xl text-shadow">{{ caption }}</p>
      <router-link
        class="p-4 text-xl transition duration-300 bg-purple-500 rounded hover:bg-purple-600"
        :to="link || '#'"
      >
        {{ buttonText }}
      </router-link>
    </div>
  </div>
</template>
