import { useId } from 'react'
import { FormFeedback, FormGroup, Input, Label } from 'reactstrap'

/**
 * Campo de formulario con etiqueta y error debajo (FormGroup / Input / FormFeedback de reactstrap).
 * - tipo: 'text' | 'email' | 'date' | 'password' | 'select' | 'textarea' ...
 * - Para 'select' pasar `opciones: [{ valor, etiqueta }]` y `placeholder` (opción vacía).
 * - `error` es el mensaje del campo (cliente o `detalles` del backend).
 * - `adorno` es un elemento opcional superpuesto a la derecha del control (p. ej. un botón de icono).
 * - Dentro de un <Row>, ocupa media fila; con `className="col-12"` ocupa la fila completa.
 */
export default function FormField({
  nombre,
  etiqueta,
  tipo = 'text',
  valor,
  onChange,
  error,
  requerido = false,
  opciones = [],
  placeholder,
  ayuda,
  adorno,
  className = '',
  ...resto
}) {
  const id = useId()
  const comunes = {
    id,
    name: nombre,
    value: valor ?? '',
    onChange,
    'aria-invalid': error ? true : undefined,
    'aria-describedby': error ? `${id}-error` : undefined,
    ...resto,
  }
  const claseColumna = /(^|\s)col-/.test(className) ? '' : 'col-md-6'

  let control
  if (tipo === 'select') {
    // Se usa <select> nativo con form-control (Bootstrap 4); reactstrap 9 emitiría clases de Bootstrap 5.
    control = (
      <select {...comunes} className={`form-control${error ? ' is-invalid' : ''}`}>
        <option value="">{placeholder ?? 'Seleccione...'}</option>
        {opciones.map((o) => (
          <option key={o.valor} value={o.valor}>
            {o.etiqueta}
          </option>
        ))}
      </select>
    )
  } else if (tipo === 'textarea') {
    control = <Input type="textarea" rows={3} placeholder={placeholder} invalid={Boolean(error)} {...comunes} />
  } else {
    control = <Input type={tipo} placeholder={placeholder} invalid={Boolean(error)} {...comunes} />
  }

  return (
    <FormGroup className={`${claseColumna} ${className}`.trim()}>
      <Label for={id} className="font-weight-bold small mb-1">
        {etiqueta}
        {requerido && (
          <span className="text-danger ml-1" aria-hidden="true">
            *
          </span>
        )}
      </Label>
      {adorno ? (
        <div className="position-relative">
          {control}
          {adorno}
        </div>
      ) : (
        control
      )}
      {ayuda && !error && <small className="form-text text-muted">{ayuda}</small>}
      {error && (
        <FormFeedback id={`${id}-error`} className="d-block">
          {error}
        </FormFeedback>
      )}
    </FormGroup>
  )
}
