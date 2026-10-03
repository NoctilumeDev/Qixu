package dev.noctilume.qixu.recovery;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RecoveryJournalTest {
    @TempDir Path root;
    private Path path(){return root.resolve("journal");}
    private RecoveryJournal fresh() throws Exception {
        var journal=new RecoveryJournal(path(),true);journal.initialize(UUID.randomUUID().toString(),"a".repeat(64));return journal;
    }
    @Test void committedAndRolledBackHistorySurvivesExactFileReopen() throws Exception {
        String a=UUID.randomUUID().toString(),b=UUID.randomUUID().toString(),generation;
        try(var j=fresh()){generation=j.generation();j.event("PREPARE",a);j.event("COMMIT",a);j.event("PREPARE",b);j.event("ROLLBACK",b);}
        try(var j=new RecoveryJournal(path(),false)){assertEquals(generation,j.generation());assertEquals("COMMIT",j.transactions().get(a));assertEquals("ROLLBACK",j.transactions().get(b));}
    }
    @Test void secondOwnerCannotAcquireSameIndependentJournal() throws Exception {
        try(var first=fresh()){assertThrows(IOException.class,()->new RecoveryJournal(path(),false));first.assertCurrent();}
        try(var second=new RecoveryJournal(path(),false)){second.assertCurrent();}
    }
    @Test void illegalTerminalsAndDuplicatePreparesCannotRewriteBytes() throws Exception {
        String id=UUID.randomUUID().toString();
        try(var j=fresh()) {
            byte[] original=Files.readAllBytes(path());assertThrows(IOException.class,()->j.event("COMMIT",id));assertArrayEquals(original,Files.readAllBytes(path()));
            j.event("PREPARE",id);byte[] prepared=Files.readAllBytes(path());assertThrows(IOException.class,()->j.event("PREPARE",id));assertArrayEquals(prepared,Files.readAllBytes(path()));
            j.event("ROLLBACK",id);byte[] rolled=Files.readAllBytes(path());assertThrows(IOException.class,()->j.event("COMMIT",id));assertArrayEquals(rolled,Files.readAllBytes(path()));
        }
    }
    @Test void truncatedOrAlteredHistoryIsNeverSilentlyRepaired() throws Exception {
        try(var j=fresh()){j.event("PREPARE",UUID.randomUUID().toString());}
        byte[] raw=Files.readAllBytes(path());
        Files.write(path(),java.util.Arrays.copyOf(raw,raw.length-1));assertThrows(IOException.class,()->new RecoveryJournal(path(),false));
        Files.write(path(),raw);raw[raw.length-3]=raw[raw.length-3]=='a'?(byte)'b':(byte)'a';Files.write(path(),raw);
        assertThrows(IOException.class,()->new RecoveryJournal(path(),false));assertArrayEquals(raw,Files.readAllBytes(path()));
    }
    @Test void externalAppendWhileOpenInvalidatesFurtherDurableAuthority() throws Exception {
        try(var j=fresh()) {
            Files.writeString(path(),"unexpected\n",StandardOpenOption.APPEND);
            byte[] modified=Files.readAllBytes(path());assertThrows(IOException.class,j::assertCurrent);
            assertThrows(IOException.class,()->j.event("PREPARE",UUID.randomUUID().toString()));assertArrayEquals(modified,Files.readAllBytes(path()));
        }
    }
    @Test void oversizedJournalIsRejectedWithoutTruncation() throws Exception {
        try(var channel=java.nio.channels.FileChannel.open(path(),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)) {
            channel.position(RecoveryJournal.MAX_BYTES);channel.write(java.nio.ByteBuffer.wrap(new byte[]{10}));
        }
        long original=Files.size(path());assertThrows(IOException.class,()->new RecoveryJournal(path(),false));assertEquals(original,Files.size(path()));
    }
}
