package dev.noctilume.qixu.booking;

import dev.noctilume.qixu.common.Business;
import dev.noctilume.qixu.notifications.Notifications;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BookingTasks {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(BookingTasks.class);
    private final Business b; private final ShortReservations reservations; private final Notifications notifications; private final boolean enabled;
    public BookingTasks(Business b,ShortReservations reservations,Notifications notifications,@Value("${qixu.tasks-enabled:true}") boolean enabled) { this.b=b; this.reservations=reservations; this.notifications=notifications; this.enabled=enabled; }
    @Scheduled(fixedDelayString="${qixu.task-delay-ms:5000}",initialDelay=10000)
    public void run() {
        if(!enabled) return;
        try {
            var now=b.now();
            var ids=b.jdbc.queryForList("SELECT id FROM short_reservation WHERE (status='PENDING' AND check_in_deadline<=?) OR (status='CHECKED_IN' AND ends_at<=?) ORDER BY id LIMIT 100",now,now);
            for(var row:ids) reservations.expire(Business.number(row,"id"));
            notifications.dispatch();
        } catch(org.springframework.dao.DataAccessException e) { LOG.warn("Booking task deferred: {}",e.getClass().getSimpleName()); }
    }
}
