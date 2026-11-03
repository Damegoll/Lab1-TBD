<script setup>
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const nombre = ref('')
const username = ref('')
const email = ref('')
const password = ref('')
const error = ref(null)
const enviando = ref(false)

async function enviar() {
  error.value = null
  enviando.value = true
  try {
    await auth.registrar({
      username: username.value.trim(),
      nombre: nombre.value.trim(),
      email: email.value.trim(),
      password: password.value,
    })
    await router.push({ name: 'mi-cuenta' })
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
        <v-avatar color="secondary" size="58" class="mb-4">
          <v-icon icon="mdi-account-plus-outline" color="white" size="28" />
        </v-avatar>
        <h1 class="text-h5 font-weight-bold">Crear cuenta</h1>
        <p class="text-body-2 text-medium-emphasis mt-2">Regístrate como usuario de MoviTBD</p>
      </div>

      <v-form class="d-flex flex-column ga-3" @submit.prevent="enviar">
        <v-text-field
          v-model="nombre"
          label="Nombre completo"
          autocomplete="name"
          maxlength="100"
          required
        />
        <v-text-field
          v-model="username"
          label="Usuario"
          autocomplete="username"
          maxlength="50"
          required
        />
        <v-text-field
          v-model="email"
          label="Correo electrónico"
          type="email"
          autocomplete="email"
          maxlength="254"
          required
        />
        <v-text-field
          v-model="password"
          label="Contraseña (mínimo 12 caracteres)"
          type="password"
          autocomplete="new-password"
          minlength="12"
          maxlength="72"
          required
        />
        <v-alert v-if="error" type="error" variant="tonal" density="comfortable">{{ error }}</v-alert>
        <v-btn type="submit" color="primary" size="large" :loading="enviando" block>Crear cuenta</v-btn>
      </v-form>

      <p class="text-center text-body-2 mt-6 mb-0">
        ¿Ya tienes cuenta?
        <RouterLink class="text-primary font-weight-medium" to="/login">Inicia sesión</RouterLink>
      </p>
    </v-card>
  </v-container>
</template>
