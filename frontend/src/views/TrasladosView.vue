<script setup>
import { onMounted, ref } from 'vue'
import { http } from '@/api/http'

const traslados = ref([])
const estaciones = ref([])
const cargando = ref(false)
const guardando = ref(false)
const error = ref(null)
const aviso = ref(null)
const dialogo = ref(false)
const confirmando = ref(false)
const seleccionado = ref(null)
const tipos = ['BICICLETA', 'SCOOTER']
const formulario = ref({ estacionOrigenId: null, estacionDestinoId: null, tipoVehiculo: 'BICICLETA', cantidad: 1 })

const encabezados = [
  { title: 'Solicitud', key: 'id' },
  { title: 'Origen', key: 'estacionOrigenId' },
  { title: 'Destino', key: 'estacionDestinoId' },
  { title: 'Tipo', key: 'tipoVehiculo' },
  { title: 'Cantidad', key: 'cantidad' },
  { title: 'Operador', key: 'operadorId' },
  { title: 'Fecha', key: 'creadaEn' },
  { title: 'Acciones', key: 'acciones', sortable: false, align: 'end' },
]

async function cargar() {
  cargando.value = true
  error.value = null
  try {
    const [respuestaTraslados, respuestaEstaciones] = await Promise.allSettled([
      http.get('/api/v1/ordenes-traslado'),
      http.get('/api/v1/estaciones'),
    ])
    if (respuestaTraslados.status === 'rejected') throw respuestaTraslados.reason
    traslados.value = respuestaTraslados.value
    if (respuestaEstaciones.status === 'fulfilled') {
      estaciones.value = respuestaEstaciones.value.filter((estacion) => estacion.activa)
    } else {
      error.value = `No fue posible cargar las estaciones: ${respuestaEstaciones.reason.message}`
    }
  } catch (e) {
    error.value = e.message
  } finally {
    cargando.value = false
  }
}

function abrirCrear() {
  seleccionado.value = null
  formulario.value = { estacionOrigenId: null, estacionDestinoId: null, tipoVehiculo: 'BICICLETA', cantidad: 1 }
  error.value = null
  dialogo.value = true
}

function abrirEditar(traslado) {
  seleccionado.value = traslado
  formulario.value = { ...formulario.value, cantidad: traslado.cantidad }
  error.value = null
  dialogo.value = true
}

async function guardar() {
  error.value = null
  aviso.value = null
  const estacionOrigenId = Number(formulario.value.estacionOrigenId)
  const estacionDestinoId = Number(formulario.value.estacionDestinoId)
  const cantidad = Number(formulario.value.cantidad)
  if (!Number.isInteger(cantidad) || cantidad < 1) {
    error.value = 'La cantidad debe ser un número entero mayor que cero.'
    return
  }
  if (!seleccionado.value && (
    !Number.isInteger(estacionOrigenId) || estacionOrigenId < 1
    || !Number.isInteger(estacionDestinoId) || estacionDestinoId < 1
  )) {
    error.value = 'Selecciona estaciones de origen y destino válidas.'
    return
  }
  guardando.value = true
  try {
    if (!seleccionado.value && estacionOrigenId === estacionDestinoId) {
      error.value = 'La estación de origen y destino deben ser distintas.'
      return
    }

    if (seleccionado.value) {
      await http.put(`/api/v1/ordenes-traslado/${seleccionado.value.id}`, { cantidad })
    } else {
      await http.post('/api/v1/ordenes-traslado', {
        estacionOrigenId,
        estacionDestinoId,
        tipoVehiculo: formulario.value.tipoVehiculo,
        cantidad,
      })
    }
    dialogo.value = false
    aviso.value = seleccionado.value ? 'Orden ajustada y flota actualizada.' : 'Orden de traslado creada.'
    await cargar()
  } catch (e) {
    error.value = e.message
  } finally {
    guardando.value = false
  }
}

function pedirReversion(traslado) {
  seleccionado.value = traslado
  error.value = null
  confirmando.value = true
}

async function revertir() {
  if (!seleccionado.value) return
  error.value = null
  guardando.value = true
  try {
    await http.delete(`/api/v1/ordenes-traslado/${seleccionado.value.id}`)
    confirmando.value = false
    aviso.value = 'Orden revertida y flota actualizada.'
    await cargar()
  } catch (e) {
    error.value = e.message
  } finally {
    guardando.value = false
  }
}

function nombreEstacion(id) {
  return estaciones.value.find((estacion) => estacion.id === id)?.nombre ?? `Estación #${id}`
}

function formatoFecha(fecha) {
  return new Intl.DateTimeFormat('es-CL', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(fecha))
}

