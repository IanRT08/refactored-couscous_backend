package mx.edu.cenidet.estadias.services.envioCorreos;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.edu.cenidet.estadias.modelos.solicitudDescarga.EstadoSolicitud;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${awshef.mail.remitente:no-reply@awshef.cenidet.mx}")
    private String remitente;

    //Recuperación de contraseña
    @Async
    public void enviarRecuperacionPassword(String correo, String nombre, Integer codigo) {
        String html = renderizar("recuperacion-password.html",
                "{{nombre}}", nombre,
                "{{codigo}}", String.valueOf(codigo));
        enviar(correo, "Recuperación de contraseña — AW-SHEF", html);
    }

    //Confirmación de cuenta
    @Async
    public void enviarConfirmacionCuenta(String correo, String nombre, Integer codigo) {
        String html = renderizar("confirmacion-cuenta.html",
                "{{nombre}}", nombre,
                "{{codigo}}", String.valueOf(codigo));
        enviar(correo, "Confirma tu cuenta — AW-SHEF", html);
    }

    //Código para desactivar cuenta
    @Async
    public void enviarDesactivacionCuenta(String correo, String nombre, Integer codigo) {
        String html = renderizar("desactivacion-cuenta.html",
                "{{nombre}}", nombre,
                "{{codigo}}", String.valueOf(codigo));
        enviar(correo, "Solicitud de desactivación de cuenta — AW-SHEF", html);
    }

    //Credenciales de nuevo administrador
    @Async
    public void enviarCredencialesAdmin(String correo, String nombre, String passwordTemporal) {
        String html = renderizar("credenciales-admin.html",
                "{{nombre}}", nombre,
                "{{password}}", passwordTemporal);
        enviar(correo, "Tus credenciales de administrador — AW-SHEF", html);
    }

    //Resultado de solicitud de descarga
    @Async
    public void enviarResultadoSolicitud(String correo, String nombre,
                                         EstadoSolicitud resultado, String comentario) {
        String plantilla = EstadoSolicitud.APROBADA.equals(resultado)
                ? "solicitud-aprobada.html" : "solicitud-rechazada.html";
        String html = renderizar(plantilla,
                "{{nombre}}", nombre,
                "{{comentario}}", comentario != null ? comentario : "Sin comentarios adicionales.");
        String asunto = EstadoSolicitud.APROBADA.equals(resultado)
                ? "¡Tu solicitud de descarga fue aprobada! — AW-SHEF"
                : "Tu solicitud de descarga fue rechazada — AW-SHEF";
        enviar(correo, asunto, html);
    }

    private String renderizar(String nombrePlantilla, String... reemplazos) {
        try {
            ClassPathResource recurso = new ClassPathResource("mail-templates/" + nombrePlantilla);
            String html = new String(
                    recurso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            for (int i = 0; i < reemplazos.length - 1; i += 2) {
                html = html.replace(reemplazos[i],
                        reemplazos[i + 1] != null ? reemplazos[i + 1] : "");
            }
            return html;
        } catch (IOException e) {
            log.error("No se pudo cargar la plantilla: {}", nombrePlantilla);
            return "<p>Notificación del sistema AW-SHEF.</p>";
        }
    }

    private void enviar(String destinatario, String asunto, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Correo enviado a [{}]: {}", destinatario, asunto);
        } catch (MessagingException e) {
            // No propagamos la excepción para no revertir la transacción principal
            log.error("Error al enviar correo a [{}]: {}", destinatario, e.getMessage());
        }
    }
}
