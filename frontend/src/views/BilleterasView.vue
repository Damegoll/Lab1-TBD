<script setup>
import { computed, onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { http } from '@/api/http'

const auth = useAuthStore()
const esStaff = computed(() => auth.rol === 'OPERADOR' || auth.esAdmin)
const billeteras = ref([])
const usuarios = ref([])
const movimientos = ref([])
const seleccionada = ref(null)
const cargando = ref(false)
const cargandoMovimientos = ref(false)
const guardando = ref(false)
const error = ref(null)
const errorMovimientos = ref(null)
const aviso = ref(null)
const dialogoCrear = ref(false)
const dialogoAjuste = ref(false)
const confirmando = ref(false)
const nuevoUsuarioId = ref(null)
const ajuste = ref({ tipo: 'ABONO', monto: null, motivo: '' })

const encabezados = [
  { title: 'Billetera', key: 'id' },
  { title: 'Usuario ID', key: 'usuarioId' },
  { title: 'Saldo', key: 'saldo' },
  { title: 'Estado', key: 'activa' },
  { title: 'Acciones', key: 'acciones', sortable: false, align: 'end' },
]

const encabezadosMovimientos = [
  { title: 'Fecha', key: 'creadoEn' },
  { title: 'Tipo', key: 'tipo' },
  { title: 'Monto', key: 'monto' },
  { title: 'Motivo', key: 'motivo' },
  { title: 'Operador', key: 'operador' },
]

function formatoDinero(monto) {
  return new Intl.NumberFormat('es-CL', { style: 'currency', currency: 'CLP', maximumFractionDigits: 2 }).format(Number(monto))
}

function formatoFecha(fecha) {
  return new Intl.DateTimeFormat('es-CL', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(fecha))
}

async function cargar() {
  cargando.value = true
  error.value = null
  try {
    if (esStaff.value) {
      billeteras.value = await http.get('/api/v1/billeteras')
    } else {
      const propia = await http.get('/api/v1/billeteras/me')
      billeteras.value = [propia]
      seleccionada.value = propia
    }
    if (seleccionada.value) await cargarMovimientos(seleccionada.value)
  } catch (e) {
    error.value = e.message
  } finally {
    cargando.value = false
  }
}

async function cargarMovimientos(billetera) {
  seleccionada.value = billetera
  cargandoMovimientos.value = true
  errorMovimientos.value = null
  try {
    movimientos.value = await http.get(`/api/v1/billeteras/${billetera.id}/movimientos`)
  } catch (e) {
    errorMovimientos.value = e.message
  } finally {
    cargandoMovimientos.value = false
  }
}

async function abrirCrear() {
  nuevoUsuarioId.value = null
  error.value = null
  guardando.value = true
  try {
    usuarios.value = (await http.get('/api/v1/usuarios')).filter((usuario) => usuario.activo)
  } catch (e) {
    error.value = e.message
    return
  } finally {
    guardando.value = false
  }
  dialogoCrear.value = true
}

async function crear() {
  error.value = null
  const usuarioId = Number(nuevoUsuarioId.value)
  if (!Number.isInteger(usuarioId) || usuarioId < 1) {
    error.value = 'Ingresa un ID de usuario válido.'
    return
  }
  guardando.value = true
  try {
    await http.post('/api/v1/billeteras', { usuarioId })
    dialogoCrear.value = false
    aviso.value = 'Billetera creada.'
    await cargar()
  } catch (e) {
    error.value = e.message
  } finally {
    guardando.value = false
  }
}

function abrirAjuste(billetera, tipo) {
  seleccionada.value = billetera
  ajuste.value = { tipo, monto: null, motivo: '' }
  error.value = null
  dialogoAjuste.value = true
}

async function guardarAjuste() {
  error.value = null
  const monto = Number(ajuste.value.monto)
  const permiteCero = ajuste.value.tipo === 'SALDO'
  if (!Number.isFinite(monto) || monto < 0 || (!permiteCero && monto === 0) || !ajuste.value.motivo.trim()) {
    error.value = `${permiteCero ? 'Ingresa un saldo igual o mayor que cero' : 'Ingresa un monto mayor que cero'} y el motivo del movimiento.`
    return
  }
  guardando.value = true
  try {
    if (ajuste.value.tipo === 'SALDO') {
      await http.put(`/api/v1/billeteras/${seleccionada.value.id}`, {
        saldo: monto,
        motivo: ajuste.value.motivo.trim(),
      })
    } else {
      await http.patch(
        `/api/v1/billeteras/${seleccionada.value.id}/movimientos/${ajuste.value.tipo}`,
        { monto, motivo: ajuste.value.motivo.trim() },
      )
    }
    dialogoAjuste.value = false
    aviso.value = ajuste.value.tipo === 'SALDO'
      ? 'Saldo actualizado y movimiento contable registrado.'
      : ajuste.value.tipo === 'ABONO' ? 'Abono registrado.' : 'Cargo registrado.'
    await cargar()
  } catch (e) {
    error.value = e.message
  } finally {
    guardando.value = false
  }
}

function pedirDesactivacion(billetera) {
  seleccionada.value = billetera
  error.value = null
  confirmando.value = true
}

async function desactivar() {
  if (!seleccionada.value) return
  error.value = null
  guardando.value = true
  try {
    await http.delete(`/api/v1/billeteras/${seleccionada.value.id}`)
    confirmando.value = false
    aviso.value = 'Billetera desactivada.'
    seleccionada.value = null
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
      <div class="eyebrow">{{ esStaff ? 'Administración financiera' : 'Tu cuenta' }}</div>
      <h1 class="text-h4 font-weight-bold">{{ esStaff ? 'Billeteras' : 'Mi billetera' }}</h1>
      <p class="text-body-1 text-medium-emphasis">
        {{ esStaff ? 'Consulta saldos y registra movimientos.' : 'Consulta tu saldo y el historial de movimientos.' }}
      </p>
    </div>
    <v-btn v-if="auth.esAdmin" color="primary" prepend-icon="mdi-wallet-plus-outline" @click="abrirCrear">
      Crear billetera
    </v-btn>
  </div>

  <v-alert v-if="error && !dialogoCrear && !dialogoAjuste && !confirmando" type="error" variant="tonal" class="mb-4">{{ error }}</v-alert>
  <v-alert v-if="aviso" type="success" variant="tonal" closable class="mb-4">{{ aviso }}</v-alert>

  <v-row>
    <v-col v-if="esStaff" cols="12" :lg="seleccionada ? 7 : 12">
      <v-card>
        <v-data-table :headers="encabezados" :items="billeteras" :loading="cargando" item-value="id">
          <template #item.saldo="{ value }">{{ formatoDinero(value) }}</template>
          <template #item.activa="{ value }">
            <v-chip :color="value ? 'success' : 'default'" size="small" variant="tonal">
              {{ value ? 'Activa' : 'Inactiva' }}
            </v-chip>
          </template>
          <template #item.acciones="{ item }">
            <div class="d-flex justify-end ga-1">
              <v-btn icon="mdi-history" size="small" variant="text" aria-label="Ver movimientos" @click="cargarMovimientos(item)" />
              <v-btn
                v-if="item.activa"
                icon="mdi-plus-circle-outline"
                size="small"
                variant="text"
                color="success"
                aria-label="Registrar abono"
                @click="abrirAjuste(item, 'ABONO')"
              />
              <v-btn
                v-if="item.activa"
                icon="mdi-minus-circle-outline"
                size="small"
                variant="text"
                color="secondary"
                aria-label="Registrar cargo"
                @click="abrirAjuste(item, 'CARGO')"
              />
              <v-btn
                v-if="item.activa"
                icon="mdi-pencil-outline"
                size="small"
                variant="text"
                aria-label="Editar saldo"
                @click="abrirAjuste(item, 'SALDO')"
              />
              <v-btn
                v-if="auth.esAdmin && item.activa"
                icon="mdi-cancel"
                size="small"
                variant="text"
                color="error"
                aria-label="Desactivar billetera"
                @click="pedirDesactivacion(item)"
              />
            </div>
          </template>
          <template #no-data>
            <div class="pa-8 text-center text-medium-emphasis">No hay billeteras registradas.</div>
          </template>
        </v-data-table>
      </v-card>
    </v-col>

    <v-col v-if="!esStaff && billeteras[0]" cols="12" md="5">
      <v-card class="pa-6 wallet-balance-card">
        <v-avatar color="white" variant="tonal" class="mb-4">
          <v-icon icon="mdi-wallet-outline" />
        </v-avatar>
        <div class="text-body-2">Saldo disponible</div>
        <div class="text-h3 font-weight-bold mt-2">{{ formatoDinero(billeteras[0].saldo) }}</div>
        <div class="mt-4">
          <v-chip :color="billeteras[0].activa ? 'success' : 'error'" size="small" variant="tonal">
            {{ billeteras[0].activa ? 'Activa' : 'Inactiva' }}
          </v-chip>
        </div>
      </v-card>
    </v-col>

    <v-col v-if="seleccionada" cols="12" :lg="esStaff ? 5 : 7">
      <v-card>
        <div class="d-flex align-center justify-space-between pa-5 pb-2">
          <div>
            <h2 class="text-h6">Movimientos</h2>
            <p class="text-caption text-medium-emphasis">Billetera #{{ seleccionada.id }}</p>
          </div>
          <v-btn icon="mdi-refresh" variant="text" aria-label="Actualizar movimientos" :loading="cargandoMovimientos" @click="cargarMovimientos(seleccionada)" />
        </div>
        <v-alert v-if="errorMovimientos" type="error" variant="tonal" class="mx-5 mb-3">{{ errorMovimientos }}</v-alert>
        <v-data-table
          v-else
          :headers="encabezadosMovimientos"
          :items="movimientos"
          :loading="cargandoMovimientos"
          item-value="id"
          density="compact"
          :items-per-page="5"
        >
          <template #item.creadoEn="{ value }">{{ formatoFecha(value) }}</template>
          <template #item.tipo="{ value }">
            <v-chip :color="value === 'ABONO' ? 'success' : 'secondary'" size="small" variant="tonal">{{ value }}</v-chip>
          </template>
          <template #item.monto="{ value }">{{ formatoDinero(value) }}</template>
          <template #no-data>
            <div class="pa-6 text-center text-medium-emphasis">No hay movimientos registrados.</div>
          </template>
        </v-data-table>
      </v-card>
    </v-col>
  </v-row>

  <v-skeleton-loader v-if="cargando && !billeteras.length" type="card" />

  <v-dialog v-model="dialogoCrear" max-width="460">
    <v-card>
      <v-card-title class="pa-6 pb-2">Crear billetera</v-card-title>
      <v-card-text class="pa-6 pt-3">
        <v-alert v-if="error" type="error" variant="tonal" class="mb-4">{{ error }}</v-alert>
        <v-form @submit.prevent="crear">
          <v-select
            v-model.number="nuevoUsuarioId"
            label="Usuario"
            :items="usuarios"
            :item-title="(usuario) => `${usuario.username} (ID ${usuario.id})`"
            item-value="id"
            no-data-text="No hay usuarios activos disponibles"
            required
          />
          <div class="d-flex justify-end ga-2 mt-4">
            <v-btn variant="text" @click="dialogoCrear = false">Cancelar</v-btn>
            <v-btn type="submit" color="primary" :loading="guardando">Crear</v-btn>
          </div>
        </v-form>
      </v-card-text>
    </v-card>
  </v-dialog>

  <v-dialog v-model="dialogoAjuste" max-width="480">
    <v-card>
      <v-card-title class="pa-6 pb-2">
        {{ ajuste.tipo === 'SALDO' ? 'Editar saldo' : ajuste.tipo === 'ABONO' ? 'Registrar abono' : 'Registrar cargo' }}
      </v-card-title>
      <v-card-text class="pa-6 pt-3">
        <v-alert v-if="error" type="error" variant="tonal" class="mb-4">{{ error }}</v-alert>
        <v-form class="d-flex flex-column ga-3" @submit.prevent="guardarAjuste">
          <v-text-field
            v-model.number="ajuste.monto"
            :label="ajuste.tipo === 'SALDO' ? 'Nuevo saldo' : 'Monto'"
            type="number"
            :min="ajuste.tipo === 'SALDO' ? 0 : 0.01"
            step="0.01"
            required
          />
          <v-textarea v-model="ajuste.motivo" label="Motivo" maxlength="500" rows="3" required />
          <div class="d-flex justify-end ga-2 mt-2">
            <v-btn variant="text" @click="dialogoAjuste = false">Cancelar</v-btn>
            <v-btn type="submit" color="primary" :loading="guardando">Registrar</v-btn>
          </div>
        </v-form>
      </v-card-text>
    </v-card>
  </v-dialog>

  <v-dialog v-model="confirmando" max-width="480">
    <v-card>
      <v-card-title class="pa-6 pb-2">Desactivar billetera</v-card-title>
      <v-card-text>
        Para desactivarla, el saldo debe ser cero y no debe tener viajes activos. El servidor validará ambas condiciones.
      </v-card-text>
      <v-alert v-if="error" type="error" variant="tonal" class="mx-6">{{ error }}</v-alert>
      <v-card-actions class="pa-6 pt-4">
        <v-spacer />
        <v-btn variant="text" @click="confirmando = false">Cancelar</v-btn>
        <v-btn color="error" :loading="guardando" @click="desactivar">Desactivar</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
