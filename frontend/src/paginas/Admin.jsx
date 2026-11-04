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

export default function Admin() {
  const carga = useCarga(() => admin.lotes())
  const f = useFormulario({ cantidad: '20', nombre: '' })
  const [creado, setCreado] = useState(null)
  const [errorCsv, setErrorCsv] = useState(null)

  const crear = async (e) => {
    e.preventDefault()
    setCreado(null)
    const lote = await f.enviar(() => admin.crearLote(Number(f.valores.cantidad), f.valores.nombre.trim()))
    if (lote) {
      setCreado(lote)
      f.setValores({ cantidad: f.valores.cantidad, nombre: '' })
      carga.recargar()
    }
  }

  const descargar = async (lote) => {
    setErrorCsv(null)
    try {
      const blob = await admin.exportarLote(lote)
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${lote}.csv`
      a.click()
      setTimeout(() => URL.revokeObjectURL(url), 1000)
    } catch (e) {
      setErrorCsv(e.message)
    }
  }

  return (
    <Marco ancho>
      <div className="encabezado">
        <h1>Lotes de placas</h1>
        <p className="suave">Generá códigos para imprimir placas nuevas. Cada código queda sin activar hasta que un dueño lo cargue.</p>
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
        <div className="fila">
          <button className="boton" disabled={f.enviando}>
            {f.enviando ? 'Generando…' : 'Generar códigos'}
          </button>
        </div>
        {creado && (
          <>
            <Aviso tipo="ok">
              Creamos el lote {creado.lote} con {creado.cantidad} códigos.
            </Aviso>
            <div className="codigos">
              {creado.codigos.map((c) => (
                <span key={c}>{codigoLegible(c)}</span>
              ))}
            </div>
          </>
        )}
      </form>

      <div className="tarjeta">
        <h2>Lotes</h2>
        <Aviso tipo="error">{errorCsv || carga.error?.message}</Aviso>
        {carga.cargando && !carga.datos ? (
          <Cargando />
        ) : !carga.datos?.length ? (
          <p className="suave">Todavía no hay lotes.</p>
        ) : (
          <div className="tabla-envoltura">
            <table className="tabla">
              <thead>
                <tr>
                  <th>Lote</th>
                  <th>Creado</th>
                  <th className="num">Total</th>
                  <th className="num">Sin activar</th>
                  <th className="num">Activas</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {carga.datos.map((l) => (
                  <tr key={l.lote}>
                    <td>
                      <b>{l.lote}</b>
                    </td>
                    <td>{fecha(l.fechaAlta)}</td>
                    <td className="num">{l.total}</td>
                    <td className="num">{l.sinActivar}</td>
                    <td className="num">{l.activas}</td>
                    <td>
                      <button type="button" className="boton texto chico" onClick={() => descargar(l.lote)}>
                        Descargar CSV
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </Marco>
  )
}
