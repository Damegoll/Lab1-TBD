<script setup>
import { onMounted, ref } from 'vue'
import { http } from '@/api/http'

const usuarios = ref([])
const cargando = ref(false)
const guardando = ref(false)
const error = ref(null)
const exito = ref(null)
const dialogo = ref(false)
const confirmando = ref(false)
const resumenDialogo = ref(false)
const cargandoResumen = ref(false)
const errorResumen = ref(null)
const resumenes = ref([])
const seleccionado = ref(null)
const formulario = ref({ username: '', nombre: '', email: '', password: '', rol: 'USUARIO' })
const roles = ['USUARIO', 'OPERADOR', 'ADMIN']

const encabezados = [
  { title: 'Usuario', key: 'username' },
  { title: 'Nombre', key: 'nombre' },
  { title: 'Correo', key: 'email' },
  { title: 'Rol', key: 'rol' },
  { title: 'Estado', key: 'activo' },
  { title: 'Acciones', key: 'acciones', sortable: false, align: 'end' },
]

async function cargar() {
  cargando.value = true
  error.value = null
  try {
    usuarios.value = await http.get('/api/v1/usuarios')
  } catch (e) {
    error.value = e.message
  } finally {
    cargando.value = false
  }
}

function abrirCrear() {
  seleccionado.value = null
  formulario.value = { username: '', nombre: '', email: '', password: '', rol: 'USUARIO' }
  error.value = null
  exito.value = null
  dialogo.value = true
}

function abrirEditar(usuario) {
  seleccionado.value = usuario
  formulario.value = {
    username: usuario.username,
    nombre: usuario.nombre,
    email: usuario.email,
    password: '',
    rol: usuario.rol,
  }
  error.value = null
  exito.value = null
  dialogo.value = true
}

async function guardar() {
  error.value = null
  exito.value = null
  const { username, nombre, email, password, rol } = formulario.value
  if (
    !nombre.trim()
    || !email.trim()
    || !rol
    || (!seleccionado.value && (!username.trim() || password.length < 12))
    || (password && (password.length < 12 || password.length > 72))
  ) {
    error.value = 'Completa los campos obligatorios. Si defines una contraseña, debe tener al menos 12 caracteres.'
    return
  }
  guardando.value = true
  try {
    if (seleccionado.value) {
      await http.put(`/api/v1/usuarios/${seleccionado.value.id}`, {
        nombre,
        email,
        rol,
        ...(password ? { password } : {}),
      })
    } else {
      await http.post('/api/v1/usuarios', { username, nombre, email, password, rol })
    }
    dialogo.value = false
    exito.value = seleccionado.value ? 'Usuario actualizado.' : 'Usuario creado.'
    await cargar()
  } catch (e) {
    error.value = e.message
  } finally {
    guardando.value = false
  }
}

function pedirDesactivacion(usuario) {
  seleccionado.value = usuario
  error.value = null
  confirmando.value = true
}

async function desactivar() {
  if (!seleccionado.value) return
  error.value = null
  guardando.value = true
  try {
    await http.delete(`/api/v1/usuarios/${seleccionado.value.id}`)
    confirmando.value = false
    exito.value = 'Usuario desactivado.'
    await cargar()
  } catch (e) {
    error.value = e.message
  } finally {
    guardando.value = false
  }
}

async function verResumen(usuario) {
  seleccionado.value = usuario
  resumenes.value = []
  errorResumen.value = null
  resumenDialogo.value = true
  cargandoResumen.value = true
  try {
    resumenes.value = await http.get(`/api/v1/usuarios/${usuario.id}/resumen-mensual`)
  } catch (e) {
    errorResumen.value = e.message
  } finally {
    cargandoResumen.value = false
  }
}

function nombreMes(anio, mes) {
  return new Intl.DateTimeFormat('es-CL', { month: 'long', year: 'numeric' })
    .format(new Date(Number(anio), Number(mes) - 1, 1))
}

function formatoDinero(monto) {
  return new Intl.NumberFormat('es-CL', { style: 'currency', currency: 'CLP', maximumFractionDigits: 2 }).format(Number(monto ?? 0))
}

onMounted(cargar)
</script>

