import {createSSRApp} from 'vue';
import App from './App.vue';
// #ifdef H5
import {installButtonKeyboard} from './h5-accessibility';
const disposeKeyboard=installButtonKeyboard();
if(import.meta.hot)import.meta.hot.dispose(disposeKeyboard);
// #endif
export function createApp(){const app=createSSRApp(App);return {app};}
