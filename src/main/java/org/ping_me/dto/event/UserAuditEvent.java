package org.ping_me.dto.event;

import org.ping_me.model.enums.AuditAction;

/**
 * @author : user664dntp
 * @mailto : phatdang19052004@gmail.com
 * @created : 5/04/2026, Sunday
 **/
public record UserAuditEvent(
        Long userId,
        String email,
        String name,
        AuditAction action,
        long timestamp
) {}