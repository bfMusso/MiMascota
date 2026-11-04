import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import Marco from '../componentes/Marco.jsx'
import Aviso from '../componentes/Aviso.jsx'
import Campo from '../componentes/Campo.jsx'
import Cargando from '../componentes/Cargando.jsx'
import FotoMascota from '../componentes/FotoMascota.jsx'
import Huella from '../componentes/Huella.jsx'
import { useCarga } from '../componentes/useCarga.js'
import { useFormulario } from '../componentes/useFormulario.js'
import { publico } from '../api.js'
import { ESPECIES, SEXOS, castradaTexto, codigoLegible, edadDesdeMeses } from '../textos.js'

/** Registra la visita una sola vez por pestaña, para no mandarle varios avisos al dueño si recargan. */
function useVisita(codigo, activa) {
  useEffect(() => {
    if (!activa) return
    const clave = `mm-visita-${codigo}`
    try {
      if (sessionStorage.getItem(clave)) return
      sessionStorage.setItem(clave, '1')
    } catch {
      // Sin almacenamiento igual registramos la visita.
    }
    publico.escanear(codigo).catch(() => {})
  }, [codigo, activa])
}

function SinActivar({ codigo }) {
  return (
    <div className="tarjeta vacio">
      <div className="icono-grande">
        <Huella />
      </div>
      <h1>Esta placa todavía no está activada</h1>
      <p className="suave">
        Si es tuya, activala con tu cuenta y cargá los datos de tu mascota. El código es{' '}
        <b className="codigo-placa">{codigoLegible(codigo)}</b>.
      </p>
      <Link to={`/app/activar?codigo=${codigo}`} className="boton ancho">
        Activar esta placa
      </Link>
    </div>
  )
}

function NoEncontrada() {
  return (
    <div className="tarjeta vacio">
      <div className="icono-grande">
        <Huella />
      </div>
      <h1>No encontramos esta placa</h1>
      <p className="suave">Revisá que el código esté bien escrito. Si escaneaste el QR y llegaste acá, la placa ya no está en uso.</p>
      <Link to="/" className="boton secundario ancho">
        Qué es MiMascota
      </Link>
    </div>
  )
}

function Ficha({ m }) {
  const detalle = [
    ESPECIES[m.especie],
    m.raza,
    m.sexo && SEXOS[m.sexo],
    m.castrada && castradaTexto(m.sexo),
    edadDesdeMeses(m.edadMeses),
  ].filter(Boolean)

  return (
    <>
      {m.perdida && (
        <div className="aviso-perdida" role="status">
          <b>Su familia está buscando a {m.nombre}.</b>
          <span>Si sabés dónde está, avisales con el formulario de abajo. Les llega al instante.</span>
        </div>
      )}
      <div className="tarjeta ficha-publica">
        <FotoMascota mascota={m} tamano={140} />
        <h1>{m.nombre}</h1>
        {detalle.length > 0 && <p className="suave">{detalle.join(' · ')}</p>}
        {m.color && (
          <p>
            <span className="suave">Color:</span> {m.color}
          </p>
        )}
      </div>
      {(m.infoMedica || m.observaciones) && (
        <div className="tarjeta">
          {m.infoMedica && (
            <div className="encabezado">
              <h2>Información médica</h2>
              <p>{m.infoMedica}</p>
            </div>
          )}
          {m.observaciones && (
            <div className="encabezado">
              <h2>Para tener en cuenta</h2>
              <p>{m.observaciones}</p>
            </div>
          )}
        </div>
      )}
      {m.contacto && (
        <div className="tarjeta">
          <div className="encabezado">
            <h2>Llamá a {m.contacto.nombre}</h2>
            <p className="suave">Es quien cuida a {m.nombre}.</p>
          </div>
          <p className="telefono" aria-label="Teléfono">
            {m.contacto.telefono}
          </p>
          <a className="boton ancho" href={`tel:${m.contacto.telefono.replace(/[^\d+]/g, '')}`}>
            Llamar
          </a>
        </div>
      )}
    </>
  )
}

