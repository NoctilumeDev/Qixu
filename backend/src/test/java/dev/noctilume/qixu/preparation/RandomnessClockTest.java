package dev.noctilume.qixu.preparation;

import java.time.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RandomnessClockTest {
    @Test void futureRoundUsesCeilingAndExactBoundaryIsStable() {
        var genesis=LocalDateTime.ofEpochSecond(RandomnessSource.GENESIS,0,ZoneOffset.UTC);
        assertEquals(1,RandomnessSource.roundAtOrAfter(genesis));
        assertEquals(2,RandomnessSource.roundAtOrAfter(genesis.plusSeconds(1)));
        assertEquals(2,RandomnessSource.roundAtOrAfter(genesis.plusSeconds(3)));
        assertEquals(3,RandomnessSource.roundAtOrAfter(genesis.plusSeconds(4)));
        assertEquals(genesis.plusSeconds(3),RandomnessSource.roundTime(2));
        assertEquals(32721736,RandomnessSource.roundAtOrAfter(RandomnessSource.roundTime(32721736)));
        assertThrows(IllegalArgumentException.class,()->RandomnessSource.roundAtOrAfter(genesis.minusSeconds(1)));
        assertThrows(IllegalArgumentException.class,()->RandomnessSource.roundAtOrAfter(genesis.plusNanos(1)));
    }
}
