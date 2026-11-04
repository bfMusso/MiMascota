import { Link, NavLink } from 'react-router-dom'
import { useSesion } from '../sesion.jsx'
import Huella from './Huella.jsx'

/** Barra superior con el logo, el menú si hay sesión y el contenido centrado debajo. */
export default function Marco({ children, ancho = false }) {
  const { usuario, salir } = useSesion()
  return (
    <>
      <header className="barra">
        <Link to={usuario ? '/app' : '/'} className="logo">
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
      {usuario && (
        <nav className="menu" aria-label="Secciones">
          <NavLink to="/app" end>
            Mis mascotas
          </NavLink>
          <NavLink to="/app/activar">Activar placa</NavLink>
          <NavLink to="/app/cuenta">Mi cuenta</NavLink>
          {usuario.rol === 'ADMIN' && <NavLink to="/app/admin">Admin</NavLink>}
        </nav>
      )}
      <main className={ancho ? 'contenido ancho' : 'contenido'}>{children}</main>
    </>
  )
}
