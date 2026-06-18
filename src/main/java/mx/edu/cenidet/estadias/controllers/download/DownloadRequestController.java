package mx.edu.cenidet.estadias.controllers.download;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.edu.cenidet.estadias.dtos.comunes.ApiResponseDTO;
import mx.edu.cenidet.estadias.dtos.comunes.PageResponseDTO;
import mx.edu.cenidet.estadias.dtos.downloadrequest.CreateDownloadRequestDTO;
import mx.edu.cenidet.estadias.dtos.downloadrequest.DownloadRequestDetailDTO;
import mx.edu.cenidet.estadias.dtos.downloadrequest.DownloadRequestSummaryDTO;
import mx.edu.cenidet.estadias.dtos.downloadrequest.ResolveDownloadRequestDTO;
import mx.edu.cenidet.estadias.modelos.solicitudDescarga.EstadoSolicitud;
import mx.edu.cenidet.estadias.services.downloadrequest.DownloadRequestService;
import mx.edu.cenidet.estadias.util.AuthUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/solicitudes")
@RequiredArgsConstructor
public class DownloadRequestController {

    private final DownloadRequestService downloadRequestService;
    private final AuthUtils authUtils;

    //El usuario crea una solicitud de permiso.
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<DownloadRequestSummaryDTO>> crearSolicitud(
            @Valid @RequestBody CreateDownloadRequestDTO dto,
            HttpServletRequest request) {

        Long idUsuario = authUtils.getIdUsuarioActual(request);
        DownloadRequestSummaryDTO nueva = downloadRequestService.crearSolicitud(idUsuario, dto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.ok(
                        "Solicitud enviada. Un administrador la revisará pronto.", nueva));
    }

    //El usuario consulta el estado de sus solicitudes.
    @GetMapping("/mis-solicitudes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<List<DownloadRequestSummaryDTO>>> listarMisSolicitudes(
            HttpServletRequest request) {

        Long idUsuario = authUtils.getIdUsuarioActual(request);
        return ResponseEntity.ok(
                ApiResponseDTO.ok("Tus solicitudes obtenidas.",
                        downloadRequestService.listarPorUsuario(idUsuario)));
    }

    //El admin ve TODAS las solicitudes, paginadas y filtrables.
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<DownloadRequestDetailDTO>>> listarParaAdmin(
            @RequestParam(required = false) EstadoSolicitud estado,
            @PageableDefault(size = 20, sort = "fechaSolicitud",
                    direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(
                ApiResponseDTO.ok("Solicitudes obtenidas.",
                        downloadRequestService.listarParaAdmin(estado, pageable)));
    }

    //El admin aprueba o rechaza una solicitud.
    @PutMapping("/resolver")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public ResponseEntity<ApiResponseDTO<DownloadRequestDetailDTO>> resolverSolicitud(
            @Valid @RequestBody ResolveDownloadRequestDTO dto,
            HttpServletRequest request) {

        Long idAdmin = authUtils.getIdUsuarioActual(request);
        DownloadRequestDetailDTO resuelta = downloadRequestService.resolverSolicitud(idAdmin, dto);

        String mensaje = EstadoSolicitud.APROBADA.equals(dto.getDesicion())
                ? "Solicitud aprobada. El usuario fue notificado por correo."
                : "Solicitud rechazada. El usuario fue notificado por correo.";

        return ResponseEntity.ok(ApiResponseDTO.ok(mensaje, resuelta));
    }
}


