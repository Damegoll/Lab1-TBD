import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import vuetify from './plugins/vuetify'
import { setUnauthorizedHandler } from './api/http'
import { useAuthStore } from './stores/auth'
import './assets/main.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(vuetify)

// Si el backend responde 401 con un token guardado (expiró o es inválido), cerramos la sesión.
setUnauthorizedHandler(() => {
  useAuthStore().logout()
  router.push({ name: 'login' })
})

app.mount('#app')
