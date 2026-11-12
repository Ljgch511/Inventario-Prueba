package com.globalr.inventory.notify;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Dispatches ticket status change notifications asynchronously using
 * Quarkus Reactive Mailer + SmallRye Mutiny without blocking Core API callers.
 */
@Path("/api/v1/notify")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class NotifyResource {

    private static final Logger LOG = Logger.getLogger(NotifyResource.class);

    @Inject
    ReactiveMailer reactiveMailer;

    @POST
    @Path("/ticket-event")
    public Response sendTicketEvent(NotifyDtos.TicketEventNotificationRequest req) {
        if (req == null || req.recipients() == null || req.recipients().isEmpty()) {
            return Response.accepted(new NotifyDtos.NotificationAckResponse(false, 0, req != null ? req.ticketCode() : null)).build();
        }

        List<String> validRecipients = req.recipients().stream()
                .filter(r -> r != null && r.contains("@"))
                .distinct()
                .toList();

        if (validRecipients.isEmpty()) {
            return Response.accepted(new NotifyDtos.NotificationAckResponse(false, 0, req.ticketCode())).build();
        }

        String subject = String.format("[GlobalR %s] Ticket %s — %s (%s)",
                safe(req.countryCode(), "VE"),
                safe(req.ticketCode(), "N/A"),
                safe(req.eventStatus(), "ACTUALIZADO"),
                safe(req.ticketType(), "SOLICITUD"));

        String body = """
                <html>
                <body style="font-family: Arial, sans-serif; color: #1e293b;">
                  <div style="border-left: 4px solid #0f4c81; padding-left: 12px;">
                    <h2 style="color: #0f4c81; margin-bottom: 4px;">Notificación de Ticket — GlobalR Inventario</h2>
                    <p style="margin-top: 0; color: #64748b;">País: <strong>%s</strong></p>
                  </div>
                  <table style="border-collapse: collapse; margin-top: 12px;">
                    <tr><td style="padding: 4px 12px 4px 0;"><strong>Ticket:</strong></td><td>%s</td></tr>
                    <tr><td style="padding: 4px 12px 4px 0;"><strong>Tipo:</strong></td><td>%s</td></tr>
                    <tr><td style="padding: 4px 12px 4px 0;"><strong>Estado:</strong></td><td>%s</td></tr>
                    <tr><td style="padding: 4px 12px 4px 0;"><strong>Activo:</strong></td><td>%s — %s</td></tr>
                    <tr><td style="padding: 4px 12px 4px 0;"><strong>Motivo / Nota:</strong></td><td>%s</td></tr>
                    <tr><td style="padding: 4px 12px 4px 0;"><strong>Responsable de acción:</strong></td><td>%s</td></tr>
                  </table>
                  <p style="font-size: 12px; color: #64748b; margin-top: 18px;">
                    Mensaje enviado de forma asíncrona por GlobalR-SAT-Notify (Quarkus Reactive Mailer + Mutiny).
                  </p>
                </body>
                </html>
                """.formatted(
                escapeHtml(safe(req.countryCode(), "N/A")),
                escapeHtml(safe(req.ticketCode(), "N/A")),
                escapeHtml(safe(req.ticketType(), "N/A")),
                escapeHtml(safe(req.eventStatus(), "N/A")),
                escapeHtml(safe(req.assetNumber(), "-")),
                escapeHtml(safe(req.assetSummary(), "-")),
                escapeHtml(safe(req.reason(), "-")),
                escapeHtml(safe(req.actorName(), "Sistema"))
        );

        Mail mail = Mail.withHtml(validRecipients.get(0), subject, body);
        if (validRecipients.size() > 1) {
            mail.setTo(validRecipients);
        }

        reactiveMailer.send(mail).subscribe().with(
                ok -> LOG.infof("Email sent for ticket %s to %d recipients", req.ticketCode(), validRecipients.size()),
                err -> LOG.errorf(err, "Async email failed for ticket %s", req.ticketCode())
        );

        return Response.accepted(new NotifyDtos.NotificationAckResponse(true, validRecipients.size(), req.ticketCode())).build();
    }

    private String safe(String val, String fallback) {
        return (val == null || val.isBlank()) ? fallback : val;
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
