package com.kidcare.usuario_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Servicio responsable del envío de correos electrónicos del sistema KidCare.
 * Utiliza Gmail SMTP a través de Spring Boot Mail (JavaMailSender).
 *
 * <p>Configuración requerida en application.properties:
 * <ul>
 *   <li>spring.mail.username — correo Gmail del remitente</li>
 *   <li>spring.mail.password — contraseña de aplicación de Google</li>
 * </ul>
 *
 * <p>En modo desarrollo ({@code mail.dev-mode=true}) no se envía correo real;
 * el token se imprime en la consola del servidor para facilitar las pruebas.
 */
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String remitente;

    /** Si es true, imprime el token en consola en lugar de enviar el correo. */
    @Value("${mail.dev-mode:true}")
    private boolean devMode;

    /**
     * Envía un correo de recuperación de contraseña al usuario.
     *
     * <p>En modo desarrollo ({@code mail.dev-mode=true}) el token se muestra en la
     * consola del servidor en lugar de enviarse por correo, lo que permite probar
     * el flujo sin configurar Gmail SMTP.
     *
     * @param destinatario correo electrónico del usuario que olvidó su contraseña
     * @param token        UUID de recuperación válido por 24 horas
     */
    public void enviarCorreoRecuperacion(String destinatario, String token) {
        if (devMode) {
            System.out.println("\n========================================");
            System.out.println("  [DEV] TOKEN DE RECUPERACIÓN");
            System.out.println("  Destinatario : " + destinatario);
            System.out.println("  Token        : " + token);
            System.out.println("  (copia este token en la app)");
            System.out.println("========================================\n");
            return;
        }

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject("KidCare — Recuperación de contraseña");
        mensaje.setText(
            "Hola,\n\n" +
            "Recibimos una solicitud para recuperar tu contraseña de KidCare.\n\n" +
            "Tu código de recuperación es:\n\n" +
            "    " + token + "\n\n" +
            "Ingresa este código en la app junto con tu nueva contraseña.\n" +
            "El código es válido por 24 horas.\n\n" +
            "Si no solicitaste este cambio, ignora este correo. " +
            "Tu contraseña actual permanece sin cambios.\n\n" +
            "Equipo KidCare\n" +
            "Bitácora de salud pediátrica"
        );
        mailSender.send(mensaje);
    }

    public void enviarInvitacionDelegado(String destinatario, String token, Integer idMenor) {
        if (devMode) {
            System.out.println("\n========================================");
            System.out.println("  [DEV] INVITACIÓN A DELEGADO");
            System.out.println("  Destinatario : " + destinatario);
            System.out.println("  Token        : " + token);
            System.out.println("  Menor ID     : " + idMenor);
            System.out.println("  (usa este token en POST /api/invitaciones/completar)");
            System.out.println("========================================\n");
            return;
        }

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject("KidCare — Has sido invitado como delegado");
        mensaje.setText(
            "Hola,\n\n" +
            "Has sido invitado a acceder al perfil de salud de un menor en KidCare.\n\n" +
            "Para completar tu registro, usa el siguiente token en la aplicación:\n\n" +
            "    " + token + "\n\n" +
            "Este enlace es válido por 48 horas.\n\n" +
            "Si no esperabas esta invitación, ignora este correo.\n\n" +
            "Equipo KidCare"
        );
        mailSender.send(mensaje);
    }
}