function Mensaje({ codigo, nombre }) {
  const f = useFormulario({ mensaje: '', contacto: '' })
  const [ubicacion, setUbicacion] = useState({ estado: 'no', coords: null })
  const [enviado, setEnviado] = useState(false)

  const pedirUbicacion = () => {
    if (!navigator.geolocation) {
      setUbicacion({ estado: 'error', coords: null })
      return
    }
    setUbicacion({ estado: 'buscando', coords: null })
    navigator.geolocation.getCurrentPosition(
      (p) => setUbicacion({ estado: 'ok', coords: { lat: p.coords.latitude, lng: p.coords.longitude } }),
      () => setUbicacion({ estado: 'error', coords: null }),
      { enableHighAccuracy: true, timeout: 15000 },
    )
  }

  const enviar = async (e) => {
    e.preventDefault()
    const ok = await f.enviar(async () => {
      await publico.escanear(codigo, {
        mensaje: f.valores.mensaje,
        contacto: f.valores.contacto,
        lat: ubicacion.coords?.lat ?? null,
        lng: ubicacion.coords?.lng ?? null,
      })
      return true
    })
    if (ok) setEnviado(true)
  }

  if (enviado) {
    return (
      <div className="tarjeta vacio">
        <h2>¡Gracias!</h2>
        <p className="suave">Le avisamos a su familia. {f.valores.contacto ? 'Te van a contactar pronto.' : ''}</p>
      </div>
    )
  }

  const quien = nombre || 'esta mascota'
  return (
    <form className="tarjeta" onSubmit={enviar}>
      <div className="encabezado">
        <h2>¿Encontraste a {quien}?</h2>
        <p className="suave">Dejale un mensaje a su familia. Les llega por email al instante.</p>
      </div>
      <Aviso tipo="error">{f.error?.message}</Aviso>
      <Campo id="mensaje" etiqueta="Mensaje" como="textarea" rows={3} maxLength={500}
        placeholder="Está conmigo en la plaza, está bien…" value={f.valores.mensaje} onChange={f.cambiar}
        error={f.campos.mensaje} ayuda={`${f.valores.mensaje.length}/500`} />
      <Campo id="contacto" etiqueta="Tu teléfono o email (opcional)" maxLength={100} autoComplete="tel"
        placeholder="099 123 456" value={f.valores.contacto} onChange={f.cambiar} error={f.campos.contacto}
        ayuda="Para que te puedan contactar. Solo lo ve su familia." />
      <div className="ubicacion">
        {ubicacion.estado === 'ok' ? (
          <span className="pastilla">Vamos a mandar tu ubicación</span>
        ) : (
          <button type="button" className="boton secundario chico" onClick={pedirUbicacion}
            disabled={ubicacion.estado === 'buscando'}>
            {ubicacion.estado === 'buscando' ? 'Buscando tu ubicación…' : 'Compartir mi ubicación'}
          </button>
        )}
        {ubicacion.estado === 'error' && (
          <span className="suave">No pudimos obtenerla. Podés escribir dónde estás en el mensaje.</span>
        )}
      </div>
      <button className="boton ancho" disabled={f.enviando || (!f.valores.mensaje.trim() && !f.valores.contacto.trim())}>
        {f.enviando ? 'Enviando…' : 'Avisar a su familia'}
      </button>
    </form>
  )
}

export default function PlacaPublica() {
  const { codigo } = useParams()
  const carga = useCarga(() => publico.ver(codigo), [codigo])
  const placa = carga.datos
  useVisita(placa?.codigo, placa?.estado === 'ACTIVA')

  let contenido
  if (carga.cargando && !placa) contenido = <Cargando />
  else if (carga.error?.status === 404) contenido = <NoEncontrada />
  else if (carga.error) contenido = <Aviso tipo="error">{carga.error.message}</Aviso>
  else if (placa.estado === 'SIN_ACTIVAR') contenido = <SinActivar codigo={placa.codigo} />
  else
    contenido = (
      <>
        {placa.mascota ? (
          <Ficha m={placa.mascota} />
        ) : (
          <div className="tarjeta vacio">
            <div className="icono-grande">
              <Huella />
            </div>
            <h1>Esta placa tiene dueño</h1>
            <p className="suave">Dejale un mensaje y le avisamos al instante.</p>
          </div>
        )}
        <Mensaje codigo={placa.codigo} nombre={placa.mascota?.nombre} />
        <p className="pie">
          Placa {codigoLegible(placa.codigo)} · <Link to="/">Qué es MiMascota</Link>
        </p>
      </>
    )

  return <Marco>{contenido}</Marco>
}
