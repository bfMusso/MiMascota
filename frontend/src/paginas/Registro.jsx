import { Link, useNavigate } from 'react-router-dom'
import Marco from '../componentes/Marco.jsx'
import Campo from '../componentes/Campo.jsx'
import Aviso from '../componentes/Aviso.jsx'
import { useFormulario } from '../componentes/useFormulario.js'
import { auth } from '../api.js'

export default function Registro() {
  const navigate = useNavigate()
  const f = useFormulario({ nombre: '', email: '', telefono: '', password: '' })

  const enviar = async (e) => {
    e.preventDefault()
    const { nombre, email, telefono, password } = f.valores
    const ok = await f.enviar(() => auth.registro({ nombre, email, telefono: telefono || null, password }))
    if (ok) navigate(`/verificar?email=${encodeURIComponent(ok.email)}`)
  }

  return (
    <Marco>
      <div className="tarjeta">
        <div className="encabezado">
          <h1>Crear cuenta</h1>
          <p className="suave">Te vamos a mandar un código por email para confirmar que es tuyo.</p>
        </div>
        <Aviso tipo="error">
          {f.error?.codigo === 'email_registrado' ? (
            <>
              Ese email ya tiene cuenta. <Link to="/ingresar">Ingresá</Link> o{' '}
              <Link to="/olvide">recuperá tu contraseña</Link>.
            </>
          ) : (
            f.error?.message
          )}
        </Aviso>
        <form className="formulario" onSubmit={enviar} noValidate={false}>
          <Campo id="nombre" etiqueta="Tu nombre" autoComplete="name" required maxLength={100}
            value={f.valores.nombre} onChange={f.cambiar} error={f.campos.nombre} />
          <Campo id="email" etiqueta="Email" type="email" autoComplete="email" required
            value={f.valores.email} onChange={f.cambiar} error={f.campos.email} />
          <Campo id="telefono" etiqueta="Teléfono (opcional)" type="tel" autoComplete="tel" maxLength={30}
            ayuda="Solo se muestra a quien encuentre tu mascota si vos lo activás."
            value={f.valores.telefono} onChange={f.cambiar} error={f.campos.telefono} />
          <Campo id="password" etiqueta="Contraseña" type="password" autoComplete="new-password" required
            minLength={8} maxLength={72} ayuda="Al menos 8 caracteres."
            value={f.valores.password} onChange={f.cambiar} error={f.campos.password} />
          <button className="boton ancho" disabled={f.enviando}>
            {f.enviando ? 'Creando…' : 'Crear cuenta'}
          </button>
        </form>
      </div>
      <p className="pie">
        ¿Ya tenés cuenta? <Link to="/ingresar">Ingresá</Link>
      </p>
    </Marco>
  )
}
