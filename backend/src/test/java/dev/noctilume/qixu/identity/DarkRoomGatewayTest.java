package dev.noctilume.qixu.identity;

import static org.junit.jupiter.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import dev.noctilume.qixu.common.DomainException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class DarkRoomGatewayTest {
    static String path="/api/dark-room-library/v1/user/auth";
    @Test void unsafeUrisAndMissingKeyCannotEnableAdapter(){
        for(String u:List.of("http://example.com"+path,"https://x@host"+path,"https://host"+path+"?subject=1","https://host"+path+"#x","https://host/other","http://localhost"+path,"file:///tmp/identity"))assertThrows(IllegalStateException.class,()->new DarkRoomGateway(true,u,true));
        var gateway=new DarkRoomGateway(true,"http://127.0.0.1:1"+path,true);assertThrows(IllegalStateException.class,()->new ExternalTicketVault(gateway,""));assertThrows(IllegalStateException.class,()->new ExternalTicketVault(gateway,"broken"));
        var prod=new org.springframework.mock.env.MockEnvironment();prod.setActiveProfiles("prod");assertThrows(IllegalStateException.class,()->new DarkRoomGateway(true,"http://127.0.0.1:1"+path,true,prod));
    }
    @Test void vaultRejectsWrongAadKeyAndTamper(){
        var g=new DarkRoomGateway(false,"",false);var v=new ExternalTicketVault(g,Base64.getEncoder().encodeToString(new byte[32]));String cipher=v.seal("synthetic.payload.signature","actor-a");assertEquals("synthetic.payload.signature",v.open(cipher,"actor-a"));assertThrows(DomainException.class,()->v.open(cipher,"actor-b"));assertThrows(DomainException.class,()->v.open(cipher+"x","actor-a"));byte[] key=new byte[32];key[0]=1;var other=new ExternalTicketVault(g,Base64.getEncoder().encodeToString(key));assertThrows(DomainException.class,()->other.open(cipher,"actor-a"));assertNotEquals(cipher,v.seal("synthetic.payload.signature","actor-a"));
    }
    @Test void redirectOversizeSlowBodyAndInvalidUtf8AreBounded()throws Exception{
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);var executor=Executors.newCachedThreadPool();server.setExecutor(executor);var mode=new java.util.concurrent.atomic.AtomicInteger();
        server.createContext(path,x->{try{int m=mode.get();x.getResponseHeaders().set("Content-Type","application/json");if(m==0){x.getResponseHeaders().set("Location","http://127.0.0.1:1/leak");x.sendResponseHeaders(302,-1);}else if(m==1){byte[] huge=new byte[40000];x.sendResponseHeaders(200,huge.length);x.getResponseBody().write(huge);}else if(m==2){x.sendResponseHeaders(200,1000);x.getResponseBody().write('{');x.getResponseBody().flush();Thread.sleep(5000);}else{x.sendResponseHeaders(200,2);x.getResponseBody().write(new byte[]{(byte)0xc3,(byte)0x28});}}catch(Exception ignored){}finally{x.close();}});server.start();
        try{var gateway=new DarkRoomGateway(true,"http://127.0.0.1:"+server.getAddress().getPort()+path,true);for(int m=0;m<4;m++){mode.set(m);long start=System.nanoTime();var error=assertThrows(DomainException.class,()->gateway.verify("synthetic.payload.signature"));assertEquals(503,error.status());assertTrue(System.nanoTime()-start<TimeUnit.SECONDS.toNanos(4));}
            TransactionSynchronizationManager.setActualTransactionActive(true);try{assertThrows(IllegalStateException.class,()->gateway.verify("synthetic.payload.signature"));}finally{TransactionSynchronizationManager.setActualTransactionActive(false);}
        }finally{server.stop(0);executor.shutdownNow();}
    }
    @Test void fifthConcurrentVerificationIsRejectedWithoutQueueOrSourceRequest()throws Exception{
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);var worker=Executors.newCachedThreadPool();server.setExecutor(worker);var arrived=new CountDownLatch(4);var release=new CountDownLatch(1);var count=new java.util.concurrent.atomic.AtomicInteger();
        server.createContext(path,x->{try{count.incrementAndGet();arrived.countDown();release.await(3,TimeUnit.SECONDS);byte[] body="{\"code\":200,\"data\":{\"id\":1,\"accountStatus\":0,\"isLogin\":false}}".getBytes(StandardCharsets.UTF_8);x.getResponseHeaders().set("Content-Type","application/json");x.sendResponseHeaders(200,body.length);x.getResponseBody().write(body);}catch(Exception ignored){}finally{x.close();}});server.start();var pool=Executors.newFixedThreadPool(4);
        try{var g=new DarkRoomGateway(true,"http://127.0.0.1:"+server.getAddress().getPort()+path,true);var futures=new ArrayList<Future<String>>();for(int i=0;i<4;i++)futures.add(pool.submit(()->g.verify("synthetic.payload.signature")));assertTrue(arrived.await(1500,TimeUnit.MILLISECONDS));long before=System.nanoTime();assertEquals(503,assertThrows(DomainException.class,()->g.verify("synthetic.payload.signature")).status());assertTrue(System.nanoTime()-before<TimeUnit.MILLISECONDS.toNanos(300));assertEquals(4,count.get());release.countDown();for(var f:futures)assertEquals("1",f.get(3,TimeUnit.SECONDS));}
        finally{release.countDown();server.stop(0);pool.shutdownNow();worker.shutdownNow();}
    }
}
