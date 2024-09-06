// src/global-components.js
import Banner from './Banner.vue'
import Activity from './Activity.vue'
import Image from './Image.vue'
import Product from './Product.vue'
import WebPageContent from './WebPageContent.vue'
// Import other components

const components = {
  Banner,
  Activity,
  Image,
  Product,
  webPageContent: WebPageContent
}

export const registerGlobalComponents = (app) => {
  Object.entries(components).forEach(([name, component]) => {
    app.component(name, component)
  })

  // Optionally attach the registry to the app instance for later reference
  app.config.globalProperties.$registeredComponents = Object.keys(components)
}
