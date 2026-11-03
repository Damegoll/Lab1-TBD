<script setup>
import { onMounted, ref } from 'vue'
import { http } from '@/api/http'

const estaciones = ref([])
const cargando = ref(false)
const guardando = ref(false)
const error = ref(null)
const aviso = ref(null)
const dialogo = ref(false)
const confirmando = ref(false)
const seleccionada = ref(null)
const formulario = ref({ nombre: '', capacidad: 1, objetivoBicicletas: 0, objetivoScooters: 0 })

const encabezados = [
  { title: 'Nombre', key: 'nombre' },
  { title: 'Capacidad', key: 'capacidad' },
  { title: 'Objetivo bicicletas', key: 'objetivoBicicletas' },
  { title: 'Objetivo scooters', key: 'objetivoScooters' },
  { title: 'Estado', key: 'activa' },
  { title: 'Acciones', key: 'acciones', sortable: false, align: 'end' },
]

async function cargar() {
  cargando.value = true
  error.value = null
  try {
    estaciones.value = await http.get('/api/v1/estaciones')
  } catch (e) {
    error.value = e.message
  } finally {
    cargando.value = false
  }
}

function abrirCrear() {
  seleccionada.value = null
  formulario.value = { nombre: '', capacidad: 1, objetivoBicicletas: 0, objetivoScooters: 0 }
  error.value = null
  aviso.value = null
  dialogo.value = true
}

function abrirEditar(estacion) {
  seleccionada.value = estacion
  formulario.value = {
    nombre: estacion.nombre,
    capacidad: estacion.capacidad,
    objetivoBicicletas: estacion.objetivoBicicletas,
    objetivoScooters: estacion.objetivoScooters,
  }
  error.value = null
  aviso.value = null
  dialogo.value = true
}

async function guardar() {
  error.value = null
  const { nombre } = formulario.value
  const capacidad = Number(formulario.value.capacidad)
  const objetivoBicicletas = Number(formulario.value.objetivoBicicletas)
  const objetivoScooters = Number(formulario.value.objetivoScooters)
  if (
    !nombre.trim()
    || !Number.isInteger(capacidad) || capacidad < 1
    || !Number.isInteger(objetivoBicicletas) || objetivoBicicletas < 0
    || !Number.isInteger(objetivoScooters) || objetivoScooters < 0
    || objetivoBicicletas + objetivoScooters > capacidad
  ) {
    error.value = 'Revisa los campos: la capacidad debe ser mayor que cero y la suma de objetivos no puede superarla.'
    return
  }
  guardando.value = true
  try {
    const body = { nombre: nombre.trim(), capacidad, objetivoBicicletas, objetivoScooters }
    if (seleccionada.value) {
      await http.put(`/api/v1/estaciones/${seleccionada.value.id}`, body)
    } else {
      await http.post('/api/v1/estaciones', body)
    }
    dialogo.value = false
    aviso.value = seleccionada.value ? 'Estación actualizada.' : 'Estación creada.'
    await cargar()
  } catch (e) {
    error.value = e.message
  } finally {
    guardando.value = false
  }
}

function pedirDesactivacion(estacion) {
  seleccionada.value = estacion
  error.value = null
  confirmando.value = true
}

async function desactivar() {
  if (!seleccionada.value) return
  error.value = null
  guardando.value = true
  try {
    await http.delete(`/api/v1/estaciones/${seleccionada.value.id}`)
    confirmando.value = false
    aviso.value = 'Estación desactivada.'
    await cargar()
  } catch (e) {
    error.value = e.message
  } finally {
    guardando.value = false
  }
}

onMounted(cargar)
</script>

<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">Red MoviTBD</div>
      <h1 class="text-h4 font-weight-bold">Estaciones</h1>
      <p class="text-body-1 text-medium-emphasis">Administra sedes, capacidad y objetivos de flota.</p>
    </div>
    <v-btn color="primary" prepend-icon="mdi-plus" @click="abrirCrear">Nueva estación</v-btn>
  </div>

  <v-alert v-if="error && !dialogo && !confirmando" type="error" variant="tonal" class="mb-4">{{ error }}</v-alert>
  <v-alert v-if="aviso" type="success" variant="tonal" closable class="mb-4">{{ aviso }}</v-alert>

  <v-card>
    <v-data-table :headers="encabezados" :items="estaciones" :loading="cargando" item-value="id">
      <template #item.activa="{ value }">
        <v-chip :color="value ? 'success' : 'default'" size="small" variant="tonal">
          {{ value ? 'Activa' : 'Inactiva' }}
        </v-chip>
      </template>
      <template #item.acciones="{ item }">
        <div class="d-flex justify-end ga-1">
          <v-btn icon="mdi-pencil-outline" size="small" variant="text" aria-label="Editar estación" @click="abrirEditar(item)" />
          <v-btn
            v-if="item.activa"
            icon="mdi-cancel"
            size="small"
            variant="text"
            color="error"
            aria-label="Desactivar estación"
            @click="pedirDesactivacion(item)"
          />
        </div>
      </template>
      <template #no-data>
        <div class="pa-8 text-center text-medium-emphasis">No hay estaciones registradas.</div>
      </template>
    </v-data-table>
  </v-card>

  <v-dialog v-model="dialogo" max-width="560">
    <v-card>
      <v-card-title class="pa-6 pb-2">{{ seleccionada ? 'Editar estación' : 'Nueva estación' }}</v-card-title>
      <v-card-text class="pa-6 pt-3">
        <v-alert v-if="error" type="error" variant="tonal" class="mb-4">{{ error }}</v-alert>
        <v-form class="d-flex flex-column ga-3" @submit.prevent="guardar">
          <v-text-field v-model="formulario.nombre" label="Nombre" maxlength="100" required />
          <v-text-field v-model.number="formulario.capacidad" label="Capacidad total" type="number" min="1" step="1" required />
          <v-text-field
            v-model.number="formulario.objetivoBicicletas"
            label="Objetivo de bicicletas"
            type="number"
            min="0"
            step="1"
            required
          />
          <v-text-field
            v-model.number="formulario.objetivoScooters"
            label="Objetivo de scooters"
            type="number"
            min="0"
            step="1"
            required
          />
          <div class="d-flex justify-end ga-2 mt-2">
            <v-btn variant="text" @click="dialogo = false">Cancelar</v-btn>
            <v-btn type="submit" color="primary" :loading="guardando">Guardar</v-btn>
          </div>
        </v-form>
      </v-card-text>
    </v-card>
  </v-dialog>

  <v-dialog v-model="confirmando" max-width="440">
    <v-card>
      <v-card-title class="pa-6 pb-2">Desactivar estación</v-card-title>
      <v-card-text>¿Desactivar {{ seleccionada?.nombre }}? La API impedirá la acción si existen vehículos o viajes asociados.</v-card-text>
      <v-alert v-if="error" type="error" variant="tonal" class="mx-6">{{ error }}</v-alert>
      <v-card-actions class="pa-6 pt-4">
        <v-spacer />
        <v-btn variant="text" @click="confirmando = false">Cancelar</v-btn>
        <v-btn color="error" :loading="guardando" @click="desactivar">Desactivar</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
