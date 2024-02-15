<script>
export default {
  inject: ['data'],

  mounted() {
    console.log(this.data) // This should now reflect updates
  }
}
</script>

<template>
  <p>{{ data?.page.friendlyName }}</p>
</template>
