/**
 * Campo de formulario con etiqueta, ayuda y error debajo.
 * `como` elige el control: "input" (por defecto), "select" (con <option> como children) o "textarea".
 */
export default function Campo({ id, etiqueta, ayuda, error, className, como = 'input', children, ...control }) {
  const Control = como
  return (
    <div className={error ? 'campo con-error' : 'campo'}>
      <label htmlFor={id}>{etiqueta}</label>
      <Control
        id={id}
        name={id}
        className={className}
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={error || ayuda ? `${id}-nota` : undefined}
        {...control}
      >
        {children}
      </Control>
      {error ? (
        <span id={`${id}-nota`} className="error">
          {error}
        </span>
      ) : (
        ayuda && (
          <span id={`${id}-nota`} className="ayuda">
            {ayuda}
          </span>
        )
      )}
    </div>
  )
}
