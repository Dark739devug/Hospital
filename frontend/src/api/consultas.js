import client from './client.js'
import { RUTAS } from './rutas.js'

export const listarConsultas = () => client.get(RUTAS.consultas).then((r) => r.data)

/** GET /api/consultas?idCita= devuelve una lista (vacía si la cita no tiene consulta): se entrega la consulta o null. */
export const obtenerConsultaDeCita = (idCita) =>
  client.get(RUTAS.consultas, { params: { idCita } }).then((r) => r.data[0] ?? null)

export const obtenerConsulta = (id) => client.get(`${RUTAS.consultas}/${id}`).then((r) => r.data)
export const crearConsulta = (datos) => client.post(RUTAS.consultas, datos).then((r) => r.data)
export const actualizarConsulta = (id, datos) =>
  client.put(`${RUTAS.consultas}/${id}`, datos).then((r) => r.data)
/** 409 si tiene diagnósticos asociados. 422 al editar una consulta con más de 24 horas. */
export const desactivarConsulta = (id) => client.delete(`${RUTAS.consultas}/${id}`)

// Medicamentos recetados en la consulta: se asignan al tratamiento y se cobran en la factura de la consulta
/** GET -> { idFactura, estadoFactura, subtotal, total, medicamentos: [{ idDetalle, idTratamiento, idMedicamento, concepto, cantidad, precioUnitario, importe }] } */
export const listarMedicamentosConsulta = (idConsulta) =>
  client.get(`${RUTAS.consultas}/${idConsulta}/medicamentos`).then((r) => r.data)
export const agregarMedicamentoConsulta = (idConsulta, datos) =>
  client.post(`${RUTAS.consultas}/${idConsulta}/medicamentos`, datos).then((r) => r.data)
export const quitarMedicamentoConsulta = (idConsulta, idTratamiento, idMedicamento) =>
  client.delete(`${RUTAS.consultas}/${idConsulta}/medicamentos/${idTratamiento}/${idMedicamento}`).then((r) => r.data)

// Diagnósticos de una consulta (tabla de enlace)
export const listarDiagnosticosDeConsulta = (idConsulta) =>
  client.get(`${RUTAS.diagnosticoConsulta}/consulta/${idConsulta}`).then((r) => r.data)
export const listarConsultasDeDiagnostico = (idDiagnostico) =>
  client.get(`${RUTAS.diagnosticoConsulta}/diagnostico/${idDiagnostico}`).then((r) => r.data)
export const asociarDiagnostico = (idDiagnostico, idConsulta) =>
  client.post(RUTAS.diagnosticoConsulta, { idDiagnostico, idConsulta }).then((r) => r.data)
export const quitarDiagnostico = (idDiagnostico, idConsulta) =>
  client.delete(`${RUTAS.diagnosticoConsulta}/${idDiagnostico}/${idConsulta}`)
