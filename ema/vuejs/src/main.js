import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

import Banner from './components/Banner.vue'
import Activity from './components/Activity.vue'
import Image from './components/Image.vue'
import Product from './components/Product.vue'
import WebPageContent from './components/WebPageContent.vue'

const app = createApp(App)

app.use(router)

app
  .component('Banner', Banner)
  .component('Activity', Activity)
  .component('Image', Image)
  .component('Product', Product)
  .component('webPageContent', WebPageContent)

app.mount('#app')
