import Huella from './Huella.jsx'

/** Foto redonda de la mascota, o una huella si no tiene. */
export default function FotoMascota({ mascota, tamano = 64 }) {
  return (
    <div className="foto-mascota" style={{ width: tamano, height: tamano }}>
      {mascota.fotoUrl ? <img src={mascota.fotoUrl} alt={`Foto de ${mascota.nombre}`} /> : <Huella />}
    </div>
  )
}
