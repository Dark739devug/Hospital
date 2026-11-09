import { Suspense, useEffect } from 'react'
import { Link, Outlet, useLocation } from 'react-router-dom'
import { Button, Container, Navbar } from 'reactstrap'
import { useAuth } from '../auth/useAuth.js'
import Loading from '../components/Loading.jsx'
import { MODULOS, menuParaRol } from '../config/menu.js'
import { ETIQUETAS_ROL } from '../utils/formato.js'

// Color del degradado de la barra lateral de Now UI (blue | green | orange | red | yellow).
// "blue" combina con la banda azul oscuro del panel y transmite calma/confianza (adecuado para un hospital).
const COLOR_SIDEBAR = 'blue'

const IconoLogo = () => (
  <svg viewBox="0 0 24 24" width="26" height="26" fill="currentColor" aria-hidden="true">
    <path d="M10 3h4v7h7v4h-7v7h-4v-7H3v-4h7z" />
  </svg>
)

/** Nombre del módulo según la ruta actual (se muestra en la barra superior). */
function tituloDeRuta(pathname) {
  if (pathname === '/') return 'Inicio'
  const modulo = MODULOS.find((m) => pathname === m.ruta || pathname.startsWith(`${m.ruta}/`))
  return modulo ? modulo.titulo : 'Hospital'
}

export default function MainLayout() {
  const { usuario, rol, logout } = useAuth()
  const location = useLocation()
  const grupos = menuParaRol(rol)
  // La plantilla marca la opción activa con la clase `active` en el <li>
  const esActivo = (ruta) => (ruta === '/' ? location.pathname === '/' : location.pathname === ruta || location.pathname.startsWith(`${ruta}/`))

  // En tablet/móvil la barra lateral es un panel que se abre con el botón (clase nav-open de la plantilla).
  useEffect(() => {
    document.documentElement.classList.remove('nav-open')
    document.getElementById('toggle-sidebar')?.classList.remove('toggled')
    window.scrollTo(0, 0)
  }, [location.pathname])

  const alternarSidebar = () => {
    document.documentElement.classList.toggle('nav-open')
    document.getElementById('toggle-sidebar')?.classList.toggle('toggled')
  }

  return (
    <div className="wrapper">
      <div className="sidebar" data-color={COLOR_SIDEBAR}>
        <div className="logo">
          <Link to="/" className="simple-text logo-mini">
            <div className="logo-img text-white d-flex align-items-center justify-content-center">
              <IconoLogo />
            </div>
          </Link>
          <Link to="/" className="simple-text logo-normal">
            Novamedic
          </Link>
        </div>
        <div className="sidebar-wrapper" style={{ overflowY: 'auto' }}>
          <ul className="nav" aria-label="Menú principal">
            <li className={esActivo('/') ? 'active' : ''}>
              <Link to="/" className="nav-link">
                <i className="now-ui-icons design_app" />
                <p>Inicio</p>
              </Link>
            </li>
            {grupos.map((grupo) => [
              <li key={`t-${grupo.titulo}`} className="sidebar-modulos-titulo">
                {grupo.titulo}
              </li>,
              ...grupo.items.map((m) => (
                <li key={m.clave} className={esActivo(m.ruta) ? 'active' : ''}>
                  <Link to={m.ruta} className="nav-link">
                    <i className={`now-ui-icons ${m.icono ?? 'design_bullet-list-67'}`} />
                    <p>{m.titulo}</p>
                  </Link>
                </li>
              )),
            ])}
          </ul>
        </div>
      </div>

      <div className="main-panel">
        <Navbar expand="lg" className="navbar-absolute fixed-top navbar-transparent">
          <Container fluid>
            <div className="navbar-wrapper">
              <div className="navbar-toggle">
                <button type="button" id="toggle-sidebar" className="navbar-toggler" onClick={alternarSidebar} aria-label="Abrir o cerrar el menú">
                  <span className="navbar-toggler-bar bar1" />
                  <span className="navbar-toggler-bar bar2" />
                  <span className="navbar-toggler-bar bar3" />
                </button>
              </div>
              <span className="navbar-brand">{tituloDeRuta(location.pathname)}</span>
            </div>
            <div className="ml-auto d-flex align-items-center text-white">
              <div className="text-right mr-3 d-none d-sm-block" style={{ lineHeight: 1.2 }}>
                <div className="font-weight-bold">{usuario?.username}</div>
                <small>{ETIQUETAS_ROL[rol] ?? rol}</small>
              </div>
              <Button color="neutral" size="sm" className="btn-round" onClick={logout}>
                <i className="now-ui-icons media-1_button-power mr-1" />
                Cerrar sesión
              </Button>
            </div>
          </Container>
        </Navbar>

        <div className="panel-header panel-header-sm" />
        <div className="content">
          <Suspense fallback={<Loading texto="Cargando..." />}>
            <Outlet />
          </Suspense>
        </div>
      </div>
    </div>
  )
}
