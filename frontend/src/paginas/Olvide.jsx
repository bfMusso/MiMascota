import { Link, useNavigate } from 'react-router-dom'
import Marco from '../componentes/Marco.jsx'
import Campo from '../componentes/Campo.jsx'
import Aviso from '../componentes/Aviso.jsx'
import { useFormulario } from '../componentes/useFormulario.js'
import { auth } from '../api.js'

export default function Olvide() {
  const navigate = useNavigate()
  const f = useFormulario({ email: '' })

  const enviar = async (e) => {
    e.preventDefault()
    const ok = await f.enviar(async () => {
      await auth.olvidePassword(f.valores.email)
      return true
    })
    if (ok) navigate(`/restablecer?email=${encodeURIComponent(f.valores.email)}`)
  }

  return (
    <Marco>
      <div className="tarjeta">
        <div className="encabezado">
          <h1>Recuperar contraseña</h1>
          <p className="suave">Escribí tu email y te mandamos un código para elegir una contraseña nueva.</p>
        </div>
        <Aviso tipo="error">{f.error?.message}</Aviso>
        <form className="formulario" onSubmit={enviar}>
          <Campo id="email" etiqueta="Email" type="email" autoComplete="email" required
            value={f.valores.email} onChange={f.cambiar} error={f.campos.email} />
          <button className="boton ancho" disabled={f.enviando}>
            {f.enviando ? 'Enviando…' : 'Mandarme el código'}
          </button>
        </form>
      </div>
      <p className="pie">
        <Link to="/ingresar">Volver a ingresar</Link>
      </p>
    </Marco>
  )
}
