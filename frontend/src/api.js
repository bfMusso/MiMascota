// Cliente de la API. El access token vive solo en memoria; el refresh va en una
// cookie httpOnly que el navegador manda solo a /api/v1/auth.

let accessToken = null
let alCerrarSesion = () => {}

export function setAccessToken(token) {
  accessToken = token
}

export function onSesionPerdida(fn) {
  alCerrarSesion = fn
}

export class ApiError extends Error {
  constructor(status, cuerpo) {
    super(cuerpo?.detail || 'No pudimos conectar con el servidor. Probá de nuevo.')
    this.status = status
    this.codigo = cuerpo?.codigo
    // { campo: mensaje } para mostrar debajo de cada campo
    this.campos = Object.fromEntries((cuerpo?.errors || []).map((e) => [e.campo, e.mensaje]))
  }
}

async function pedir(metodo, ruta, cuerpo, { conToken = true } = {}) {
  const headers = {}
  if (cuerpo !== undefined) headers['Content-Type'] = 'application/json'
  if (conToken && accessToken) headers.Authorization = `Bearer ${accessToken}`

  let res
  try {
    res = await fetch(`/api/v1${ruta}`, {
      method: metodo,
      headers,
      body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
      credentials: 'same-origin',
    })
  } catch {
    throw new ApiError(0)
  }

  if (res.status === 204) return null
  const texto = await res.text()
  const datos = texto ? JSON.parse(texto) : null
  if (!res.ok) throw new ApiError(res.status, datos)
  return datos
}

let refrescando = null

/** Pide un access token nuevo con la cookie. Varias llamadas a la vez comparten el mismo pedido. */
export function refrescar() {
  refrescando ??= pedir('POST', '/auth/refresh', undefined, { conToken: false })
    .then((sesion) => {
      accessToken = sesion.accessToken
      return sesion
    })
    .finally(() => {
      refrescando = null
    })
  return refrescando
}

/** Llamada autenticada: si el token venció, refresca una vez y reintenta. */
async function conSesion(metodo, ruta, cuerpo) {
  try {
    return await pedir(metodo, ruta, cuerpo)
  } catch (e) {
    if (e.status !== 401) throw e
    try {
      await refrescar()
    } catch {
      accessToken = null
      alCerrarSesion()
      throw e
    }
    return pedir(metodo, ruta, cuerpo)
  }
}

export const auth = {
  registro: (datos) => pedir('POST', '/auth/registro', datos, { conToken: false }),
  verificarEmail: (email, codigo) => pedir('POST', '/auth/verificar-email', { email, codigo }, { conToken: false }),
  reenviarVerificacion: (email) => pedir('POST', '/auth/reenviar-verificacion', { email }, { conToken: false }),
  login: (email, password) => pedir('POST', '/auth/login', { email, password }, { conToken: false }),
  logout: () => pedir('POST', '/auth/logout', undefined, { conToken: false }),
  olvidePassword: (email) => pedir('POST', '/auth/olvide-password', { email }, { conToken: false }),
  resetPassword: (email, codigo, password) =>
    pedir('POST', '/auth/reset-password', { email, codigo, password }, { conToken: false }),
}

export const me = {
  ver: () => conSesion('GET', '/me'),
  actualizar: (datos) => conSesion('PATCH', '/me', datos),
  cambiarPassword: (actual, nueva) => conSesion('POST', '/me/cambiar-password', { actual, nueva }),
}
