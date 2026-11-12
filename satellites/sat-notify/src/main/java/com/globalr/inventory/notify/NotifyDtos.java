package com.globalr.inventory.notify;

import java.util.List;

public class NotifyDtos {

    public record TicketEventNotificationRequest(
            String ticketCode,
            String ticketType,
            String eventStatus,
            String countryCode,
            String assetNumber,
            String assetSummary,
            String reason,
            String actorName,
            List<String> recipients
    ) {}

    public record NotificationAckResponse(
            boolean queued,
            int recipientCount,
            String ticketCode
    ) {}
}
