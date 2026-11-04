import { useRef, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import Marco from '../componentes/Marco.jsx'
import Aviso from '../componentes/Aviso.jsx'
import Cargando from '../componentes/Cargando.jsx'
import FotoMascota from '../componentes/FotoMascota.jsx'
import FormMascota, { datosMascota } from '../componentes/FormMascota.jsx'
import { useCarga } from '../componentes/useCarga.js'
import { useFormulario } from '../componentes/useFormulario.js'
import { mascotas, placas } from '../api.js'
import { CAMPOS_PUBLICOS, ESPECIES, aRequest, castradaTexto, codigoLegible, edad, fechaHora, mapa } from '../textos.js'

function Cabecera({ m, setM }) {
  const input = useRef(null)
  const [estado, setEstado] = useState({ subiendo: false, error: null })

  const subir = async (e) => {
    const archivo = e.target.files[0]
    e.target.value = ''
    if (!archivo) return
    if (archivo.size > 5 * 1024 * 1024) {
      setEstado({ subiendo: false, error: 'La foto pesa más de 5 MB. Probá con una más liviana.' })
      return
    }
    setEstado({ subiendo: true, error: null })
    try {
      setM(await mascotas.subirFoto(m.id, archivo))
      setEstado({ subiendo: false, error: null })
    } catch (err) {
      setEstado({ subiendo: false, error: err.message })
    }
  }

  const quitar = async () => {
    try {
      setM(await mascotas.quitarFoto(m.id))
    } catch (err) {
      setEstado({ subiendo: false, error: err.message })
    }
  }

  const detalle = [ESPECIES[m.especie], m.raza, edad(m.fechaNacimiento), m.castrada && castradaTexto(m.sexo)]
    .filter(Boolean)
    .join(' · ')

  return (
    <div className="tarjeta">
      <div className="cabecera-mascota">
        <FotoMascota mascota={m} tamano={96} />
        <div>
          <h1>{m.nombre}</h1>
          <span className="suave">{detalle}</span>
          {m.perdida && (
            <div className="fila">
              <span className="pastilla alerta">Se perdió</span>
            </div>
          )}
        </div>
      </div>
      <Aviso tipo="error">{estado.error}</Aviso>
      <div className="fila">
        <input ref={input} type="file" accept="image/jpeg,image/png,image/webp" hidden onChange={subir}
          id="foto" />
        <button type="button" className="boton secundario chico" disabled={estado.subiendo}
          onClick={() => input.current.click()}>
          {estado.subiendo ? 'Subiendo…' : m.fotoUrl ? 'Cambiar foto' : 'Subir foto'}
        </button>
        {m.fotoUrl && (
          <button type="button" className="boton texto chico" onClick={quitar}>
            Quitar foto
          </button>
        )}
      </div>
    </div>
  )
}

function ModoPerdida({ m, setM }) {
  const [error, setError] = useState(null)
  const cambiar = async (e) => {
    setError(null)
    try {
      setM(await mascotas.actualizar(m.id, aRequest(m, { perdida: e.target.checked })))
    } catch (err) {
      setError(err.message)
    }
  }
  return (
    <div className={m.perdida ? 'tarjeta perdida' : 'tarjeta'}>
      <label className="interruptor" htmlFor="perdida">
        <span className="encabezado">
          <h2>{m.perdida ? `Estás buscando a ${m.nombre}` : '¿Se perdió?'}</h2>
          <span className="suave">
            {m.perdida
              ? 'Quien escanee la placa va a ver que la estás buscando, y te avisamos al instante.'
              : 'Activalo si no aparece. Lo desactivás cuando vuelva a casa.'}
          </span>
        </span>
        <input id="perdida" type="checkbox" checked={m.perdida} onChange={cambiar} />
      </label>
      <Aviso tipo="error">{error}</Aviso>
    </div>
  )
}

function Datos({ m, setM }) {
  const [guardado, setGuardado] = useState(false)
  const f = useFormulario({
    nombre: m.nombre,
    especie: m.especie,
    raza: m.raza || '',
    color: m.color || '',
    sexo: m.sexo || '',
    castrada: m.castrada == null ? '' : String(m.castrada),
    fechaNacimiento: m.fechaNacimiento || '',
    infoMedica: m.infoMedica || '',
    observaciones: m.observaciones || '',
  })

  const enviar = async (e) => {
    e.preventDefault()
    setGuardado(false)
    const nueva = await f.enviar(() => mascotas.actualizar(m.id, aRequest(m, datosMascota(f.valores))))
    if (nueva) {
      setM(nueva)
      setGuardado(true)
    }
  }

  return (
    <form className="tarjeta" onSubmit={enviar}>
      <h2>Datos</h2>
      <Aviso tipo="error">{f.error?.message}</Aviso>
      <FormMascota valores={f.valores} cambiar={f.cambiar} campos={f.campos} />
      <Aviso tipo="ok">{guardado && 'Guardamos los cambios.'}</Aviso>
      <div className="fila">
        <button className="boton" disabled={f.enviando}>
          {f.enviando ? 'Guardando…' : 'Guardar'}
        </button>
      </div>
    </form>
  )
}

function FichaPublica({ m, setM }) {
  const [error, setError] = useState(null)
  const cambiar = async (campo, visible) => {
    setError(null)
    const campos = visible ? [...m.camposPublicos, campo] : m.camposPublicos.filter((c) => c !== campo)
    try {
      setM(await mascotas.actualizar(m.id, aRequest(m, { camposPublicos: campos })))
    } catch (err) {
      setError(err.message)
    }
  }
  return (
    <div className="tarjeta">
      <div className="encabezado">
        <h2>Qué se ve al escanear la placa</h2>
        <p className="suave">El nombre y la especie se ven siempre. Tu email y tu dirección, nunca.</p>
      </div>
      <Aviso tipo="error">{error}</Aviso>
      <div className="checks">
        {CAMPOS_PUBLICOS.map(([campo, texto]) => (
          <label key={campo} className="check" htmlFor={`pub-${campo}`}>
            <input id={`pub-${campo}`} type="checkbox" checked={m.camposPublicos.includes(campo)}
              onChange={(e) => cambiar(campo, e.target.checked)} />
            <span>{texto}</span>
          </label>
        ))}
      </div>
      {m.camposPublicos.includes('CONTACTO') && (
        <p className="suave">
          Se muestra el teléfono que tenés en <Link to="/app/cuenta">Mi cuenta</Link>.
        </p>
      )}
    </div>
  )
}

function Placas({ m, recargar }) {
  const [error, setError] = useState(null)
  const quitar = async (codigo) => {
    setError(null)
    try {
      await placas.asignar(codigo, null)
      recargar()
    } catch (err) {
      setError(err.message)
    }
  }
  return (
    <div className="tarjeta">
      <div className="encabezado">
        <h2>Placas</h2>
        <p className="suave">
          {m.placas.length
            ? 'Si alguien escanea cualquiera de estas placas, ve la ficha de ' + m.nombre + '.'
            : `${m.nombre} todavía no tiene placa.`}
        </p>
      </div>
      <Aviso tipo="error">{error}</Aviso>
      {m.placas.length > 0 && (
        <div>
          {m.placas.map((codigo) => (
            <div className="placa-fila" key={codigo}>
              <span className="codigo-placa">{codigoLegible(codigo)}</span>
              <button type="button" className="boton texto chico" onClick={() => quitar(codigo)}>
                Quitar de {m.nombre}
              </button>
            </div>
          ))}
        </div>
      )}
      <div className="fila">
        <Link to={`/app/activar?mascota=${m.id}`} className="boton secundario chico">
          {m.placas.length ? 'Activar otra placa' : 'Activar una placa'}
        </Link>
      </div>
    </div>
  )
}

function Escaneos({ m }) {
  const [paginas, setPaginas] = useState(1)
  const carga = useCarga(() => mascotas.escaneos(m.id, 0, paginas * 10), [m.id, paginas])
  const datos = carga.datos

  return (
    <div className="tarjeta">
      <div className="encabezado">
        <h2>Escaneos</h2>
        <p className="suave">Cada vez que alguien abre la ficha de {m.nombre} o te deja un mensaje.</p>
      </div>
      <Aviso tipo="error">{carga.error?.message}</Aviso>
      {!datos ? (
        <Cargando />
      ) : datos.totalElements === 0 ? (
        <p className="suave">Todavía nadie escaneó sus placas.</p>
      ) : (
        <>
          <div>
            {datos.content.map((e) => (
              <div className="escaneo" key={e.id}>
                <div className="fila entre">
                  <b>{fechaHora(e.fecha)}</b>
                  <span className="suave">Placa {codigoLegible(e.placa)}</span>
                </div>
                {e.mensaje ? <p className="mensaje">“{e.mensaje}”</p> : <p className="suave">Abrieron la ficha.</p>}
                <div className="fila">
                  {e.contacto && (
                    <span>
                      <span className="suave">Contacto:</span> <b>{e.contacto}</b>
                    </span>
                  )}
                  {e.lat != null && (
                    <a href={mapa(e.lat, e.lng)} target="_blank" rel="noreferrer">
                      Ver en el mapa
                    </a>
                  )}
                </div>
              </div>
            ))}
          </div>
          {datos.content.length < datos.totalElements && (
            <div className="fila">
              <button type="button" className="boton texto chico" disabled={carga.cargando}
                onClick={() => setPaginas(paginas + 1)}>
                Ver más
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}

function Borrar({ m }) {
  const navigate = useNavigate()
  const [confirmando, setConfirmando] = useState(false)
  const [error, setError] = useState(null)
  if (m.rol !== 'TITULAR') return null

  const borrar = async () => {
    try {
      await mascotas.borrar(m.id)
      navigate('/app', { replace: true })
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div className="tarjeta">
      <div className="encabezado">
        <h2>Borrar a {m.nombre}</h2>
        <p className="suave">Se borra la ficha. Sus placas siguen siendo tuyas y podés asignarlas a otra mascota.</p>
      </div>
      <Aviso tipo="error">{error}</Aviso>
      {confirmando ? (
        <div className="fila">
          <button type="button" className="boton peligro lleno chico" onClick={borrar}>
            Sí, borrar a {m.nombre}
          </button>
          <button type="button" className="boton texto chico" onClick={() => setConfirmando(false)}>
            Cancelar
          </button>
        </div>
      ) : (
        <div className="fila">
          <button type="button" className="boton peligro chico" onClick={() => setConfirmando(true)}>
            Borrar mascota
          </button>
        </div>
      )}
    </div>
  )
}

export default function MascotaFicha() {
  const { id } = useParams()
  const location = useLocation()
  const carga = useCarga(() => mascotas.ver(id), [id])
  const m = carga.datos

  return (
    <Marco ancho>
      <p>
        <Link to="/app">← Mis mascotas</Link>
      </p>
      {carga.cargando && !m ? (
        <Cargando />
      ) : carga.error ? (
        <Aviso tipo="error">
          {carga.error.status === 404 ? 'No encontramos esa mascota.' : carga.error.message}
        </Aviso>
      ) : (
        <>
          <Aviso tipo="ok">{location.state?.aviso}</Aviso>
          <Cabecera m={m} setM={carga.setDatos} />
          <ModoPerdida m={m} setM={carga.setDatos} />
          <Escaneos m={m} />
          <Placas m={m} recargar={carga.recargar} />
          <FichaPublica m={m} setM={carga.setDatos} />
          <Datos key={m.id} m={m} setM={carga.setDatos} />
          <Borrar m={m} />
        </>
      )}
    </Marco>
  )
}
