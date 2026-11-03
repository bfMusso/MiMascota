/** Mensaje en un recuadro de color: tipo "error", "ok" o "info". */
export default function Aviso({ tipo = 'info', children }) {
  if (!children) return null
  return (
    <div className={`aviso ${tipo}`} role={tipo === 'error' ? 'alert' : 'status'}>
      {children}
    </div>
  )
}
