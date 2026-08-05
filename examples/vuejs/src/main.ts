import { createApp } from 'vue';

import App from './App.vue';
import { dotCMSVue } from './lib/dotCMSClient';
import { router } from './router';

import './assets/globals.css';

createApp(App).use(router).use(dotCMSVue).mount('#app');
