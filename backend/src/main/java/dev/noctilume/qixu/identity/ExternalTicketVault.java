package dev.noctilume.qixu.identity;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Separate deployment key; tickets must never be written or returned in plaintext. */
@Component
public class ExternalTicketVault {
    private final byte[] key;
    private final SecureRandom random=new SecureRandom();
    public ExternalTicketVault(DarkRoomGateway gateway,@Value("${qixu.external.ticket-key:}") String value){
        try{key=value.isEmpty()?null:Base64.getDecoder().decode(value);}catch(IllegalArgumentException e){throw new IllegalStateException("Invalid external ticket key");}
        if((key!=null && key.length!=32) || (gateway.enabled() && key==null))throw new IllegalStateException("External identity requires a separate 256-bit ticket key");
    }
    public String seal(String ticket,String aad){
        try{byte[] nonce=new byte[12];random.nextBytes(nonce);var cipher=cipher(Cipher.ENCRYPT_MODE,nonce,aad);
            return "gcm1."+Base64.getEncoder().encodeToString(nonce)+"."+Base64.getEncoder().encodeToString(cipher.doFinal(ticket.getBytes(StandardCharsets.UTF_8)));}
        catch(Exception e){throw DarkRoomGateway.unavailable();}
    }
    public String open(String value,String aad){
        try{if(value==null || value.length()>6000)throw new IllegalArgumentException();String[] parts=value.split("\\.",-1);
            if(parts.length!=3 || !parts[0].equals("gcm1"))throw new IllegalArgumentException();byte[] nonce=Base64.getDecoder().decode(parts[1]);if(nonce.length!=12)throw new IllegalArgumentException();
            return new String(cipher(Cipher.DECRYPT_MODE,nonce,aad).doFinal(Base64.getDecoder().decode(parts[2])),StandardCharsets.UTF_8);}
        catch(Exception e){throw DarkRoomGateway.unavailable();}
    }
    private Cipher cipher(int mode,byte[] nonce,String aad)throws Exception{if(key==null)throw new IllegalStateException();var c=Cipher.getInstance("AES/GCM/NoPadding");c.init(mode,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,nonce));c.updateAAD(aad.getBytes(StandardCharsets.UTF_8));return c;}
    public static String aad(String tokenHash,long user,long binding,long version,String issuer,String subject){return String.join("\n","qixu-external-ticket/1",tokenHash,Long.toString(user),Long.toString(binding),Long.toString(version),issuer,subject);}
}
