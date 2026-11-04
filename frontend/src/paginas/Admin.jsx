import { useState } from 'react'
import Marco from '../componentes/Marco.jsx'
import Aviso from '../componentes/Aviso.jsx'
import Campo from '../componentes/Campo.jsx'
import Cargando from '../componentes/Cargando.jsx'
import { useCarga } from '../componentes/useCarga.js'
import { useFormulario } from '../componentes/useFormulario.js'
import { admin } from '../api.js'
import { codigoLegible } from '../textos.js'

const fecha = (iso) => new Date(iso).toLocaleString('es-UY', { dateStyle: 'short', timeStyle: 'short' })

const ESTADOS = { SIN_ACTIVAR: 'Sin activar', ACTIVA: 'Activa', BLOQUEADA: 'Bloqueada' }
const FILTROS = [
  ['TODAS', 'Todas'],
  ['SIN_ACTIVAR', 'Sin activar'],
  ['ACTIVA', 'Activas'],
]

/** Baja un archivo que devuelve la API (con la sesión) y lo guarda con ese nombre. */
function guardar(blob, nombre) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = nombre
  a.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

function CodigosDelLote({ lote }) {
  const carga = useCarga(() => admin.placasDeLote(lote), [lote])
  const [filtro, setFiltro] = useState('TODAS')
  const [copiado, setCopiado] = useState(null)
  if (carga.cargando && !carga.datos) return <Cargando />
  if (carga.error) return <Aviso tipo="error">{carga.error.message}</Aviso>

  const lista = carga.datos.filter((p) => filtro === 'TODAS' || p.estado === filtro)
  const copiar = async (codigo) => {
    try {
      await navigator.clipboard.writeText(codigo)
      setCopiado(codigo)
      setTimeout(() => setCopiado(null), 1200)
    } catch {
      setCopiado(null)
    }
  }

  return (
    <div className="codigos-lote">
      <div className="filtros" role="group" aria-label="Filtrar por estado">
        {FILTROS.map(([valor, texto]) => (
          <button key={valor} type="button" className={filtro === valor ? 'filtro activo' : 'filtro'}
            aria-pressed={filtro === valor} onClick={() => setFiltro(valor)}>
            {texto}
            <span>{valor === 'TODAS' ? carga.datos.length : carga.datos.filter((p) => p.estado === valor).length}</span>
          </button>
        ))}
      </div>
      {lista.length === 0 ? (
        <p className="suave">No hay placas en este estado.</p>
      ) : (
        <div className="grilla-codigos">
          {lista.map((p) => (
            <button key={p.codigo} type="button" className={`ficha-codigo ${p.estado.toLowerCase()}`}
              title="Copiar código" onClick={() => copiar(p.codigo)}>
              <span className="codigo-placa">{codigoLegible(p.codigo)}</span>
              <span className="estado">{copiado === p.codigo ? 'Copiado' : ESTADOS[p.estado]}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

function Lote({ l, abierto, alternar, setError }) {
  const [bajando, setBajando] = useState(null)
  const bajar = async (tipo) => {
    setError(null)
    setBajando(tipo)
    try {
      const blob = tipo === 'pdf' ? await admin.exportarPdf(l.lote) : await admin.exportarLote(l.lote)
      guardar(blob, `${l.lote}.${tipo}`)
    } catch (e) {
      setError(e.message)
    } finally {
      setBajando(null)
    }
  }

  return (
    <div className="lote">
      <div className="lote-cabecera">
        <div>
          <h3>{l.lote}</h3>
          <span className="suave">{fecha(l.fechaAlta)}</span>
        </div>
        <div className="fila">
          <span className="pastilla neutra">{l.total} en total</span>
          <span className="pastilla neutra">{l.sinActivar} sin activar</span>
          <span className="pastilla">{l.activas === 1 ? "1 activa" : `${l.activas} activas`}</span>
        </div>
      </div>
      <div className="fila">
        <button type="button" className="boton secundario chico" aria-expanded={abierto} onClick={alternar}>
          {abierto ? 'Ocultar códigos' : 'Ver códigos'}
        </button>
        <button type="button" className="boton texto chico" disabled={!!bajando} onClick={() => bajar('pdf')}>
          {bajando === 'pdf' ? 'Generando…' : 'PDF con QR'}
        </button>
        <button type="button" className="boton texto chico" disabled={!!bajando} onClick={() => bajar('csv')}>
          CSV
        </button>
      </div>
      {abierto && <CodigosDelLote lote={l.lote} />}
    </div>
  )
}

export default function Admin() {
  const carga = useCarga(() => admin.lotes())
  const f = useFormulario({ cantidad: '20', nombre: '' })
  const [creado, setCreado] = useState(null)
  const [abierto, setAbierto] = useState(null)
  const [errorArchivo, setErrorArchivo] = useState(null)

  const crear = async (e) => {
    e.preventDefault()
    setCreado(null)
    const lote = await f.enviar(() => admin.crearLote(Number(f.valores.cantidad), f.valores.nombre.trim()))
    if (lote) {
      setCreado(lote)
      setAbierto(lote.lote)
      f.setValores({ cantidad: f.valores.cantidad, nombre: '' })
      carga.recargar()
    }
  }

  return (
    <Marco ancho>
      <div className="encabezado">
        <h1>Lotes de placas</h1>
        <p className="suave">
          Generá códigos para imprimir placas nuevas. Cada código queda sin activar hasta que un dueño lo cargue.
        </p>
      </div>

      <form className="tarjeta" onSubmit={crear}>
        <h2>Generar un lote</h2>
        <Aviso tipo="error">{f.error?.message}</Aviso>
        <div className="dos-columnas">
          <Campo id="cantidad" etiqueta="Cantidad" type="number" min={1} max={500} required
            ayuda="Hasta 500 por lote." value={f.valores.cantidad} onChange={f.cambiar} error={f.campos.cantidad} />
          <Campo id="nombre" etiqueta="Nombre del lote (opcional)" maxLength={40} pattern="[A-Za-z0-9_\-]+"
            placeholder="impresion-noviembre" ayuda="Si lo dejás vacío, se usa la fecha y hora."
            value={f.valores.nombre} onChange={f.cambiar} error={f.campos.nombre} />
        </div>
        <Aviso tipo="ok">{creado && `Creamos el lote ${creado.lote} con ${creado.cantidad} códigos. Lo ves abajo.`}</Aviso>
        <div className="fila">
          <button className="boton" disabled={f.enviando}>
            {f.enviando ? 'Generando…' : 'Generar códigos'}
          </button>
        </div>
      </form>

      <div className="tarjeta">
        <h2>Lotes</h2>
        <Aviso tipo="error">{errorArchivo || carga.error?.message}</Aviso>
        {carga.cargando && !carga.datos ? (
          <Cargando />
        ) : !carga.datos?.length ? (
          <p className="suave">Todavía no hay lotes.</p>
        ) : (
          <div className="lotes">
            {carga.datos.map((l) => (
              <Lote key={l.lote} l={l} abierto={abierto === l.lote} setError={setErrorArchivo}
                alternar={() => setAbierto(abierto === l.lote ? null : l.lote)} />
            ))}
          </div>
        )}
      </div>
    </Marco>
  )
}
