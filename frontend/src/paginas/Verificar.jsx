import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import Marco from '../componentes/Marco.jsx'
import Campo from '../componentes/Campo.jsx'
import Aviso from '../componentes/Aviso.jsx'
import { useFormulario } from '../componentes/useFormulario.js'
import { ApiError, auth } from '../api.js'
import { useSesion } from '../sesion.jsx'

export default function Verificar() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const { iniciar } = useSesion()
  const f = useFormulario({ email: params.get('email') || '', codigo: '' })
  const [reenvio, setReenvio] = useState({ estado: null, texto: '' })

  const enviar = async (e) => {
    e.preventDefault()
    const sesion = await f.enviar(() => auth.verificarEmail(f.valores.email, f.valores.codigo))
    if (sesion) {
      iniciar(sesion)
      navigate('/app', { replace: true })
    }
  }

  const reenviar = async () => {
    if (!f.valores.email) {
      setReenvio({ estado: 'error', texto: 'Escribí tu email para mandarte un código nuevo.' })
      return
    }
    try {
      await auth.reenviarVerificacion(f.valores.email)
      setReenvio({ estado: 'ok', texto: 'Listo, te mandamos un código nuevo. El anterior ya no sirve.' })
    } catch (err) {
      setReenvio({ estado: 'error', texto: err instanceof ApiError ? err.message : 'No pudimos reenviarlo.' })
    }
  }

  const soloDigitos = (e) => {
    e.target.value = e.target.value.replace(/\D/g, '').slice(0, 6)
    f.cambiar(e)
  }

  return (
    <Marco>
      <div className="tarjeta">
        <div className="encabezado">
          <h1>Revisá tu email</h1>
          <p className="suave">
            {params.get('email') ? (
              <>Te mandamos un código de 6 números a <b>{params.get('email')}</b>. Vence en 10 minutos.</>
            ) : (
              'Escribí tu email y el código de 6 números que te mandamos.'
            )}
          </p>
        </div>
        <Aviso tipo="error">
          {f.error?.codigo === 'email_ya_verificado' ? (
            <>
              Tu email ya está confirmado. <Link to="/ingresar">Ingresá</Link>.
            </>
          ) : (
            f.error?.message
          )}
        </Aviso>
        <Aviso tipo={reenvio.estado}>{reenvio.texto}</Aviso>
        <form className="formulario" onSubmit={enviar}>
          {!params.get('email') && (
            <Campo id="email" etiqueta="Email" type="email" autoComplete="email" required
              value={f.valores.email} onChange={f.cambiar} error={f.campos.email} />
          )}
          <Campo id="codigo" etiqueta="Código" className="codigo" inputMode="numeric" autoComplete="one-time-code"
            required pattern="\d{6}" placeholder="······" value={f.valores.codigo} onChange={soloDigitos}
            error={f.campos.codigo} />
          <button className="boton ancho" disabled={f.enviando || f.valores.codigo.length !== 6}>
            {f.enviando ? 'Confirmando…' : 'Confirmar'}
          </button>
        </form>
        <div className="fila entre">
          <span className="suave">¿No te llegó? Mirá en spam.</span>
          <button type="button" className="boton texto" onClick={reenviar}>
            Mandar otro código
          </button>
        </div>
      </div>
    </Marco>
  )
}
