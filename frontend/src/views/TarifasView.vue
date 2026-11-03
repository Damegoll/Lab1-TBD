<script setup>
import { onMounted, ref } from 'vue'
import { http } from '@/api/http'

const tarifas = ref([])
const cargando = ref(false)
const guardando = ref(false)
const error = ref(null)
const aviso = ref(null)
const dialogo = ref(false)
const confirmando = ref(false)
const seleccionada = ref(null)
const tipos = ['BICICLETA', 'SCOOTER']
const formulario = ref({ tipoVehiculo: 'BICICLETA', precioPorMinuto: 0, vigenteDesde: '', vigenteHasta: '' })

const encabezados = [
  { title: 'Tipo', key: 'tipoVehiculo' },
  { title: 'Precio por minuto', key: 'precioPorMinuto' },
  { title: 'Vigente desde', key: 'vigenteDesde' },
  { title: 'Vigente hasta', key: 'vigenteHasta' },
  { title: 'Estado', key: 'activa' },
  { title: 'Acciones', key: 'acciones', sortable: false, align: 'end' },
]

async function cargar() {
  cargando.value = true
  error.value = null
  try {
    tarifas.value = await http.get('/api/v1/tarifas')
  } catch (e) {
    error.value = e.message
  } finally {
    cargando.value = false
  }
}

function fechaLocal(fecha) {
  if (!fecha) return ''
  const date = new Date(fecha)
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16)
}

function fechaIso(fecha) {
  return fecha ? new Date(fecha).toISOString() : null
}

function abrirCrear() {
  seleccionada.value = null
  formulario.value = {
    tipoVehiculo: 'BICICLETA',
    precioPorMinuto: 0,
    vigenteDesde: fechaLocal(new Date()),
    vigenteHasta: '',
  }
  error.value = null
  dialogo.value = true
}

function abrirEditar(tarifa) {
  seleccionada.value = tarifa
  formulario.value = {
    tipoVehiculo: tarifa.tipoVehiculo,
    precioPorMinuto: Number(tarifa.precioPorMinuto),
    vigenteDesde: fechaLocal(tarifa.vigenteDesde),
    vigenteHasta: fechaLocal(tarifa.vigenteHasta),
  }
  error.value = null
  aviso.value = null
  dialogo.value = true
}

async function guardar() {
  error.value = null
  const precioPorMinuto = Number(formulario.value.precioPorMinuto)
  if (!Number.isFinite(precioPorMinuto) || precioPorMinuto < 0 || !formulario.value.vigenteDesde) {
    error.value = 'Ingresa un precio válido y la fecha desde la que aplica la tarifa.'
    return
  }
  if (
    formulario.value.vigenteHasta
    && new Date(formulario.value.vigenteHasta) <= new Date(formulario.value.vigenteDesde)
  ) {
    error.value = 'La fecha de término debe ser posterior a la fecha de inicio.'
    return
  }
  guardando.value = true
  try {
    const body = {
      tipoVehiculo: formulario.value.tipoVehiculo,
      precioPorMinuto,
      vigenteDesde: fechaIso(formulario.value.vigenteDesde),
      vigenteHasta: fechaIso(formulario.value.vigenteHasta),
    }
    if (seleccionada.value) {
      await http.put(`/api/v1/tarifas/${seleccionada.value.id}`, body)
    } else {
      await http.post('/api/v1/tarifas', body)
    }
    dialogo.value = false
    aviso.value = seleccionada.value ? 'Tarifa actualizada.' : 'Tarifa creada.'
    await cargar()
  } catch (e) {
    error.value = e.message
  } finally {
    guardando.value = false
  }
}

function pedirDesactivacion(tarifa) {
  seleccionada.value = tarifa
  error.value = null
  confirmando.value = true
}

async function desactivar() {
  if (!seleccionada.value) return
  error.value = null
  guardando.value = true
  try {
    await http.delete(`/api/v1/tarifas/${seleccionada.value.id}`)
    confirmando.value = false
    aviso.value = 'Tarifa desactivada.'
    await cargar()
  } catch (e) {
    error.value = e.message
  } finally {
    guardando.value = false
  }
}

function formatoFecha(fecha) {
  if (!fecha) return 'Sin fecha de término'
  return new Intl.DateTimeFormat('es-CL', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(fecha))
}

