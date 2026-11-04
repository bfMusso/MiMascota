// Nombres que ve el usuario para los valores que maneja la API.

export const ESPECIES = { PERRO: 'Perro', GATO: 'Gato', OTRO: 'Otra' }

export const SEXOS = { MACHO: 'Macho', HEMBRA: 'Hembra' }

/** Datos que el dueño puede mostrar en la ficha pública, en el orden en que se ven. */
export const CAMPOS_PUBLICOS = [
  ['FOTO', 'Foto'],
  ['RAZA', 'Raza'],
  ['COLOR', 'Color'],
  ['SEXO', 'Sexo y si está castrado/a'],
  ['EDAD', 'Edad'],
  ['INFO_MEDICA', 'Información médica'],
  ['OBSERVACIONES', 'Observaciones'],
  ['CONTACTO', 'Tu teléfono'],
]

/** "Castrado", "Castrada" o "Castrado/a" según el sexo. Con `pregunta`, para la etiqueta del campo. */
export function castradaTexto(sexo, pregunta = false) {
  const palabra = sexo === 'MACHO' ? 'castrado' : sexo === 'HEMBRA' ? 'castrada' : 'castrado/a'
  return pregunta ? `¿Está ${palabra}?` : palabra[0].toUpperCase() + palabra.slice(1)
}

/** "3 años", "8 meses" o "" a partir de la fecha de nacimiento (AAAA-MM-DD). */
export function edad(fecha) {
  if (!fecha) return ''
  const [a, m, d] = fecha.split('-').map(Number)
  const hoy = new Date()
  let meses = (hoy.getFullYear() - a) * 12 + (hoy.getMonth() + 1 - m)
  if (hoy.getDate() < d) meses -= 1
  if (meses < 1) return 'Menos de un mes'
  if (meses < 12) return meses === 1 ? '1 mes' : `${meses} meses`
  const anios = Math.floor(meses / 12)
  return anios === 1 ? '1 año' : `${anios} años`
}

/** Muestra el código de placa en dos bloques para leerlo mejor: ABCD 2345. */
export function codigoLegible(codigo) {
  return codigo.length === 8 ? `${codigo.slice(0, 4)} ${codigo.slice(4)}` : codigo
}

/** Lo que espera la API al editar: la ficha completa. */
export function aRequest(m, cambios = {}) {
  const base = {
    nombre: m.nombre,
    especie: m.especie,
    raza: m.raza || '',
    sexo: m.sexo || null,
    castrada: m.castrada ?? null,
    fechaNacimiento: m.fechaNacimiento || null,
    color: m.color || '',
    infoMedica: m.infoMedica || '',
    observaciones: m.observaciones || '',
    perdida: m.perdida,
    camposPublicos: m.camposPublicos,
  }
  return { ...base, ...cambios }
}
