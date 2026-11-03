import { Link } from 'react-router-dom'
import Marco from '../componentes/Marco.jsx'
import Huella from '../componentes/Huella.jsx'
import { useSesion } from '../sesion.jsx'

export default function Inicio() {
  const { usuario } = useSesion()
  return (
    <Marco>
      <section className="portada">
        <div className="icono">
          <Huella />
        </div>
        <h1>Si tu mascota se pierde, que te encuentren rápido</h1>
        <p className="suave">
          Una placa con NFC y código QR. Quien la encuentre la escanea con el celular, ve su ficha y te avisa al
          momento, con la ubicación.
        </p>
      </section>

      <div className="tarjeta">
        <h2>Cómo funciona</h2>
        <ol className="pasos">
          <li>
            <b>1</b>
            <span>Creá tu cuenta y activá la placa con el código que viene impreso.</span>
          </li>
          <li>
            <b>2</b>
            <span>Cargá la ficha de tu mascota y elegí qué datos se ven al escanearla.</span>
          </li>
          <li>
            <b>3</b>
            <span>Si alguien la escanea, te llega un aviso con su mensaje y dónde está.</span>
          </li>
        </ol>
      </div>

      {usuario ? (
        <Link to="/app" className="boton ancho">
          Ir a mi cuenta
        </Link>
      ) : (
        <div className="formulario">
          <Link to="/registro" className="boton ancho">
            Crear cuenta
          </Link>
          <Link to="/ingresar" className="boton secundario ancho">
            Ya tengo cuenta
          </Link>
        </div>
      )}
    </Marco>
  )
}
