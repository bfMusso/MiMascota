import { Link } from 'react-router-dom'
import { useSesion } from '../sesion.jsx'
import Huella from './Huella.jsx'

/** Barra superior con el logo y el contenido centrado debajo. */
export default function Marco({ children, ancho = false }) {
  const { usuario, salir } = useSesion()
  return (
    <>
      <header className="barra">
        <Link to="/" className="logo">
          <Huella />
          MiMascota
        </Link>
        {usuario ? (
          <button type="button" className="boton texto" onClick={salir}>
            Salir
          </button>
        ) : (
          <Link to="/ingresar">Ingresar</Link>
        )}
      </header>
      <main className={ancho ? 'contenido ancho' : 'contenido'}>{children}</main>
    </>
  )
}
