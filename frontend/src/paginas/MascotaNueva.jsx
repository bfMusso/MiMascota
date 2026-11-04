import { Link, useNavigate } from 'react-router-dom'
import Marco from '../componentes/Marco.jsx'
import Aviso from '../componentes/Aviso.jsx'
import FormMascota, { MASCOTA_VACIA, datosMascota } from '../componentes/FormMascota.jsx'
import { useFormulario } from '../componentes/useFormulario.js'
import { mascotas } from '../api.js'

export default function MascotaNueva() {
  const navigate = useNavigate()
  const f = useFormulario(MASCOTA_VACIA)

  const enviar = async (e) => {
    e.preventDefault()
    const m = await f.enviar(() => mascotas.crear(datosMascota(f.valores)))
    if (m) navigate(`/app/mascotas/${m.id}`, { replace: true })
  }

  return (
    <Marco>
      <form className="tarjeta" onSubmit={enviar}>
        <div className="encabezado">
          <h1>Agregar mascota</h1>
          <p className="suave">Después le podés subir una foto y asignarle una placa.</p>
        </div>
        <Aviso tipo="error">{f.error?.message}</Aviso>
        <FormMascota valores={f.valores} cambiar={f.cambiar} campos={f.campos} />
        <button className="boton ancho" disabled={f.enviando}>
          {f.enviando ? 'Guardando…' : 'Guardar'}
        </button>
      </form>
      <p className="pie">
        <Link to="/app">Volver a mis mascotas</Link>
      </p>
    </Marco>
  )
}
