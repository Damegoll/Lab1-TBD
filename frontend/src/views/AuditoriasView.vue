<script setup>
import { onMounted, ref } from 'vue'
import { http } from '@/api/http'

const auditorias = ref([])
const vehiculos = ref([])
const usuarios = ref([])
const cargando = ref(false)
const guardando = ref(false)
const error = ref(null)
const aviso = ref(null)
const dialogo = ref(false)
const confirmando = ref(false)
const seleccionado = ref(null)
const estados = ['EN_MANTENCION', 'DADO_DE_BAJA']
const formularioVacio = () => ({
  vehiculoId: null,
  estadoAnterior: null,
  estadoNuevo: 'EN_MANTENCION',
  operadorId: null,
  fecha: fechaLocal(new Date()),
  motivo: '',
})
const formulario = ref(formularioVacio())

const encabezados = [
  { title: 'Vehículo', key: 'vehiculoId' },
  { title: 'Estado anterior', key: 'estadoAnterior' },
  { title: 'Estado nuevo', key: 'estadoNuevo' },
  { title: 'Operador', key: 'operador' },
  { title: 'Fecha', key: 'fecha' },
  { title: 'Motivo', key: 'motivo' },
  { title: 'Acciones', key: 'acciones', sortable: false, align: 'end' },
]

function fechaLocal(fecha) {
  const date = new Date(fecha)
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16)
}

async function cargar() {
  cargando.value = true
  error.value = null
  try {
    const [respuestaAuditorias, respuestaVehiculos, respuestaUsuarios] = await Promise.all([
      http.get('/api/v1/auditorias'),
      http.get('/api/v1/vehiculos'),
      http.get('/api/v1/usuarios'),
    ])
    auditorias.value = respuestaAuditorias
    vehiculos.value = respuestaVehiculos
    usuarios.value = respuestaUsuarios.filter((usuario) => ['OPERADOR', 'ADMIN'].includes(usuario.rol))
  } catch (e) {
    error.value = e.message
  } finally {
    cargando.value = false
  }
}

function operadorIdDesdeNombre(username) {
  return usuarios.value.find((usuario) => usuario.username === username)?.id ?? null
}

function abrirCrear() {
  seleccionado.value = null
  formulario.value = formularioVacio()
  error.value = null
  aviso.value = null
  dialogo.value = true
}

function abrirEditar(registro) {
  seleccionado.value = registro
  formulario.value = {
    vehiculoId: registro.vehiculoId,
    estadoAnterior: registro.estadoAnterior,
    estadoNuevo: registro.estadoNuevo,
    operadorId: operadorIdDesdeNombre(registro.operador),
    fecha: fechaLocal(registro.fecha),
    motivo: registro.motivo,
  }
  error.value = null
  aviso.value = null
  dialogo.value = true
}

async function guardar() {
  error.value = null
  const vehiculoId = Number(formulario.value.vehiculoId)
  const operadorId = Number(formulario.value.operadorId)
  const motivo = formulario.value.motivo.trim()
  if (
    !Number.isInteger(vehiculoId) || vehiculoId < 1
    || !Number.isInteger(operadorId) || operadorId < 1
    || !formulario.value.estadoNuevo
    || !formulario.value.fecha
    || !motivo
  ) {
    error.value = 'Completa el vehículo, el estado, el operador, la fecha y el motivo.'
    return
  }
  if (formulario.value.estadoAnterior === formulario.value.estadoNuevo) {
    error.value = 'El estado anterior y el nuevo deben ser distintos.'
    return
  }

  guardando.value = true
  try {
    const body = {
      vehiculoId,
      estadoAnterior: formulario.value.estadoAnterior || null,
      estadoNuevo: formulario.value.estadoNuevo,
      operadorId,
      fecha: new Date(formulario.value.fecha).toISOString(),
      motivo,
    }
    if (seleccionado.value) {
      await http.put(`/api/v1/auditorias/${seleccionado.value.id}`, body)
    } else {
      await http.post('/api/v1/auditorias', body)
    }
    dialogo.value = false
    aviso.value = seleccionado.value ? 'Registro de auditoría actualizado.' : 'Registro de auditoría creado.'
    await cargar()
  } catch (e) {
    error.value = e.message
  } finally {
    guardando.value = false
  }
}

function pedirEliminacion(registro) {
  seleccionado.value = registro
  error.value = null
  confirmando.value = true
}

async function eliminar() {
  if (!seleccionado.value) return
  guardando.value = true
  error.value = null
  try {
    await http.delete(`/api/v1/auditorias/${seleccionado.value.id}`)
    confirmando.value = false
    aviso.value = 'Registro de auditoría eliminado.'
    await cargar()
  } catch (e) {
    error.value = e.message
  } finally {
    guardando.value = false
  }
}

function nombreVehiculo(id) {
  const vehiculo = vehiculos.value.find((item) => item.id === id)
  return vehiculo ? `${vehiculo.codigo} · ${vehiculo.tipo}` : `Vehículo #${id}`
}

