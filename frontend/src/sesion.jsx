import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { auth, onSesionPerdida, refrescar, setAccessToken } from './api.js'

const SesionContext = createContext(null)

/** Guarda el usuario logueado. Al abrir la página intenta recuperar la sesión con la cookie. */
export function SesionProvider({ children }) {
  const [usuario, setUsuario] = useState(null)
  const [cargando, setCargando] = useState(true)

  useEffect(() => {
    onSesionPerdida(() => setUsuario(null))
    refrescar()
      .then((s) => setUsuario(s.usuario))
      .catch(() => setUsuario(null))
      .finally(() => setCargando(false))
  }, [])

  const iniciar = useCallback((sesion) => {
    setAccessToken(sesion.accessToken)
    setUsuario(sesion.usuario)
  }, [])

  const salir = useCallback(async () => {
    try {
      await auth.logout()
    } finally {
      setAccessToken(null)
      setUsuario(null)
    }
  }, [])

  const valor = useMemo(() => ({ usuario, setUsuario, cargando, iniciar, salir }), [usuario, cargando, iniciar, salir])
  return <SesionContext.Provider value={valor}>{children}</SesionContext.Provider>
}

export function useSesion() {
  return useContext(SesionContext)
}
