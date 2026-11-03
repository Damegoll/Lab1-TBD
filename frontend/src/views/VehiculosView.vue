<script setup>
import { onMounted, ref } from 'vue'
import { http } from '@/api/http'

const vehiculos = ref([])
const estaciones = ref([])
const cargando = ref(false)
const guardando = ref(false)
const error = ref(null)
const aviso = ref(null)
const dialogo = ref(false)
const dialogoEstado = ref(false)
const dialogoAuditoria = ref(false)
const dialogoBaja = ref(false)
const seleccionado = ref(null)
const auditoria = ref([])
const errorAuditoria = ref(null)
const motivoBaja = ref('')
const formulario = ref({ codigo: '', tipo: 'BICICLETA', bateria: 100, estacionId: null })
const cambioEstado = ref({ estado: 'DISPONIBLE', motivo: '' })
const tipos = ['BICICLETA', 'SCOOTER']
const estados = ['DISPONIBLE', 'EN_USO', 'EN_MANTENCION', 'DADO_DE_BAJA']

const encabezados = [
  { title: 'Código', key: 'codigo' },
  { title: 'Tipo', key: 'tipo' },
  { title: 'Estado', key: 'estado' },
  { title: 'Batería', key: 'bateria' },
  { title: 'Estación', key: 'estacionId' },
  { title: 'Acciones', key: 'acciones', sortable: false, align: 'end' },
]

