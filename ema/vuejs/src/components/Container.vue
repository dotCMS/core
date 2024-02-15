<script>
import FallbackComponent from './FallbackComponent.vue'

export default {
  components: {
    FallbackComponent
  },
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
  },
  methods: {
    getComponent(contentlet) {
      const componentName = contentlet.contentType
      const isComponentRegistered = this.$registeredComponents.includes(componentName)
      return isComponentRegistered ? componentName : 'FallbackComponent'
    }
  }
}
</script>

<template>
  <div class="flex flex-col gap-4" data-dot="container">
    <p v-for="contentlet in contentlets" :key="contentlet.identifier">
      <component v-bind="contentlet" :is="getComponent(contentlet)"></component>
    </p>
  </div>
</template>