onMounted(cargar)
</script>

<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">Configuración</div>
      <h1 class="text-h4 font-weight-bold">Tarifas</h1>
      <p class="text-body-1 text-medium-emphasis">Define el precio por minuto para cada tipo de vehículo.</p>
    </div>
    <v-btn color="primary" prepend-icon="mdi-plus" @click="abrirCrear">Nueva tarifa</v-btn>
  </div>

  <v-alert v-if="error && !dialogo && !confirmando" type="error" variant="tonal" class="mb-4">{{ error }}</v-alert>
  <v-alert v-if="aviso" type="success" variant="tonal" closable class="mb-4">{{ aviso }}</v-alert>

  <v-card>
    <v-data-table :headers="encabezados" :items="tarifas" :loading="cargando" item-value="id">
      <template #item.tipoVehiculo="{ value }">
        <v-chip size="small" color="secondary" variant="tonal">{{ value }}</v-chip>
      </template>
      <template #item.precioPorMinuto="{ value }">{{ Number(value).toLocaleString('es-CL') }}</template>
      <template #item.vigenteDesde="{ value }">{{ formatoFecha(value) }}</template>
      <template #item.vigenteHasta="{ value }">{{ formatoFecha(value) }}</template>
      <template #item.activa="{ value }">
        <v-chip :color="value ? 'success' : 'default'" size="small" variant="tonal">
          {{ value ? 'Activa' : 'Inactiva' }}
        </v-chip>
      </template>
      <template #item.acciones="{ item }">
        <div class="d-flex justify-end ga-1">
          <v-btn
            v-if="item.activa"
            icon="mdi-pencil-outline"
            size="small"
            variant="text"
            aria-label="Editar tarifa"
            @click="abrirEditar(item)"
          />
          <v-btn
            v-if="item.activa"
            icon="mdi-cancel"
            size="small"
            variant="text"
            color="error"
            aria-label="Desactivar tarifa"
            @click="pedirDesactivacion(item)"
          />
        </div>
      </template>
      <template #no-data>
        <div class="pa-8 text-center text-medium-emphasis">No hay tarifas registradas.</div>
      </template>
    </v-data-table>
  </v-card>

  <v-dialog v-model="dialogo" max-width="540">
    <v-card>
      <v-card-title class="pa-6 pb-2">{{ seleccionada ? 'Editar tarifa' : 'Nueva tarifa' }}</v-card-title>
      <v-card-text class="pa-6 pt-3">
        <v-alert v-if="error" type="error" variant="tonal" class="mb-4">{{ error }}</v-alert>
        <v-form class="d-flex flex-column ga-3" @submit.prevent="guardar">
          <v-select v-model="formulario.tipoVehiculo" label="Tipo de vehículo" :items="tipos" required />
          <v-text-field
            v-model.number="formulario.precioPorMinuto"
            label="Precio por minuto"
            type="number"
            min="0"
            step="0.01"
            required
          />
          <v-text-field
            v-model="formulario.vigenteDesde"
            label="Vigente desde"
            type="datetime-local"
            required
          />
          <v-text-field
            v-model="formulario.vigenteHasta"
            label="Vigente hasta (opcional)"
            type="datetime-local"
          />
          <div class="d-flex justify-end ga-2 mt-2">
            <v-btn variant="text" @click="dialogo = false">Cancelar</v-btn>
            <v-btn type="submit" color="primary" :loading="guardando">
              {{ seleccionada ? 'Guardar cambios' : 'Crear tarifa' }}
            </v-btn>
          </div>
        </v-form>
      </v-card-text>
    </v-card>
  </v-dialog>

  <v-dialog v-model="confirmando" max-width="440">
    <v-card>
      <v-card-title class="pa-6 pb-2">Desactivar tarifa</v-card-title>
      <v-card-text>¿Desactivar la tarifa {{ seleccionada?.tipoVehiculo }}? La acción no elimina el registro.</v-card-text>
      <v-alert v-if="error" type="error" variant="tonal" class="mx-6">{{ error }}</v-alert>
      <v-card-actions class="pa-6 pt-4">
        <v-spacer />
        <v-btn variant="text" @click="confirmando = false">Cancelar</v-btn>
        <v-btn color="error" :loading="guardando" @click="desactivar">Desactivar</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
