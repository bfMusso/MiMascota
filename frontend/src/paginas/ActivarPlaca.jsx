import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import Marco from '../componentes/Marco.jsx'
import Aviso from '../componentes/Aviso.jsx'
import Campo from '../componentes/Campo.jsx'
import FotoMascota from '../componentes/FotoMascota.jsx'
import FormMascota, { MASCOTA_VACIA, datosMascota } from '../componentes/FormMascota.jsx'
import { useCarga } from '../componentes/useCarga.js'
import { useFormulario } from '../componentes/useFormulario.js'
import { mascotas, placas } from '../api.js'
import { ESPECIES, codigoLegible } from '../textos.js'

const NUEVA = 'nueva'

export default function ActivarPlaca() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const carga = useCarga(() => mascotas.listar())
  const lista = carga.datos || []
  const [codigo, setCodigo] = useState(params.get('codigo') || '')
  const [eleccion, setEleccion] = useState(params.get('mascota') || null)
  const f = useFormulario(MASCOTA_VACIA)

  // Si no eligió, por defecto: la mascota que vino en la URL, la única que tiene, o una nueva.
  const elegida = eleccion || (lista.length ? lista[0].id : NUEVA)

  const enviar = async (e) => {
    e.preventDefault()
    const datos = elegida === NUEVA ? { mascota: datosMascota(f.valores) } : { mascotaId: elegida }
    const placa = await f.enviar(() => placas.activar(codigo, datos))
    if (placa) {
      navigate(`/app/mascotas/${placa.mascotaId}`, {
        replace: true,
        state: { aviso: `Listo, la placa ${codigoLegible(placa.codigo)} ya es de ${placa.mascotaNombre}.` },
      })
    }
  }

  const camposMascota = Object.fromEntries(
    Object.entries(f.campos)
      .filter(([k]) => k.startsWith('mascota.'))
      .map(([k, v]) => [k.slice('mascota.'.length), v]),
  )

  const errorCodigo = {
    placa_no_encontrada: 'No encontramos una placa con ese código. Revisá que esté bien escrito.',
    placa_ya_activa: 'Esa placa ya está activada. Si es tuya, la ves en Mis mascotas.',
  }[f.error?.codigo]

  return (
    <Marco>
      <form className="tarjeta" onSubmit={enviar}>
        <div className="encabezado">
          <h1>Activar placa</h1>
          <p className="suave">El código está impreso atrás de la placa: son 8 letras y números.</p>
        </div>
        <Campo id="codigo" etiqueta="Código de la placa" className="placa" required autoComplete="off"
          autoCapitalize="characters" spellCheck={false} maxLength={12} placeholder="ABCD 2345"
          value={codigo} error={errorCodigo}
          onChange={(e) => {
            setCodigo(e.target.value.toUpperCase())
            f.setError(null)
          }} />

        <div className="campo">
          <label>¿Para qué mascota es?</label>
          <div className="opciones" role="radiogroup">
            {lista.map((m) => (
              <label key={m.id} className="opcion" htmlFor={`m-${m.id}`}>
                <input id={`m-${m.id}`} type="radio" name="mascota" checked={elegida === m.id}
                  onChange={() => setEleccion(m.id)} />
                <FotoMascota mascota={m} tamano={36} />
                <span>
                  <b>{m.nombre}</b> <span className="suave">{ESPECIES[m.especie]}</span>
                </span>
              </label>
            ))}
            <label className="opcion" htmlFor="m-nueva">
              <input id="m-nueva" type="radio" name="mascota" checked={elegida === NUEVA}
                onChange={() => setEleccion(NUEVA)} />
              <span>
                <b>Una mascota nueva</b>
              </span>
            </label>
          </div>
        </div>

        {elegida === NUEVA && (
          <FormMascota valores={f.valores} cambiar={f.cambiar} campos={camposMascota} completo={false}
            prefijo="nueva-" />
        )}

        <Aviso tipo="error">{!errorCodigo && f.error?.message}</Aviso>
        <button className="boton ancho" disabled={f.enviando || codigo.replace(/[^A-Za-z0-9]/g, '').length < 8}>
          {f.enviando ? 'Activando…' : 'Activar placa'}
        </button>
      </form>
      <p className="pie">
        <Link to="/app">Volver a mis mascotas</Link>
      </p>
    </Marco>
  )
}
