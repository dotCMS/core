<script>
export default {
  inject: ['data'],
  props: {
    containerRef: Object
  },
  computed: {
    containers() {
      return this.data.containers // Accessing the injected context data
    },
    contentlets() {
      const { identifier, uuid } = this.containerRef
      const contentlets = this.containers[identifier].contentlets[`uuid-${uuid}`]
      return contentlets || []
    }
  }
}
</script>

<template>
  <div data-dot="container">
    <p v-for="contentlet in contentlets" :key="contentlet.identifier">
      <component v-bind="contentlet" :is="contentlet.contentType"></component>
    </p>
  </div>
</template>
