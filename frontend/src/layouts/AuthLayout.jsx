import { Outlet } from 'react-router-dom'
import imagenMedico from '../assets/now-ui/images/doctor.jpg'

function Marca({ className = '' }) {
  return (
    <div className={`login-marca ${className}`.trim()}>
      <svg viewBox="0 0 32 32" width="34" height="34" aria-hidden="true">
        <circle cx="16" cy="16" r="16" fill="currentColor" />
        <path className="login-marca-cruz" d="M13 7h6v6h6v6h-6v6h-6v-6H7v-6h6z" />
      </svg>
      <span>NovaMedic</span>
    </div>
  )
}

export default function AuthLayout() {
  return (
    <div className="login-fondo">
      <div className="login-tarjeta">
        <aside className="login-lateral">
          <div className="login-lateral-imagen">
            <img src={imagenMedico} alt="" />
          </div>
          <div className="login-lateral-info">
            <Marca className="login-marca-clara" />
            <div className="login-bienvenida">
              <p className="login-bienvenida-saludo">
                Bienvenido a <strong>NovaMedic</strong>
              </p>
              <p className="login-bienvenida-sistema">Sistema de gestión hospitalaria</p>
            </div>
          </div>
        </aside>

        <main className="login-panel">
          <div className="login-panel-contenido">
            <Marca />
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}
