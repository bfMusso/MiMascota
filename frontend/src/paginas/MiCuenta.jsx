import { useState } from 'react'
import Marco from '../componentes/Marco.jsx'
import Campo from '../componentes/Campo.jsx'
import Aviso from '../componentes/Aviso.jsx'
import { useFormulario } from '../componentes/useFormulario.js'
import { auth, me } from '../api.js'
import { useSesion } from '../sesion.jsx'

function Perfil() {
  const { usuario, setUsuario } = useSesion()
  const [guardado, setGuardado] = useState(false)
  const f = useFormulario({
    nombre: usuario.nombre || '',
    telefono: usuario.telefono || '',
    mostrarContacto: usuario.mostrarContacto,
  })

  const enviar = async (e) => {
    e.preventDefault()
    setGuardado(false)
    const u = await f.enviar(() =>
      me.actualizar({
        nombre: f.valores.nombre,
        telefono: f.valores.telefono,
        mostrarContacto: f.valores.mostrarContacto,
      }),
    )
    if (u) {
      setUsuario(u)
      setGuardado(true)
    }
  }

  return (
    <form className="tarjeta" onSubmit={enviar}>
      <div className="encabezado">
        <h2>Tus datos</h2>
      </div>
      <Aviso tipo="error">{f.error?.message}</Aviso>
      <Aviso tipo="ok">{guardado && 'Guardamos tus datos.'}</Aviso>
      <Campo id="nombre" etiqueta="Nombre" autoComplete="name" required maxLength={100}
        value={f.valores.nombre} onChange={f.cambiar} error={f.campos.nombre} />
      <Campo id="telefono" etiqueta="Teléfono" type="tel" autoComplete="tel" maxLength={30}
        value={f.valores.telefono} onChange={f.cambiar} error={f.campos.telefono} />
      <label className="check" htmlFor="mostrarContacto">
        <input id="mostrarContacto" name="mostrarContacto" type="checkbox"
          checked={f.valores.mostrarContacto} onChange={f.cambiar} />
        <span>
          Mostrar mi teléfono a quien encuentre a mi mascota
          <br />
          <span className="suave">Si no, solo te pueden dejar un mensaje desde la ficha.</span>
        </span>
      </label>
      <div className="fila">
        <button className="boton" disabled={f.enviando}>
          {f.enviando ? 'Guardando…' : 'Guardar'}
        </button>
      </div>
    </form>
  )
}

function CambiarPassword() {
  const { usuario, iniciar } = useSesion()
  const [listo, setListo] = useState(false)
  const f = useFormulario({ actual: '', nueva: '' })

  const enviar = async (e) => {
    e.preventDefault()
    setListo(false)
    const ok = await f.enviar(async () => {
      await me.cambiarPassword(f.valores.actual, f.valores.nueva)
      // Cambiar la contraseña cierra todas las sesiones; entramos de nuevo con la nueva.
      iniciar(await auth.login(usuario.email, f.valores.nueva))
      return true
    })
    if (ok) {
      f.setValores({ actual: '', nueva: '' })
      setListo(true)
    }
  }

  const error = f.error?.codigo === 'password_incorrecta' ? null : f.error?.message
  const errorActual = f.error?.codigo === 'password_incorrecta' ? 'No es tu contraseña actual.' : f.campos.actual

  return (
    <form className="tarjeta" onSubmit={enviar}>
      <div className="encabezado">
        <h2>Cambiar contraseña</h2>
        <p className="suave">Se cierran las sesiones abiertas en otros equipos.</p>
      </div>
      <Aviso tipo="error">{error}</Aviso>
      <Aviso tipo="ok">{listo && 'Cambiamos tu contraseña.'}</Aviso>
      <Campo id="actual" etiqueta="Contraseña actual" type="password" autoComplete="current-password" required
        value={f.valores.actual} onChange={f.cambiar} error={errorActual} />
      <Campo id="nueva" etiqueta="Contraseña nueva" type="password" autoComplete="new-password" required
        minLength={8} maxLength={72} ayuda="Al menos 8 caracteres."
        value={f.valores.nueva} onChange={f.cambiar} error={f.campos.nueva} />
      <div className="fila">
        <button className="boton secundario" disabled={f.enviando}>
          {f.enviando ? 'Cambiando…' : 'Cambiar contraseña'}
        </button>
      </div>
    </form>
  )
}

export default function MiCuenta() {
  const { usuario } = useSesion()
  return (
    <Marco ancho>
      <div className="encabezado">
        <h1>Mi cuenta</h1>
        <p className="suave">{usuario.email}</p>
      </div>
      <Perfil />
      <CambiarPassword />
    </Marco>
  )
}
