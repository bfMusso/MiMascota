import { useState } from 'react'
import { ApiError } from '../api.js'

/**
 * Estado común de un formulario: valores, si está enviando y los errores del servidor.
 * `enviar(accion)` corre la acción y guarda el error general y los de cada campo.
 */
export function useFormulario(inicial) {
  const [valores, setValores] = useState(inicial)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState(null)
  const [campos, setCampos] = useState({})

  const cambiar = (e) => {
    const { name, value, type, checked } = e.target
    setValores((v) => ({ ...v, [name]: type === 'checkbox' ? checked : value }))
    setCampos((c) => ({ ...c, [name]: undefined }))
  }

  const enviar = async (accion) => {
    setEnviando(true)
    setError(null)
    setCampos({})
    try {
      return await accion()
    } catch (e) {
      if (e instanceof ApiError && Object.keys(e.campos).length) {
        setCampos(e.campos)
      } else {
        setError(e instanceof ApiError ? e : new ApiError(0))
      }
      return undefined
    } finally {
      setEnviando(false)
    }
  }

  return { valores, setValores, cambiar, enviando, error, setError, campos, enviar }
}