async function cargar() {
  cargando.value = true
  error.value = null
  try {
    const [respuestaVehiculos, respuestaEstaciones] = await Promise.allSettled([
      http.get('/api/v1/vehiculos'),
      http.get('/api/v1/estaciones'),
    ])
    if (respuestaVehiculos.status === 'rejected') throw respuestaVehiculos.reason
    vehiculos.value = respuestaVehiculos.value
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
  formulario.value = { codigo: '', tipo: 'BICICLETA', bateria: 100, estacionId: null }
  error.value = null
  dialogo.value = true
}

function abrirEditar(vehiculo) {
  seleccionado.value = vehiculo
  formulario.value = { codigo: vehiculo.codigo, tipo: vehiculo.tipo, bateria: vehiculo.bateria, estacionId: vehiculo.estacionId }
  error.value = null
  dialogo.value = true
}

async function guardar() {
  error.value = null
  const codigo = formulario.value.codigo.trim()
  const bateria = Number(formulario.value.bateria)
  const estacionId = Number(formulario.value.estacionId)
  if (!codigo || !Number.isInteger(bateria) || bateria < 0 || bateria > 100) {
    error.value = 'Ingresa un código y un nivel de batería entre 0 y 100.'
    return
  }
  if (!seleccionado.value && (!Number.isInteger(estacionId) || estacionId < 1)) {
    error.value = 'Ingresa un ID de estación válido.'
    return
  }
  guardando.value = true
  try {
    if (seleccionado.value) {
      await http.put(`/api/v1/vehiculos/${seleccionado.value.id}`, {
        codigo,
        bateria,
      })
    } else {
      await http.post('/api/v1/vehiculos', {
        codigo,
        tipo: formulario.value.tipo,
        bateria,
        estacionId,
      })
    }
    dialogo.value = false
    aviso.value = seleccionado.value ? 'Vehículo actualizado.' : 'Vehículo creado.'
    await cargar()
  } catch (e) {
    error.value = e.message
  } finally {
    guardando.value = false
  }
}

function abrirEstado(vehiculo) {
  seleccionado.value = vehiculo
  cambioEstado.value = { estado: vehiculo.estado, motivo: '' }
  error.value = null
  dialogoEstado.value = true
}

async function guardarEstado() {
  if (!seleccionado.value) return
  error.value = null
  if (!cambioEstado.value.estado || !cambioEstado.value.motivo.trim()) {
    error.value = 'Selecciona un estado e ingresa el motivo del cambio.'
    return
  }
  guardando.value = true
  try {
    await http.patch(`/api/v1/vehiculos/${seleccionado.value.id}/estado`, {
      estado: cambioEstado.value.estado,
      motivo: cambioEstado.value.motivo.trim(),
    })
    dialogoEstado.value = false
    aviso.value = 'Estado del vehículo actualizado.'
    await cargar()
  } catch (e) {
    error.value = e.message
  } finally {
    guardando.value = false
  }
}

async function verAuditoria(vehiculo) {
  seleccionado.value = vehiculo
  auditoria.value = []
  errorAuditoria.value = null
  dialogoAuditoria.value = true
  try {
    auditoria.value = await http.get(`/api/v1/vehiculos/${vehiculo.id}/auditoria`)
  } catch (e) {
    errorAuditoria.value = e.message
  }
}

function pedirBaja(vehiculo) {
  seleccionado.value = vehiculo
  motivoBaja.value = ''
  error.value = null
  dialogoBaja.value = true
}

async function darDeBaja() {
  if (!seleccionado.value) return
  const motivo = motivoBaja.value.trim()
  if (!motivo) {
    error.value = 'Describe el motivo de la baja del vehículo.'
    return
  }
  guardando.value = true
  error.value = null
  try {
    await http.delete(`/api/v1/vehiculos/${seleccionado.value.id}`, { motivo })
    dialogoBaja.value = false
    aviso.value = 'Vehículo dado de baja. El registro se conserva para mantener el historial.'
    await cargar()
  } catch (e) {
    error.value = e.message
  } finally {
    guardando.value = false
  }
}

function nombreEstacion(id) {
  return estaciones.value.find((estacion) => estacion.id === id)?.nombre ?? (id == null ? 'Sin estación' : `Estación #${id}`)
}

function colorEstado(estado) {
  if (estado === 'DISPONIBLE') return 'success'
  if (estado === 'EN_USO') return 'primary'
  if (estado === 'EN_MANTENCION') return 'warning'
  return 'error'
}

function formatoFecha(fecha) {
  return new Intl.DateTimeFormat('es-CL', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(fecha))
}

onMounted(cargar)
</script>

<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">Flota</div>
      <h1 class="text-h4 font-weight-bold">Vehículos</h1>
      <p class="text-body-1 text-medium-emphasis">Administra bicicletas y scooters de la red.</p>
    </div>
    <v-btn color="primary" prepend-icon="mdi-plus" @click="abrirCrear">Nuevo vehículo</v-btn>
  </div>

  <v-alert v-if="error && !dialogo && !dialogoEstado && !dialogoBaja" type="error" variant="tonal" class="mb-4">{{ error }}</v-alert>
  <v-alert v-if="aviso" type="success" variant="tonal" closable class="mb-4">{{ aviso }}</v-alert>

  <v-card>
    <v-data-table :headers="encabezados" :items="vehiculos" :loading="cargando" item-value="id">
      <template #item.tipo="{ value }">
        <v-chip size="small" color="secondary" variant="tonal">{{ value }}</v-chip>
      </template>
      <template #item.estado="{ value }">
        <v-chip size="small" :color="colorEstado(value)" variant="tonal">{{ value.replaceAll('_', ' ') }}</v-chip>
      </template>
      <template #item.bateria="{ value }">
        <div class="d-flex align-center ga-2 battery-cell">
          <v-progress-linear :model-value="value" color="primary" rounded height="7" />
          <span>{{ value }}%</span>
        </div>
      </template>
      <template #item.estacionId="{ value }">{{ nombreEstacion(value) }}</template>
      <template #item.acciones="{ item }">
        <div class="d-flex justify-end ga-1">
          <v-btn icon="mdi-pencil-outline" size="small" variant="text" aria-label="Editar vehículo" @click="abrirEditar(item)" />
          <v-btn icon="mdi-swap-horizontal" size="small" variant="text" aria-label="Cambiar estado" @click="abrirEstado(item)" />
          <v-btn icon="mdi-history" size="small" variant="text" aria-label="Ver auditoría" @click="verAuditoria(item)" />
          <v-btn
            v-if="item.estado !== 'DADO_DE_BAJA'"
            icon="mdi-delete-off-outline"
            size="small"
            variant="text"
            color="error"
            aria-label="Dar de baja vehículo"
            @click="pedirBaja(item)"
          />
        </div>
      </template>
      <template #no-data>
        <div class="pa-8 text-center text-medium-emphasis">No hay vehículos registrados.</div>
      </template>
    </v-data-table>
  </v-card>

  <v-dialog v-model="dialogo" max-width="540">
    <v-card>
      <v-card-title class="pa-6 pb-2">{{ seleccionado ? 'Editar vehículo' : 'Nuevo vehículo' }}</v-card-title>
      <v-card-text class="pa-6 pt-3">
        <v-alert v-if="error" type="error" variant="tonal" class="mb-4">{{ error }}</v-alert>
        <v-form class="d-flex flex-column ga-3" @submit.prevent="guardar">
          <v-text-field v-model="formulario.codigo" label="Código" maxlength="40" required />
          <v-select v-if="!seleccionado" v-model="formulario.tipo" label="Tipo" :items="tipos" required />
          <v-text-field
            v-model.number="formulario.bateria"
            label="Batería (%)"
            type="number"
            min="0"
            max="100"
            required
          />
          <v-select
            v-if="!seleccionado"
            v-model.number="formulario.estacionId"
            label="Estación"
            :items="estaciones"
            item-title="nombre"
            item-value="id"
            no-data-text="No hay estaciones activas"
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

  <v-dialog v-model="dialogoEstado" max-width="500">
    <v-card>
      <v-card-title class="pa-6 pb-2">Cambiar estado</v-card-title>
      <v-card-text class="pa-6 pt-3">
        <p class="text-body-2 mb-4">Vehículo {{ seleccionado?.codigo }}</p>
        <v-alert v-if="error" type="error" variant="tonal" class="mb-4">{{ error }}</v-alert>
        <v-form class="d-flex flex-column ga-3" @submit.prevent="guardarEstado">
          <v-select v-model="cambioEstado.estado" label="Nuevo estado" :items="estados" required />
          <v-textarea v-model="cambioEstado.motivo" label="Motivo" maxlength="500" rows="3" required />
          <div class="d-flex justify-end ga-2 mt-2">
            <v-btn variant="text" @click="dialogoEstado = false">Cancelar</v-btn>
            <v-btn type="submit" color="primary" :loading="guardando">Guardar estado</v-btn>
          </div>
        </v-form>
      </v-card-text>
    </v-card>
  </v-dialog>

  <v-dialog v-model="dialogoBaja" max-width="500">
    <v-card>
      <v-card-title class="pa-6 pb-2">Dar de baja vehículo</v-card-title>
      <v-card-text class="pa-6 pt-3">
        <p class="mb-4">
          El vehículo {{ seleccionado?.codigo }} quedará como dado de baja. No se elimina físicamente y el cambio quedará auditado.
        </p>
        <v-alert v-if="error" type="error" variant="tonal" class="mb-4">{{ error }}</v-alert>
        <v-textarea v-model="motivoBaja" label="Motivo de la baja" maxlength="500" rows="3" required />
      </v-card-text>
      <v-card-actions class="px-6 pb-5">
        <v-spacer />
        <v-btn variant="text" @click="dialogoBaja = false">Cancelar</v-btn>
        <v-btn color="error" :loading="guardando" @click="darDeBaja">Confirmar baja</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>

  <v-dialog v-model="dialogoAuditoria" max-width="900">
    <v-card>
      <v-card-title class="pa-6 pb-2">Historial de {{ seleccionado?.codigo }}</v-card-title>
      <v-card-text class="pa-6 pt-3">
        <v-alert v-if="errorAuditoria" type="error" variant="tonal">{{ errorAuditoria }}</v-alert>
        <v-table v-else-if="auditoria.length" density="comfortable">
          <thead>
            <tr><th>Fecha</th><th>Operador</th><th>Cambio</th><th>Motivo</th></tr>
          </thead>
          <tbody>
            <tr v-for="registro in auditoria" :key="registro.id">
              <td>{{ formatoFecha(registro.fecha) }}</td>
              <td>{{ registro.operador }}</td>
              <td>{{ registro.estadoAnterior ?? '—' }} → {{ registro.estadoNuevo }}</td>
              <td>{{ registro.motivo }}</td>
            </tr>
          </tbody>
        </v-table>
        <p v-else class="text-medium-emphasis">No hay cambios registrados para este vehículo.</p>
      </v-card-text>
      <v-card-actions class="px-6 pb-5">
        <v-spacer />
        <v-btn variant="text" @click="dialogoAuditoria = false">Cerrar</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
