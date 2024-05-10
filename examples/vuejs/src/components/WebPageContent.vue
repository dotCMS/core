<template>
  <div>
    <h1 class="text-xl font-bold">{{ title }}</h1>
    <div v-html="body"></div>
  </div>
</template>

<script>
export default {
  props: {
    title: String,
    body: String
  }
}
</script>