onMounted(cargar)
</script>

<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">Operaciones</div>
      <h1 class="text-h4 font-weight-bold">Traslados</h1>
      <p class="text-body-1 text-medium-emphasis">Rebalancea vehículos entre estaciones.</p>
    </div>
    <v-btn color="primary" prepend-icon="mdi-plus" @click="abrirCrear">Nuevo traslado</v-btn>
  </div>

  <v-alert v-if="error && !dialogo && !confirmando" type="error" variant="tonal" class="mb-4">{{ error }}</v-alert>
  <v-alert v-if="aviso" type="success" variant="tonal" closable class="mb-4">{{ aviso }}</v-alert>

  <v-card>
    <v-data-table :headers="encabezados" :items="traslados" :loading="cargando" item-value="id">
      <template #item.estacionOrigenId="{ value }">{{ nombreEstacion(value) }}</template>
      <template #item.estacionDestinoId="{ value }">{{ nombreEstacion(value) }}</template>
      <template #item.tipoVehiculo="{ value }">
        <v-chip size="small" color="secondary" variant="tonal">{{ value }}</v-chip>
      </template>
      <template #item.creadaEn="{ value }">{{ formatoFecha(value) }}</template>
      <template #item.acciones="{ item }">
        <div class="d-flex justify-end ga-1">
          <v-btn icon="mdi-pencil-outline" size="small" variant="text" aria-label="Ajustar orden" @click="abrirEditar(item)" />
          <v-btn
            icon="mdi-undo"
            size="small"
            variant="text"
            color="error"
            aria-label="Revertir orden"
            @click="pedirReversion(item)"
          />
        </div>
      </template>
      <template #no-data>
        <div class="pa-8 text-center text-medium-emphasis">No hay órdenes de traslado registradas.</div>
      </template>
    </v-data-table>
  </v-card>

  <v-dialog v-model="dialogo" max-width="540">
    <v-card>
      <v-card-title class="pa-6 pb-2">{{ seleccionado ? 'Ajustar orden de traslado' : 'Nuevo traslado' }}</v-card-title>
      <v-card-text class="pa-6 pt-3">
        <v-alert v-if="seleccionado" type="info" variant="tonal" density="comfortable" class="mb-4">
          Al cambiar la cantidad, el servidor ajusta también el inventario de las dos estaciones.
        </v-alert>
        <v-alert v-if="error" type="error" variant="tonal" class="mb-4">{{ error }}</v-alert>
        <v-form class="d-flex flex-column ga-3" @submit.prevent="guardar">
          <v-select
            v-if="!seleccionado"
            v-model.number="formulario.estacionOrigenId"
            label="Estación de origen"
            :items="estaciones"
            item-title="nombre"
            item-value="id"
            no-data-text="No hay estaciones activas"
            required
          />
          <v-select
            v-if="!seleccionado"
            v-model.number="formulario.estacionDestinoId"
            label="Estación de destino"
            :items="estaciones"
            item-title="nombre"
            item-value="id"
            no-data-text="No hay estaciones activas"
            required
          />
          <v-select v-if="!seleccionado" v-model="formulario.tipoVehiculo" label="Tipo de vehículo" :items="tipos" required />
          <v-text-field
            v-model.number="formulario.cantidad"
            label="Cantidad"
            type="number"
            min="1"
            step="1"
            required
          />
          <div class="d-flex justify-end ga-2 mt-2">
            <v-btn variant="text" @click="dialogo = false">Cancelar</v-btn>
            <v-btn type="submit" color="primary" :loading="guardando">
              {{ seleccionado ? 'Aplicar ajuste' : 'Crear traslado' }}
            </v-btn>
          </div>
        </v-form>
      </v-card-text>
    </v-card>
  </v-dialog>

  <v-dialog v-model="confirmando" max-width="500">
    <v-card>
      <v-card-title class="pa-6 pb-2">Revertir orden de traslado</v-card-title>
      <v-card-text>
        Se intentará devolver {{ seleccionado?.cantidad }} {{ seleccionado?.tipoVehiculo?.toLowerCase() }}(s)
        de {{ nombreEstacion(seleccionado?.estacionDestinoId) }} a {{ nombreEstacion(seleccionado?.estacionOrigenId) }}.
        La operación solo se realiza si el inventario y la capacidad lo permiten.
      </v-card-text>
      <v-alert v-if="error" type="error" variant="tonal" class="mx-6">{{ error }}</v-alert>
      <v-card-actions class="pa-6 pt-4">
        <v-spacer />
        <v-btn variant="text" @click="confirmando = false">Volver</v-btn>
        <v-btn color="error" :loading="guardando" @click="revertir">Revertir orden</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
