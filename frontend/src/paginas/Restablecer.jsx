import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import Marco from '../componentes/Marco.jsx'
import Campo from '../componentes/Campo.jsx'
import Aviso from '../componentes/Aviso.jsx'
import { useFormulario } from '../componentes/useFormulario.js'
import { auth } from '../api.js'

export default function Restablecer() {
  const [params] = useSearchParams()
  const [listo, setListo] = useState(false)
  const f = useFormulario({ email: params.get('email') || '', codigo: '', password: '' })

  const enviar = async (e) => {
    e.preventDefault()
    const ok = await f.enviar(async () => {
      await auth.resetPassword(f.valores.email, f.valores.codigo, f.valores.password)
      return true
    })
    if (ok) setListo(true)
  }

  const soloDigitos = (e) => {
    e.target.value = e.target.value.replace(/\D/g, '').slice(0, 6)
    f.cambiar(e)
  }

  if (listo) {
    return (
      <Marco>
        <div className="tarjeta">
          <h1>Contraseña cambiada</h1>
          <p className="suave">Ya podés entrar con tu contraseña nueva. Cerramos las sesiones abiertas en otros equipos.</p>
          <Link to="/ingresar" className="boton ancho">
            Ingresar
          </Link>
        </div>
      </Marco>
    )
  }

  return (
    <Marco>
      <div className="tarjeta">
        <div className="encabezado">
          <h1>Elegí una contraseña nueva</h1>
          <p className="suave">
            Si {params.get('email') ? <b>{params.get('email')}</b> : 'tu email'} tiene cuenta, te llegó un código de 6
            números. Vence en 10 minutos.
          </p>
        </div>
        <Aviso tipo="error">{f.error?.message}</Aviso>
        <form className="formulario" onSubmit={enviar}>
          {!params.get('email') && (
            <Campo id="email" etiqueta="Email" type="email" autoComplete="email" required
              value={f.valores.email} onChange={f.cambiar} error={f.campos.email} />
          )}
          <Campo id="codigo" etiqueta="Código" className="codigo" inputMode="numeric" autoComplete="one-time-code"
            required pattern="\d{6}" placeholder="······" value={f.valores.codigo} onChange={soloDigitos}
            error={f.campos.codigo} />
          <Campo id="password" etiqueta="Contraseña nueva" type="password" autoComplete="new-password" required
            minLength={8} maxLength={72} ayuda="Al menos 8 caracteres."
            value={f.valores.password} onChange={f.cambiar} error={f.campos.password} />
          <button className="boton ancho" disabled={f.enviando || f.valores.codigo.length !== 6}>
            {f.enviando ? 'Guardando…' : 'Cambiar contraseña'}
          </button>
        </form>
      </div>
      <p className="pie">
        <Link to="/olvide">Pedir otro código</Link>
      </p>
    </Marco>
  )
}
