<script setup>
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const username = ref('')
const password = ref('')
const error = ref(null)
const enviando = ref(false)

async function enviar() {
  error.value = null
  enviando.value = true
  try {
    await auth.login(username.value.trim(), password.value)
    await router.push({ name: 'vehiculos' })
  } catch (e) {
    error.value = e.message
  } finally {
    enviando.value = false
  }
}
</script>

<template>
  <v-container class="auth-page" fluid>
    <v-card class="auth-card pa-6 pa-sm-8" elevation="3">
      <div class="text-center mb-6">
        <v-avatar color="primary" size="58" class="mb-4">
          <v-icon icon="mdi-bike-fast" color="white" size="30" />
        </v-avatar>
        <h1 class="text-h5 font-weight-bold">Bienvenido a MoviTBD</h1>
        <p class="text-body-2 text-medium-emphasis mt-2">Inicia sesión para continuar</p>
      </div>

      <v-form class="d-flex flex-column ga-4" @submit.prevent="enviar">
        <v-text-field
          v-model="username"
          label="Usuario"
          autocomplete="username"
          prepend-inner-icon="mdi-account-outline"
          required
        />
        <v-text-field
          v-model="password"
          label="Contraseña"
          type="password"
          autocomplete="current-password"
          prepend-inner-icon="mdi-lock-outline"
          required
        />
        <v-alert v-if="error" type="error" variant="tonal" density="comfortable">{{ error }}</v-alert>
        <v-btn type="submit" color="primary" size="large" :loading="enviando" block>Iniciar sesión</v-btn>
      </v-form>

      <p class="text-center text-body-2 mt-6 mb-0">
        ¿No tienes cuenta?
        <RouterLink class="text-primary font-weight-medium" to="/registro">Regístrate</RouterLink>
      </p>
    </v-card>
  </v-container>
</template>
