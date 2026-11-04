import { useCallback, useEffect, useState } from 'react'

/** Carga datos al montar y permite recargar. Devuelve { datos, setDatos, error, cargando, recargar }. */
export function useCarga(cargar, deps = []) {
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(true)

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const recargar = useCallback(() => {
    setCargando(true)
    setError(null)
    return cargar()
      .then(setDatos)
      .catch(setError)
      .finally(() => setCargando(false))
  }, deps)

  useEffect(() => {
    recargar()
  }, [recargar])

  return { datos, setDatos, error, cargando, recargar }
}
