import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const rolesOperativos = ['OPERADOR', 'ADMIN']
const rolesValidos = ['USUARIO', ...rolesOperativos]

function rutaInicio(auth) {
  return auth.rol === 'USUARIO' ? { name: 'mi-cuenta' } : { name: 'vehiculos' }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { guestOnly: true } },
    { path: '/registro', name: 'registro', component: () => import('../views/RegisterView.vue'), meta: { guestOnly: true } },
    {
      path: '/estaciones',
      name: 'estaciones',
      component: () => import('../views/EstacionesView.vue'),
      meta: { requiresAuth: true, roles: rolesOperativos },
    },
    {
      path: '/estaciones-disponibles',
      name: 'estaciones-disponibles',
      component: () => import('../views/EstacionesDisponiblesView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/vehiculos',
      name: 'vehiculos',
      component: () => import('../views/VehiculosView.vue'),
      meta: { requiresAuth: true, roles: rolesOperativos },
    },
    {
      path: '/billeteras',
      name: 'billeteras',
      component: () => import('../views/BilleterasView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/viajes',
      name: 'viajes',
      component: () => import('../views/ViajesView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/tarifas',
      name: 'tarifas',
      component: () => import('../views/TarifasView.vue'),
      meta: { requiresAuth: true, roles: rolesOperativos },
    },
    {
      path: '/traslados',
      name: 'traslados',
      component: () => import('../views/TrasladosView.vue'),
      meta: { requiresAuth: true, roles: rolesOperativos },
    },
    {
      path: '/usuarios',
      name: 'usuarios',
      component: () => import('../views/UsuariosView.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] },
    },
    {
      path: '/auditorias',
      name: 'auditorias',
      component: () => import('../views/AuditoriasView.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] },
    },
    {
      path: '/mi-cuenta',
      name: 'mi-cuenta',
      component: () => import('../views/MiCuentaView.vue'),
      meta: { requiresAuth: true },
    },
    { path: '/', redirect: () => (useAuthStore().rol === 'USUARIO' ? '/mi-cuenta' : '/vehiculos') },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

// Guard de navegación (RBAC en el front). La autorización real siempre la aplica el backend:
// esto solo evita mostrar pantallas a las que el usuario no tiene acceso.
router.beforeEach((to) => {
  const auth = useAuthStore()

  if (to.meta.requiresAuth && !auth.estaAutenticado) return { name: 'login' }
  if (auth.estaAutenticado && !rolesValidos.includes(auth.rol)) {
    auth.logout()
    return { name: 'login' }
  }
  if (to.meta.guestOnly && auth.estaAutenticado) return rutaInicio(auth)
  if (to.meta.roles && !to.meta.roles.includes(auth.rol)) return rutaInicio(auth)
})

export default router
