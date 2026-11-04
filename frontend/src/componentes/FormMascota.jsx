import Campo from './Campo.jsx'
import { ESPECIES, SEXOS, castradaTexto } from '../textos.js'

export const MASCOTA_VACIA = {
  nombre: '',
  especie: 'PERRO',
  raza: '',
  color: '',
  sexo: '',
  castrada: '',
  fechaNacimiento: '',
  infoMedica: '',
  observaciones: '',
}

/** Lo que manda el formulario a la API (vacíos como null). */
export function datosMascota(v) {
  return {
    nombre: v.nombre,
    especie: v.especie,
    raza: v.raza,
    color: v.color,
    sexo: v.sexo || null,
    castrada: v.castrada === '' || v.castrada == null ? null : v.castrada === 'true',
    fechaNacimiento: v.fechaNacimiento || null,
    infoMedica: v.infoMedica,
    observaciones: v.observaciones,
  }
}

const hoy = () => new Date().toISOString().slice(0, 10)

/** Campos de la ficha de una mascota. `completo` agrega fecha, sexo y notas. */
export default function FormMascota({ valores, cambiar, campos = {}, completo = true, prefijo = '' }) {
  const id = (n) => prefijo + n
  const props = (n) => ({ id: id(n), name: n, value: valores[n] ?? '', onChange: cambiar, error: campos[n] })
  return (
    <>
      <Campo etiqueta="Nombre" required maxLength={60} autoComplete="off" {...props('nombre')} />
      <Campo etiqueta="Especie" como="select" required {...props('especie')}>
        {Object.entries(ESPECIES).map(([v, t]) => (
          <option key={v} value={v}>
            {t}
          </option>
        ))}
      </Campo>
      <div className="dos-columnas">
        <Campo etiqueta="Raza" maxLength={60} placeholder="Mestizo" {...props('raza')} />
        <Campo etiqueta="Color" maxLength={60} placeholder="Marrón con blanco" {...props('color')} />
      </div>
      {completo && (
        <>
          <div className="dos-columnas">
            <Campo etiqueta="Sexo" como="select" {...props('sexo')}>
              <option value="">Sin indicar</option>
              {Object.entries(SEXOS).map(([v, t]) => (
                <option key={v} value={v}>
                  {t}
                </option>
              ))}
            </Campo>
            <Campo etiqueta={castradaTexto(valores.sexo, true)} como="select" {...props('castrada')}>
              <option value="">Sin indicar</option>
              <option value="true">Sí</option>
              <option value="false">No</option>
            </Campo>
          </div>
          <Campo etiqueta="Fecha de nacimiento" type="date" max={hoy()} ayuda="Aproximada está bien."
            {...props('fechaNacimiento')} />
          <Campo etiqueta="Información médica" como="textarea" rows={3} maxLength={1000}
            placeholder="Alergias, medicación, si está castrado…" {...props('infoMedica')} />
          <Campo etiqueta="Observaciones" como="textarea" rows={3} maxLength={1000}
            placeholder="Es miedoso, no le gustan los gatos…" {...props('observaciones')} />
        </>
      )}
    </>
  )
}
