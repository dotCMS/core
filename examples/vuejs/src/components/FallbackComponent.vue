<template>
  <div class="p-4 border border-red-500">No component for {{ contentType }}</div>
</template>

<script>
export default {
  name: 'FallbackComponent',
  props: {
    contentType: String
  }
}
</script>
