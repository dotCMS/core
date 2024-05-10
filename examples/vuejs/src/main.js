import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

import { registerGlobalComponents } from './components/global-components'

const app = createApp(App)

app.use(router)

registerGlobalComponents(app)

app.mount('#app')
