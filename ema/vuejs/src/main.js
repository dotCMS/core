import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

import Banner from './components/Banner.vue'

const app = createApp(App)

app.use(router)

app.component('Banner', Banner)

app.mount('#app')
