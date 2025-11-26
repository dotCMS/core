<template>
  <div class="overflow-hidden bg-white rounded shadow-lg">
    <div class="p-4">
      <img class="w-full" :src="imageUrl" width="100" height="100" :alt="title" />
    </div>
    <div class="px-6 py-4 bg-slate-100">
      <div class="mb-2 text-xl font-bold">{{ title }}</div>
      <div class="text-gray-500 line-through" v-if="retailPrice && salePrice">
        {{ formatPrice(retailPrice) }}
      </div>
      <div :class="{ 'text-3xl font-bold': true, 'text-gray-500': retailPrice && salePrice }">
        {{ salePrice ? formatPrice(salePrice) : formatPrice(retailPrice) }}
      </div>
      <router-link
        class="inline-block px-4 py-2 mt-4 text-white bg-green-500 rounded hover:bg-green-600"
        :to="`/store/products/${urlTitle || '#'}`"
      >
        Buy Now
      </router-link>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    image: String,
    title: String,
    salePrice: String,
    retailPrice: String,
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
  },
  methods: {
    formatPrice(price) {
      return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD'
      }).format(price)
    }
  }
}
</script>