function nombreOperador(username) {
  const usuario = usuarios.value.find((item) => item.username === username)
  return usuario ? `${usuario.nombre} (${usuario.username})` : username
}

function formatoFecha(fecha) {
  return new Intl.DateTimeFormat('es-CL', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(fecha))
}

onMounted(cargar)
</script>

<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">Administración</div>
      <h1 class="text-h4 font-weight-bold">Auditorías</h1>
      <p class="text-body-1 text-medium-emphasis">Mantenimiento manual del historial de cambios de estado de vehículos.</p>
    </div>
    <v-btn color="primary" prepend-icon="mdi-plus" @click="abrirCrear">Nuevo registro</v-btn>
  </div>

  <v-alert type="warning" variant="tonal" class="mb-4">
    Estos cambios modifican el historial de auditoría. Los cambios normales de estado se registran automáticamente.
  </v-alert>
  <v-alert v-if="error && !dialogo && !confirmando" type="error" variant="tonal" class="mb-4">{{ error }}</v-alert>
  <v-alert v-if="aviso" type="success" variant="tonal" closable class="mb-4">{{ aviso }}</v-alert>

  <v-card>
    <v-data-table :headers="encabezados" :items="auditorias" :loading="cargando" item-value="id">
      <template #item.vehiculoId="{ value }">{{ nombreVehiculo(value) }}</template>
      <template #item.estadoAnterior="{ value }">{{ value?.replaceAll('_', ' ') ?? '—' }}</template>
      <template #item.estadoNuevo="{ value }">{{ value.replaceAll('_', ' ') }}</template>
      <template #item.operador="{ value }">{{ nombreOperador(value) }}</template>
      <template #item.fecha="{ value }">{{ formatoFecha(value) }}</template>
      <template #item.acciones="{ item }">
        <div class="d-flex justify-end ga-1">
          <v-btn icon="mdi-pencil-outline" size="small" variant="text" aria-label="Editar auditoría" @click="abrirEditar(item)" />
          <v-btn icon="mdi-delete-outline" size="small" variant="text" color="error" aria-label="Eliminar auditoría" @click="pedirEliminacion(item)" />
        </div>
      </template>
      <template #no-data>
        <div class="pa-8 text-center text-medium-emphasis">No hay registros de auditoría.</div>
      </template>
    </v-data-table>
  </v-card>

  <v-dialog v-model="dialogo" max-width="600">
    <v-card>
      <v-card-title class="pa-6 pb-2">{{ seleccionado ? 'Editar auditoría' : 'Nuevo registro de auditoría' }}</v-card-title>
      <v-card-text class="pa-6 pt-3">
        <v-alert v-if="error" type="error" variant="tonal" class="mb-4">{{ error }}</v-alert>
        <v-form class="d-flex flex-column ga-3" @submit.prevent="guardar">
          <v-select
            v-model.number="formulario.vehiculoId"
            label="Vehículo"
            :items="vehiculos"
            :item-title="(item) => `${item.codigo} · ${item.tipo}`"
            item-value="id"
            required
          />
          <v-select
            v-model="formulario.estadoAnterior"
            label="Estado anterior (opcional)"
            :items="['DISPONIBLE', 'EN_USO', 'EN_MANTENCION', 'DADO_DE_BAJA']"
            clearable
          />
          <v-select v-model="formulario.estadoNuevo" label="Estado nuevo" :items="estados" required />
          <v-select
            v-model.number="formulario.operadorId"
            label="Operador responsable"
            :items="usuarios"
            :item-title="(item) => `${item.nombre} (${item.username})`"
            item-value="id"
            required
          />
          <v-text-field v-model="formulario.fecha" label="Fecha" type="datetime-local" required />
          <v-textarea v-model="formulario.motivo" label="Motivo" maxlength="500" rows="3" required />
          <div class="d-flex justify-end ga-2 mt-2">
            <v-btn variant="text" @click="dialogo = false">Cancelar</v-btn>
            <v-btn type="submit" color="primary" :loading="guardando">
              {{ seleccionado ? 'Guardar cambios' : 'Crear registro' }}
            </v-btn>
          </div>
        </v-form>
      </v-card-text>
    </v-card>
  </v-dialog>

  <v-dialog v-model="confirmando" max-width="500">
    <v-card>
      <v-card-title class="pa-6 pb-2">Eliminar auditoría</v-card-title>
      <v-card-text>
        ¿Eliminar el registro de {{ nombreVehiculo(seleccionado?.vehiculoId) }}?
        Esta acción elimina una entrada del historial.
      </v-card-text>
      <v-alert v-if="error" type="error" variant="tonal" class="mx-6">{{ error }}</v-alert>
      <v-card-actions class="pa-6 pt-4">
        <v-spacer />
        <v-btn variant="text" @click="confirmando = false">Cancelar</v-btn>
        <v-btn color="error" :loading="guardando" @click="eliminar">Eliminar registro</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
