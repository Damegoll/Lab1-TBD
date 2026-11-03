<script setup>
import { onMounted, ref } from 'vue'
import { http } from '@/api/http'

const usuario = ref(null)
const resumenes = ref([])
const cargando = ref(false)
const error = ref(null)
const errorResumen = ref(null)

async function cargar() {
  cargando.value = true
  error.value = null
  errorResumen.value = null
  const [perfil, resumen] = await Promise.allSettled([
    http.get('/api/v1/usuarios/me'),
    http.get('/api/v1/usuarios/me/resumen-mensual'),
  ])
  if (perfil.status === 'fulfilled') {
    usuario.value = perfil.value
  } else {
    error.value = perfil.reason.message
  }
  if (resumen.status === 'fulfilled') {
    resumenes.value = resumen.value
  } else {
    errorResumen.value = resumen.reason.message
  }
  cargando.value = false
}

function formatoDinero(monto) {
  return new Intl.NumberFormat('es-CL', { style: 'currency', currency: 'CLP', maximumFractionDigits: 2 }).format(Number(monto ?? 0))
}

function nombreMes(anio, mes) {
  return new Intl.DateTimeFormat('es-CL', { month: 'long', year: 'numeric' })
    .format(new Date(Number(anio), Number(mes) - 1, 1))
}

onMounted(cargar)
</script>

<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">Cuenta</div>
      <h1 class="text-h4 font-weight-bold">Mi cuenta</h1>
      <p class="text-body-1 text-medium-emphasis">Consulta los datos asociados a tu sesión.</p>
    </div>
  </div>

  <v-alert v-if="error" type="error" variant="tonal" class="mb-4">{{ error }}</v-alert>
  <v-skeleton-loader v-if="cargando" type="card" />
  <v-row v-else-if="usuario">
    <v-col cols="12" md="5">
      <v-card class="pa-6">
        <div class="d-flex align-center ga-4 mb-6">
          <v-avatar color="primary" size="58">
            <v-icon icon="mdi-account" color="white" size="30" />
          </v-avatar>
          <div>
            <h2 class="text-h6">{{ usuario.nombre }}</h2>
            <p class="text-body-2 text-medium-emphasis">@{{ usuario.username }}</p>
          </div>
        </div>
        <v-list lines="two">
          <v-list-item prepend-icon="mdi-email-outline" title="Correo electrónico" :subtitle="usuario.email" />
          <v-list-item prepend-icon="mdi-shield-account-outline" title="Rol" :subtitle="usuario.rol" />
          <v-list-item prepend-icon="mdi-account-check-outline" title="Estado">
            <template #subtitle>
              <v-chip :color="usuario.activo ? 'success' : 'error'" size="small" variant="tonal">
                {{ usuario.activo ? 'Activo' : 'Inactivo' }}
              </v-chip>
            </template>
          </v-list-item>
        </v-list>
      </v-card>
    </v-col>
    <v-col cols="12" md="7">
      <div class="mb-3">
        <h2 class="text-h6">Resumen mensual de viajes</h2>
        <p class="text-body-2 text-medium-emphasis">Cantidad, duración y gasto mensual.</p>
      </div>
      <v-alert v-if="errorResumen" type="error" variant="tonal" class="mb-3">{{ errorResumen }}</v-alert>
      <v-row v-else-if="resumenes.length">
        <v-col v-for="resumen in resumenes" :key="`${resumen.anio}-${resumen.mes}`" cols="12" sm="6">
          <v-card class="pa-5" height="100%">
            <div class="eyebrow mb-3">{{ nombreMes(resumen.anio, resumen.mes) }}</div>
            <div class="summary-metric">
              <span>Viajes</span>
              <strong>{{ resumen.cantidadViajes ?? 0 }}</strong>
            </div>
            <div class="summary-metric">
              <span>Minutos totales</span>
              <strong>{{ Number(resumen.minutosTotales ?? 0).toLocaleString('es-CL', { maximumFractionDigits: 1 }) }}</strong>
            </div>
            <div class="summary-metric">
              <span>Gasto total</span>
              <strong>{{ formatoDinero(resumen.gastoTotal) }}</strong>
            </div>
          </v-card>
        </v-col>
      </v-row>
      <v-card v-else class="pa-6 text-center text-medium-emphasis">
        Aún no hay resúmenes mensuales para mostrar.
      </v-card>
    </v-col>
  </v-row>
</template>
