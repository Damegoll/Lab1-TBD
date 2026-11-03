<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const drawer = ref(true)

const links = computed(() => {
  if (!auth.estaAutenticado) return []

  if (auth.rol === 'USUARIO') {
    return [
      { title: 'Mi cuenta', icon: 'mdi-account', to: '/mi-cuenta' },
      { title: 'Mi billetera', icon: 'mdi-wallet-outline', to: '/billeteras' },
      { title: 'Mis viajes', icon: 'mdi-bike-fast', to: '/viajes' },
      { title: 'Estaciones disponibles', icon: 'mdi-map-marker-radius', to: '/estaciones-disponibles' },
    ]
  }

  const items = [
    { title: 'Estaciones', icon: 'mdi-map-marker-multiple', to: '/estaciones' },
    { title: 'Disponibilidad', icon: 'mdi-map-marker-radius', to: '/estaciones-disponibles' },
    { title: 'Vehículos', icon: 'mdi-scooter', to: '/vehiculos' },
    { title: 'Billeteras', icon: 'mdi-wallet-outline', to: '/billeteras' },
    { title: 'Viajes', icon: 'mdi-bike-fast', to: '/viajes' },
    { title: 'Tarifas', icon: 'mdi-currency-usd', to: '/tarifas' },
    { title: 'Traslados', icon: 'mdi-swap-horizontal', to: '/traslados' },
  ]

  if (auth.esAdmin) {
    items.push(
      { title: 'Usuarios', icon: 'mdi-account-group', to: '/usuarios' },
      { title: 'Auditorías', icon: 'mdi-history', to: '/auditorias' },
    )
  }
  return items
})

const nombreRol = computed(() => ({
  USUARIO: 'Usuario',
  OPERADOR: 'Operador',
  ADMIN: 'Administrador',
})[auth.rol] ?? '')

function salir() {
  auth.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <v-app>
    <v-navigation-drawer
      v-if="auth.estaAutenticado"
      v-model="drawer"
      :permanent="$vuetify.display.mdAndUp"
      :temporary="!$vuetify.display.mdAndUp"
      width="264"
    >
      <div class="brand-lockup">
        <v-avatar color="primary" size="42">
          <v-icon icon="mdi-bike-fast" color="white" />
        </v-avatar>
        <div>
          <div class="brand-name">MoviTBD</div>
          <div class="brand-caption">Movilidad compartida</div>
        </div>
      </div>

      <v-divider class="mx-4" />

      <v-list nav density="comfortable" class="px-3 py-4">
        <v-list-item
          v-for="link in links"
          :key="link.to"
          :to="link.to"
          :prepend-icon="link.icon"
          :title="link.title"
          rounded="lg"
          color="primary"
          @click="drawer = $vuetify.display.mdAndUp"
        />
      </v-list>

      <template #append>
        <div class="drawer-user">
          <v-avatar color="secondary" size="38">
            <span class="text-white font-weight-bold">{{ auth.usuario?.username?.[0]?.toUpperCase() }}</span>
          </v-avatar>
          <div class="drawer-user-details">
            <span class="font-weight-medium">{{ auth.usuario?.username }}</span>
            <span class="text-caption text-medium-emphasis">{{ nombreRol }}</span>
          </div>
        </div>
      </template>
    </v-navigation-drawer>

    <v-app-bar flat border>
      <v-app-bar-nav-icon
        v-if="auth.estaAutenticado && !$vuetify.display.mdAndUp"
        aria-label="Abrir menú"
        @click="drawer = !drawer"
      />
      <v-app-bar-title>{{ auth.estaAutenticado ? 'Panel de gestión' : 'MoviTBD' }}</v-app-bar-title>
      <template v-if="auth.estaAutenticado" #append>
        <v-chip class="mr-2" color="tertiary" variant="tonal" size="small">{{ nombreRol }}</v-chip>
        <v-btn icon="mdi-logout" variant="text" aria-label="Cerrar sesión" @click="salir" />
      </template>
    </v-app-bar>

    <v-main>
      <v-container fluid class="page-container">
        <RouterView />
      </v-container>
    </v-main>
  </v-app>
</template>
