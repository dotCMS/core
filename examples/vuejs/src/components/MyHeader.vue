<template>
  <header class="flex items-center justify-between p-4 bg-purple-500">
    <div class="flex items-center">
      <h2 class="text-3xl font-bold text-white">
        <router-link to="/">TravelLux in Vue</router-link>
      </h2>
    </div>
    <slot></slot>
  </header>
</template>

<script>
export default {
  name: 'MyHeader'
}
</script>