<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">Administración</div>
      <h1 class="text-h4 font-weight-bold">Usuarios</h1>
      <p class="text-body-1 text-medium-emphasis">Administra las cuentas y sus permisos.</p>
    </div>
    <v-btn color="primary" prepend-icon="mdi-account-plus" @click="abrirCrear">Nuevo usuario</v-btn>
  </div>

  <v-alert v-if="error && !dialogo && !confirmando && !resumenDialogo" type="error" variant="tonal" class="mb-4">{{ error }}</v-alert>
  <v-alert v-if="exito" type="success" variant="tonal" closable class="mb-4">{{ exito }}</v-alert>

  <v-card>
    <v-data-table :headers="encabezados" :items="usuarios" :loading="cargando" item-value="id">
      <template #item.rol="{ value }">
        <v-chip size="small" color="tertiary" variant="tonal">{{ value }}</v-chip>
      </template>
      <template #item.activo="{ value }">
        <v-chip :color="value ? 'success' : 'error'" size="small" variant="tonal">
          {{ value ? 'Activo' : 'Inactivo' }}
        </v-chip>
      </template>
      <template #item.acciones="{ item }">
        <div class="d-flex justify-end ga-1">
          <v-btn icon="mdi-chart-box-outline" size="small" variant="text" aria-label="Ver resumen mensual" @click="verResumen(item)" />
          <v-btn icon="mdi-pencil-outline" size="small" variant="text" aria-label="Editar usuario" @click="abrirEditar(item)" />
          <v-btn
            v-if="item.activo"
            icon="mdi-account-cancel-outline"
            size="small"
            variant="text"
            color="error"
            aria-label="Desactivar usuario"
            @click="pedirDesactivacion(item)"
          />
        </div>
      </template>
      <template #no-data>
        <div class="pa-8 text-center text-medium-emphasis">No hay usuarios registrados.</div>
      </template>
    </v-data-table>
  </v-card>

  <v-dialog v-model="dialogo" max-width="560">
    <v-card>
      <v-card-title class="pa-6 pb-2">{{ seleccionado ? 'Editar usuario' : 'Nuevo usuario' }}</v-card-title>
      <v-card-text class="pa-6 pt-3">
        <v-alert v-if="error" type="error" variant="tonal" class="mb-4">{{ error }}</v-alert>
        <v-form class="d-flex flex-column ga-3" @submit.prevent="guardar">
          <v-text-field
            v-if="!seleccionado"
            v-model="formulario.username"
            label="Usuario"
            maxlength="50"
            required
          />
          <v-text-field v-model="formulario.nombre" label="Nombre" maxlength="100" required />
          <v-text-field v-model="formulario.email" label="Correo" type="email" maxlength="254" required />
          <v-text-field
            v-model="formulario.password"
            :label="seleccionado ? 'Nueva contraseña (opcional, mínimo 12 caracteres)' : 'Contraseña (mínimo 12 caracteres)'"
            type="password"
            :minlength="seleccionado ? undefined : 12"
            maxlength="72"
            :required="!seleccionado"
          />
          <v-select v-model="formulario.rol" label="Rol" :items="roles" required />
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
      <v-card-title class="pa-6 pb-2">Desactivar usuario</v-card-title>
      <v-card-text>¿Desactivar la cuenta de {{ seleccionado?.username }}? No se eliminarán sus datos.</v-card-text>
      <v-alert v-if="error" type="error" variant="tonal" class="mx-6">{{ error }}</v-alert>
      <v-card-actions class="pa-6 pt-4">
        <v-spacer />
        <v-btn variant="text" @click="confirmando = false">Cancelar</v-btn>
        <v-btn color="error" :loading="guardando" @click="desactivar">Desactivar</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>

  <v-dialog v-model="resumenDialogo" max-width="680">
    <v-card>
      <v-card-title class="pa-6 pb-2">Resumen mensual · {{ seleccionado?.username }}</v-card-title>
      <v-card-text class="pa-6 pt-3">
        <v-alert v-if="errorResumen" type="error" variant="tonal">{{ errorResumen }}</v-alert>
        <v-skeleton-loader v-else-if="cargandoResumen" type="list-item-three-line, list-item-three-line" />
        <v-row v-else-if="resumenes.length">
          <v-col v-for="resumen in resumenes" :key="`${resumen.anio}-${resumen.mes}`" cols="12" sm="6">
            <v-card variant="tonal" class="pa-4">
              <div class="eyebrow mb-2">{{ nombreMes(resumen.anio, resumen.mes) }}</div>
              <div class="summary-metric"><span>Viajes</span><strong>{{ resumen.cantidadViajes ?? 0 }}</strong></div>
              <div class="summary-metric"><span>Minutos</span><strong>{{ Number(resumen.minutosTotales ?? 0).toLocaleString('es-CL', { maximumFractionDigits: 1 }) }}</strong></div>
              <div class="summary-metric"><span>Gasto</span><strong>{{ formatoDinero(resumen.gastoTotal) }}</strong></div>
            </v-card>
          </v-col>
        </v-row>
        <p v-else class="text-medium-emphasis">No hay resúmenes mensuales para este usuario.</p>
      </v-card-text>
      <v-card-actions class="px-6 pb-5">
        <v-spacer />
        <v-btn variant="text" @click="resumenDialogo = false">Cerrar</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
