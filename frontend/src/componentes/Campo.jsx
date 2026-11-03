/** Campo de formulario con etiqueta, ayuda y error debajo. */
export default function Campo({ id, etiqueta, ayuda, error, className, ...input }) {
  return (
    <div className={error ? 'campo con-error' : 'campo'}>
      <label htmlFor={id}>{etiqueta}</label>
      <input
        id={id}
        name={id}
        className={className}
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={error || ayuda ? `${id}-nota` : undefined}
        {...input}
      />
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
