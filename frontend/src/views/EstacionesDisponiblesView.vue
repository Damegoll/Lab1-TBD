<script setup>
import { computed, onMounted, ref } from 'vue'
import { http } from '@/api/http'

const filas = ref([])
const cargando = ref(false)
const error = ref(null)

const estaciones = computed(() => {
  const agrupadas = new Map()
  for (const fila of filas.value) {
    const estacion = agrupadas.get(fila.estacionId) ?? {
      id: fila.estacionId,
      nombre: fila.estacionNombre,
      bicicletas: null,
      scooters: null,
    }
    if (fila.tipoVehiculo === 'BICICLETA') {
      estacion.bicicletas = { cantidad: fila.cantidad, bateriaPromedio: fila.bateriaPromedio }
    } else if (fila.tipoVehiculo === 'SCOOTER') {
      estacion.scooters = { cantidad: fila.cantidad, bateriaPromedio: fila.bateriaPromedio }
    }
    agrupadas.set(fila.estacionId, estacion)
  }
  return [...agrupadas.values()]
})

async function cargar() {
  cargando.value = true
  error.value = null
  try {
    filas.value = await http.get('/api/v1/estaciones/disponibles')
  } catch (e) {
    error.value = e.message
  } finally {
    cargando.value = false
  }
}

function mostrarBateria(valor) {
  return valor == null ? '—' : `${Number(valor).toFixed(0)}%`
}

onMounted(cargar)
</script>

<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">Red MoviTBD</div>
      <h1 class="text-h4 font-weight-bold">Estaciones disponibles</h1>
      <p class="text-body-1 text-medium-emphasis">Vehículos disponibles y nivel promedio de batería por estación.</p>
    </div>
    <v-btn variant="tonal" prepend-icon="mdi-refresh" :loading="cargando" @click="cargar">Actualizar</v-btn>
  </div>

  <v-alert v-if="error" type="error" variant="tonal" class="mb-4">{{ error }}</v-alert>

  <v-row v-if="estaciones.length">
    <v-col v-for="estacion in estaciones" :key="estacion.id" cols="12" sm="6" xl="4">
      <v-card class="station-card pa-5" height="100%">
        <div class="d-flex align-center ga-3 mb-5">
          <v-avatar color="primary" variant="tonal">
            <v-icon icon="mdi-map-marker" />
          </v-avatar>
          <div>
            <h2 class="text-h6">{{ estacion.nombre }}</h2>
            <p class="text-caption text-medium-emphasis">Estación #{{ estacion.id }}</p>
          </div>
        </div>
        <v-divider class="mb-3" />
        <div class="availability-row">
          <v-icon icon="mdi-bike" color="primary" />
          <span class="availability-label">Bicicletas</span>
          <strong>{{ estacion.bicicletas?.cantidad ?? 0 }}</strong>
          <span class="text-caption text-medium-emphasis">{{ mostrarBateria(estacion.bicicletas?.bateriaPromedio) }}</span>
        </div>
        <div class="availability-row">
          <v-icon icon="mdi-scooter" color="secondary" />
          <span class="availability-label">Scooters</span>
          <strong>{{ estacion.scooters?.cantidad ?? 0 }}</strong>
          <span class="text-caption text-medium-emphasis">{{ mostrarBateria(estacion.scooters?.bateriaPromedio) }}</span>
        </div>
      </v-card>
    </v-col>
  </v-row>
  <v-card v-else-if="!cargando" class="pa-8 text-center">
    <v-icon icon="mdi-map-marker-off-outline" size="42" color="secondary" class="mb-2" />
    <p class="text-body-1 mb-0">No hay estaciones con vehículos disponibles.</p>
  </v-card>
  <v-skeleton-loader v-else type="card" />
</template>
