package com.example.hospital.medicamentos.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.hospital.medicamentos.Medicamentos;
import com.example.hospital.medicamentos.MedicamentosRepository;
import com.example.hospital.medicamentos.MedicamentosService;
import com.example.hospital.medicamentos.dto.MedicamentosDTO;
import com.example.hospital.tratamientomedicamento.TratamientoMedicamentoRepository;
import com.example.hospital.shared.exception.DuplicadoException;
import com.example.hospital.shared.exception.RecursoNoEncontradoException;
import com.example.hospital.shared.exception.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MedicamentosServiceImpl implements MedicamentosService {

    private static final Logger log = LoggerFactory.getLogger(MedicamentosServiceImpl.class);

    private final MedicamentosRepository medicamentosRepository;
    private final TratamientoMedicamentoRepository tratamientoMedicamentoRepository;

    public MedicamentosServiceImpl(MedicamentosRepository medicamentosRepository,
                                  TratamientoMedicamentoRepository tratamientoMedicamentoRepository) {
        this.medicamentosRepository = medicamentosRepository;
        this.tratamientoMedicamentoRepository = tratamientoMedicamentoRepository;
    }

    private MedicamentosDTO mapToDTO(Medicamentos m) {
        return new MedicamentosDTO(
                m.getIdMedicamento(),
                m.getNombreComercial(),
                m.getNombreGenerico(),
                m.getPresentacion(),
                m.getLaboratorio(),
                m.getStock(),
                m.getStockMinimo(),
                m.getPrecio(),
                m.getRequiereReceta(),
                m.getActivo()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicamentosDTO> findAll() {
        return medicamentosRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicamentosDTO> findActivos() {
        return medicamentosRepository.findByActivoTrue()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MedicamentosDTO findById(Long id) {
        Medicamentos entity = medicamentosRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el medicamento con ID: " + id));
        return mapToDTO(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicamentosDTO> buscar(String criterio) {
        if (criterio == null || criterio.trim().isEmpty()) {
            return findActivos();
        }
        String query = criterio.trim();
        return medicamentosRepository.findByNombreComercialContainingIgnoreCaseOrNombreGenericoContainingIgnoreCase(query, query)
                .stream()
                .filter(m -> Boolean.TRUE.equals(m.getActivo()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicamentosDTO> buscar(String criterio, boolean bajoStock, boolean soloActivos) {
        String q = criterio == null ? "" : criterio.trim().toLowerCase();
        return medicamentosRepository.findAll().stream()
                .filter(m -> !soloActivos || Boolean.TRUE.equals(m.getActivo()))
                .filter(m -> !bajoStock || (m.getStock() != null && m.getStockMinimo() != null
                        && m.getStock() <= m.getStockMinimo()))
                .filter(m -> q.isEmpty()
                        || (m.getNombreComercial() != null && m.getNombreComercial().toLowerCase().contains(q))
                        || (m.getNombreGenerico() != null && m.getNombreGenerico().toLowerCase().contains(q))
                        || (m.getLaboratorio() != null && m.getLaboratorio().toLowerCase().contains(q)))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicamentosDTO> findBajoStock() {
        return medicamentosRepository.findByActivoTrue()
                .stream()
                .filter(m -> m.getStock() != null && m.getStockMinimo() != null && m.getStock() <= m.getStockMinimo())
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MedicamentosDTO crear(MedicamentosDTO dto) {
        validarDatosMedicamento(dto, null);

        Medicamentos medicamento = new Medicamentos();
        medicamento.setNombreComercial(dto.nombreComercial().trim());
        medicamento.setNombreGenerico(dto.nombreGenerico() != null ? dto.nombreGenerico().trim() : null);
        medicamento.setPresentacion(dto.presentacion() != null ? dto.presentacion().trim() : null);
        medicamento.setLaboratorio(dto.laboratorio() != null ? dto.laboratorio().trim() : null);
        medicamento.setStock(dto.stock() != null ? dto.stock() : 0);
        medicamento.setStockMinimo(dto.stockMinimo() != null ? dto.stockMinimo() : 0);
        medicamento.setPrecio(dto.precio());
        medicamento.setRequiereReceta(dto.requiereReceta() != null ? dto.requiereReceta() : false);
        medicamento.setActivo(dto.activo() != null ? dto.activo() : true);

        Medicamentos guardado = medicamentosRepository.save(medicamento);
        return mapToDTO(guardado);
    }

    @Override
    @Transactional
    public MedicamentosDTO actualizar(MedicamentosDTO dto, Long id) {
        if (id == null) {
            throw new ReglaNegocioException("El ID del medicamento es obligatorio para actualizar.");
        }

        Medicamentos existente = medicamentosRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el medicamento con ID: " + id));

        validarDatosMedicamento(dto, id);

        existente.setNombreComercial(dto.nombreComercial().trim());
        existente.setNombreGenerico(dto.nombreGenerico() != null ? dto.nombreGenerico().trim() : null);
        existente.setPresentacion(dto.presentacion() != null ? dto.presentacion().trim() : null);
        existente.setLaboratorio(dto.laboratorio() != null ? dto.laboratorio().trim() : null);
        // El stock NO se modifica aqui: solo cambia con PATCH /{id}/stock.
        existente.setStockMinimo(dto.stockMinimo() != null ? dto.stockMinimo() : 0);
        existente.setPrecio(dto.precio());
        if (dto.requiereReceta() != null) {
            existente.setRequiereReceta(dto.requiereReceta());
        }
        if (dto.activo() != null) {
            existente.setActivo(dto.activo());
        }

        Medicamentos actualizado = medicamentosRepository.save(existente);
        return mapToDTO(actualizado);
    }

    @Override
    @Transactional
    public MedicamentosDTO ajustarStock(Long id, Integer cambioStock) {
        if (id == null) {
            throw new ReglaNegocioException("El ID del medicamento es obligatorio.");
        }
        if (cambioStock == null || cambioStock == 0) {
            throw new ReglaNegocioException("El valor de ajuste de stock no puede ser nulo o cero.");
        }

        Medicamentos existente = medicamentosRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el medicamento con ID: " + id));

        if (!Boolean.TRUE.equals(existente.getActivo())) {
            throw new ReglaNegocioException("No se puede ajustar el stock de un medicamento inactivo.");
        }

        int stockActual = existente.getStock() != null ? existente.getStock() : 0;
        int nuevoStock = stockActual + cambioStock;

        if (nuevoStock < 0) {
            throw new ReglaNegocioException(String.format(
                    "Stock insuficiente. Stock actual: %d, cambio solicitado: %d (resultado: %d).",
                    stockActual, cambioStock, nuevoStock
            ));
        }

        existente.setStock(nuevoStock);
        Medicamentos actualizado = medicamentosRepository.save(existente);
        log.info("Stock del medicamento {} ajustado en {}: nuevo stock {}", id, cambioStock, actualizado.getStock());
        if (actualizado.getStockMinimo() != null && actualizado.getStock() <= actualizado.getStockMinimo()) {
            log.warn("Medicamento {} ({}) con stock bajo: {} (mínimo {})", id, actualizado.getNombreComercial(), actualizado.getStock(), actualizado.getStockMinimo());
        }
        return mapToDTO(actualizado);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (id == null) {
            throw new ReglaNegocioException("El ID del medicamento es obligatorio.");
        }

        Medicamentos existente = medicamentosRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el medicamento con ID: " + id));

        if (!Boolean.TRUE.equals(existente.getActivo())) {
            throw new ReglaNegocioException("El medicamento ya se encuentra inactivo.");
        }

        // RN-MED-05: Integridad referencial con tratamientos
        if (tratamientoMedicamentoRepository.existsByIdMedicamento(id)) {
            throw new DuplicadoException("No se puede desactivar el medicamento porque está asociado a uno o más tratamientos médicos.");
        }

        // RN-MED-06: Borrado lógico sistemático
        existente.setActivo(false);
        medicamentosRepository.save(existente);
        log.info("Medicamento {} desactivado", id);
    }

    private void validarDatosMedicamento(MedicamentosDTO dto, Long idActual) {
        if (dto == null) {
            throw new ReglaNegocioException("Los datos del medicamento son obligatorios.");
        }

        // RN-MED-01: nombre comercial unico (obligatoriedad, tamanos, precio y stock se validan en el DTO)
        String nombreComercial = dto.nombreComercial().trim();

        if (idActual == null) {
            if (medicamentosRepository.existsByNombreComercialIgnoreCase(nombreComercial)) {
                throw new DuplicadoException("Ya existe un medicamento registrado con el nombre comercial: " + nombreComercial);
            }
        } else {
            if (medicamentosRepository.existsByNombreComercialIgnoreCaseAndIdMedicamentoNot(nombreComercial, idActual)) {
                throw new DuplicadoException("Ya existe otro medicamento registrado con el nombre comercial: " + nombreComercial);
            }
        }
    }
}
