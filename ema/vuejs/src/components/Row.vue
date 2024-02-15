<script>
export default {
  inject: ['data'],
  mounted() {
    console.log(this.data) // This should now reflect updates
  }
}
</script>

<template>
  <div>
    <p>Row</p>
    <p v-if="data">{{ data.page.friendlyName }}</p>
  </div>
</template>
