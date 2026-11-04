import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { useSesion } from './sesion.jsx'
import Inicio from './paginas/Inicio.jsx'
import Registro from './paginas/Registro.jsx'
import Verificar from './paginas/Verificar.jsx'
import Ingresar from './paginas/Ingresar.jsx'
import Olvide from './paginas/Olvide.jsx'
import Restablecer from './paginas/Restablecer.jsx'
import MiCuenta from './paginas/MiCuenta.jsx'
import MisMascotas from './paginas/MisMascotas.jsx'
import MascotaNueva from './paginas/MascotaNueva.jsx'
import MascotaFicha from './paginas/MascotaFicha.jsx'
import ActivarPlaca from './paginas/ActivarPlaca.jsx'
import Admin from './paginas/Admin.jsx'
import PlacaPublica from './paginas/PlacaPublica.jsx'
import Cargando from './componentes/Cargando.jsx'

/** Solo con sesión; si no, manda a ingresar y vuelve después. */
function ConSesion({ children }) {
  const { usuario, cargando } = useSesion()
  const location = useLocation()
  if (cargando) return <Cargando />
  if (!usuario) return <Navigate to="/ingresar" replace state={{ volverA: location.pathname + location.search }} />
  return children
}

/** Solo para administradores. */
function SoloAdmin({ children }) {
  const { usuario } = useSesion()
  if (usuario?.rol !== 'ADMIN') return <Navigate to="/app" replace />
  return children
}

/** Pantallas de cuenta: si ya entró, lo lleva a su cuenta. */
function SinSesion({ children }) {
  const { usuario, cargando } = useSesion()
  if (cargando) return <Cargando />
  if (usuario) return <Navigate to="/app" replace />
  return children
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Inicio />} />
      <Route path="/p/:codigo" element={<PlacaPublica />} />
      <Route path="/registro" element={<SinSesion><Registro /></SinSesion>} />
      <Route path="/verificar" element={<SinSesion><Verificar /></SinSesion>} />
      <Route path="/ingresar" element={<SinSesion><Ingresar /></SinSesion>} />
      <Route path="/olvide" element={<SinSesion><Olvide /></SinSesion>} />
      <Route path="/restablecer" element={<SinSesion><Restablecer /></SinSesion>} />
      <Route path="/app" element={<ConSesion><MisMascotas /></ConSesion>} />
      <Route path="/app/mascotas/nueva" element={<ConSesion><MascotaNueva /></ConSesion>} />
      <Route path="/app/mascotas/:id" element={<ConSesion><MascotaFicha /></ConSesion>} />
      <Route path="/app/activar" element={<ConSesion><ActivarPlaca /></ConSesion>} />
      <Route path="/app/cuenta" element={<ConSesion><MiCuenta /></ConSesion>} />
      <Route path="/app/admin" element={<ConSesion><SoloAdmin><Admin /></SoloAdmin></ConSesion>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
