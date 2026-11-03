const BASE_URL = (import.meta.env.VITE_API_URL ?? 'http://localhost:8080').replace(/\/$/, '')

let onUnauthorized = () => {}

const mensajesDominio = [
  [/saldo es negativo|saldo negativo/i, 'No puedes iniciar el viaje porque tu billetera tiene saldo negativo. Contacta a un operador para regularizarlo.'],
  [/ya tiene un viaje activo/i, 'Ya tienes un viaje en curso. Finalízalo o cancélalo antes de iniciar otro.'],
  [/bater[ií]a.*20%/i, 'El vehículo no cumple el mínimo de batería del 20%. Selecciona otro vehículo.'],
  [/no est[aá] disponible en una estaci[oó]n/i, 'El vehículo no está disponible para iniciar un viaje. Selecciona otro vehículo.'],
  [/no existe una tarifa vigente/i, 'No hay una tarifa vigente para este vehículo. Contacta a un operador.'],
  [/el cargo dejar[ií]a saldo negativo|saldo solicitado no es v[aá]lido/i, 'El saldo solicitado no es válido y no puede quedar negativo.'],
  [/billetera.*inactiva|billetera inactiva/i, 'La billetera está inactiva y no admite movimientos.'],
  [/saldo debe ser cero/i, 'La billetera debe tener saldo cero antes de desactivarla.'],
  [/viaje activo/i, 'No se puede completar la acción mientras exista un viaje activo asociado.'],
  [/capacidad.*inventario actual|capacidad no puede ser menor/i, 'La capacidad no puede ser inferior al inventario actual de la estación.'],
  [/alcanz[oó] su capacidad|capacidad f[ií]sica suficiente/i, 'La estación de destino no tiene capacidad suficiente.'],
  [/origen no tiene el exceso/i, 'La estación de origen no tiene suficientes vehículos sobre su objetivo para ese traslado.'],
  [/destino no tiene un d[eé]ficit/i, 'La estación de destino no tiene suficiente déficit para ese traslado.'],
  [/no fue posible reservar todos los veh[ií]culos/i, 'No hay suficientes vehículos disponibles para completar el traslado.'],
  [/faltan veh[ií]culos disponibles o capacidad/i, 'No se puede revertir el traslado: revisa la disponibilidad y capacidad de ambas estaciones.'],
  [/ya fue finalizado|ya finaliz[oó]/i, 'Este viaje ya fue finalizado.'],
  [/no existe o est[aá] inactiva/i, 'No se encontró el recurso solicitado o está inactivo.'],
  [/no se puede modificar un veh[ií]culo con un viaje activo/i, 'No se puede modificar este vehículo mientras tenga un viaje activo.'],
]

function mensajeDeError(status, body) {
  const message = typeof body?.message === 'string' ? body.message : ''
  for (const [pattern, friendly] of mensajesDominio) {
    if (pattern.test(message)) return friendly
  }

  const contieneDetalleTecnico = /org\.postgresql|sqlstate|psqlexception|preparedstatement|statementcallback| at org\.|error executing|syntax error|constraint \"/i.test(message)
  if (message && !contieneDetalleTecnico && message.length <= 240) return message

  if (status === 400) return 'No se pudo completar la operación. Revisa los datos ingresados y las reglas indicadas en el formulario.'
  if (status === 404) return 'No se encontró el elemento solicitado. Actualiza la lista e inténtalo nuevamente.'
  if (status === 409) return 'No se pudo completar la operación porque los datos actuales no cumplen las reglas del sistema.'
  if (status >= 500) return 'Ocurrió un error interno. Inténtalo nuevamente más tarde.'
  return `No se pudo completar la solicitud (error ${status}).`
}

export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn
}

async function request(path, options = {}) {
  const token = localStorage.getItem('token')
  const headers = { ...options.headers }
  if (options.body) headers['Content-Type'] = 'application/json'
  if (token) headers.Authorization = `Bearer ${token}`

  const response = await fetch(`${BASE_URL}${path}`, { ...options, headers })

  if (!response.ok) {
    if (response.status === 401 && token) onUnauthorized()

    let body = null
    try {
      body = await response.json()
    } catch {
      body = null
    }
    if (response.status === 403) {
      throw new Error('No tienes permisos para realizar esta acción.')
    }
    throw new Error(mensajeDeError(response.status, body))
  }

  // 204 No Content (por ejemplo, en DELETE)
  return response.status === 204 ? null : response.json()
}

export const http = {
  get: (path) => request(path),
  post: (path, body) => request(path, { method: 'POST', body: JSON.stringify(body) }),
  put: (path, body) => request(path, { method: 'PUT', body: JSON.stringify(body) }),
  patch: (path, body) => request(path, { method: 'PATCH', body: JSON.stringify(body) }),
  delete: (path, body) => request(path, {
    method: 'DELETE',
    ...(body ? { body: JSON.stringify(body) } : {}),
  }),
}
