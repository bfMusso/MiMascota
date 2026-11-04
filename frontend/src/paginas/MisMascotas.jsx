import { useState } from 'react'
import { Link } from 'react-router-dom'
import Marco from '../componentes/Marco.jsx'
import Aviso from '../componentes/Aviso.jsx'
import Cargando from '../componentes/Cargando.jsx'
import FotoMascota from '../componentes/FotoMascota.jsx'
import { useCarga } from '../componentes/useCarga.js'
import { mascotas, placas } from '../api.js'
import { useSesion } from '../sesion.jsx'
import { ESPECIES, codigoLegible } from '../textos.js'

function PlacasLibres({ libres, lista, alAsignar }) {
  const [elegida, setElegida] = useState({})
  const [error, setError] = useState(null)
  if (!libres.length) return null

  const asignar = async (codigo) => {
    setError(null)
    try {
      await placas.asignar(codigo, elegida[codigo] || lista[0]?.id)
      alAsignar()
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <div className="tarjeta">
      <div className="encabezado">
        <h2>Placas sin mascota</h2>
        <p className="suave">Ya son tuyas, pero nadie la va a encontrar con ellas hasta que las asignes.</p>
      </div>
      <Aviso tipo="error">{error}</Aviso>
      <div>
        {libres.map((p) => (
          <div className="placa-fila" key={p.codigo}>
            <span className="codigo-placa">{codigoLegible(p.codigo)}</span>
            {lista.length > 0 && (
              <div className="fila">
                <select aria-label={`Mascota para la placa ${p.codigo}`} className="boton secundario chico"
                  value={elegida[p.codigo] || lista[0].id}
                  onChange={(e) => setElegida({ ...elegida, [p.codigo]: e.target.value })}>
                  {lista.map((m) => (
                    <option key={m.id} value={m.id}>
                      {m.nombre}
                    </option>
                  ))}
                </select>
                <button type="button" className="boton chico" onClick={() => asignar(p.codigo)}>
                  Asignar
                </button>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}

export default function MisMascotas() {
  const { usuario } = useSesion()
  const carga = useCarga(() => Promise.all([mascotas.listar(), placas.mias()]))
  const [lista, mias] = carga.datos || [[], []]
  const libres = mias.filter((p) => !p.mascotaId)

  return (
    <Marco ancho>
      <div className="fila entre">
        <div className="encabezado">
          <h1>Hola, {usuario.nombre.split(' ')[0]}</h1>
          <p className="suave">Tus mascotas y sus placas.</p>
        </div>
        {lista.length > 0 && (
          <Link to="/app/mascotas/nueva" className="boton secundario chico">
            Agregar mascota
          </Link>
        )}
      </div>

      {carga.cargando && !carga.datos ? (
        <Cargando />
      ) : carga.error ? (
        <Aviso tipo="error">{carga.error.message}</Aviso>
      ) : lista.length === 0 ? (
        <div className="tarjeta vacio">
          <FotoMascota mascota={{ nombre: '' }} tamano={72} />
          <h2>Todavía no cargaste mascotas</h2>
          <p className="suave">
            Si ya tenés la placa, activala con el código que viene impreso y cargás a tu mascota en el mismo paso.
          </p>
          <Link to="/app/activar" className="boton ancho">
            Activar mi placa
          </Link>
          <Link to="/app/mascotas/nueva">Agregar una mascota sin placa</Link>
        </div>
      ) : (
        <div className="lista">
          {lista.map((m) => (
            <Link key={m.id} to={`/app/mascotas/${m.id}`} className="item-mascota">
              <FotoMascota mascota={m} tamano={60} />
              <div>
                <h3>{m.nombre}</h3>
                <span className="suave">{[ESPECIES[m.especie], m.raza].filter(Boolean).join(' · ')}</span>
                <div className="fila">
                  {m.perdida && <span className="pastilla alerta">Se perdió</span>}
                  {m.placas.length ? (
                    <span className="pastilla">{m.placas.length === 1 ? '1 placa' : `${m.placas.length} placas`}</span>
                  ) : (
                    <span className="pastilla neutra">Sin placa</span>
                  )}
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}

      <PlacasLibres libres={libres} lista={lista} alAsignar={carga.recargar} />
    </Marco>
  )
}
