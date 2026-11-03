import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { http } from '@/api/http'

function leerUsuario() {
  try {
    return JSON.parse(localStorage.getItem('usuario') ?? 'null')
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token'))
  const usuario = ref(leerUsuario())

  const estaAutenticado = computed(() => !!token.value)
  const rol = computed(() => usuario.value?.rol ?? null)
  const esAdmin = computed(() => rol.value === 'ADMIN')

  function guardar(data) {
    token.value = data.token
    usuario.value = { username: data.username, rol: data.rol }
    localStorage.setItem('token', data.token)
    localStorage.setItem('usuario', JSON.stringify(usuario.value))
  }

  async function login(username, password) {
    guardar(await http.post('/api/v1/auth/login', { username, password }))
  }

  async function registrar(datos) {
    guardar(await http.post('/api/v1/auth/register', datos))
  }

  function logout() {
    token.value = null
    usuario.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('usuario')
  }

  return { token, usuario, estaAutenticado, rol, esAdmin, login, registrar, logout }
})
