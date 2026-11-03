import { Link, useLocation, useNavigate } from 'react-router-dom'
import Marco from '../componentes/Marco.jsx'
import Campo from '../componentes/Campo.jsx'
import Aviso from '../componentes/Aviso.jsx'
import { useFormulario } from '../componentes/useFormulario.js'
import { auth } from '../api.js'
import { useSesion } from '../sesion.jsx'

export default function Ingresar() {
  const navigate = useNavigate()
  const location = useLocation()
  const { iniciar } = useSesion()
  const f = useFormulario({ email: '', password: '' })

  const enviar = async (e) => {
    e.preventDefault()
    const sesion = await f.enviar(() => auth.login(f.valores.email, f.valores.password))
    if (sesion) {
      iniciar(sesion)
      navigate(location.state?.volverA || '/app', { replace: true })
    }
  }

  const sinVerificar = f.error?.codigo === 'email_no_verificado'

  return (
    <Marco>
      <div className="tarjeta">
        <div className="encabezado">
          <h1>Ingresar</h1>
          <p className="suave">Entrá para ver tus mascotas y sus placas.</p>
        </div>
        <Aviso tipo="error">
          {sinVerificar ? (
            <>
              Todavía no confirmaste tu email.{' '}
              <Link to={`/verificar?email=${encodeURIComponent(f.valores.email)}`}>Confirmalo con el código</Link>.
            </>
          ) : (
            f.error?.message
          )}
        </Aviso>
        <form className="formulario" onSubmit={enviar}>
          <Campo id="email" etiqueta="Email" type="email" autoComplete="email" required
            value={f.valores.email} onChange={f.cambiar} error={f.campos.email} />
          <Campo id="password" etiqueta="Contraseña" type="password" autoComplete="current-password" required
            value={f.valores.password} onChange={f.cambiar} error={f.campos.password} />
          <button className="boton ancho" disabled={f.enviando}>
            {f.enviando ? 'Entrando…' : 'Ingresar'}
          </button>
        </form>
        <Link to="/olvide" className="pie">
          Olvidé mi contraseña
        </Link>
      </div>
      <p className="pie">
        ¿No tenés cuenta? <Link to="/registro">Creala acá</Link>
      </p>
    </Marco>
  )
}
