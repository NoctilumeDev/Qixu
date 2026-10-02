package dev.noctilume.qixu.governance;

import dev.noctilume.qixu.common.DomainException;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import javax.imageio.ImageIO;

/** Bounded untrusted raster evidence. It never opens a filesystem path or remote URL. */
public final class RasterEvidence {
    public record Info(String type,int width,int height) {}
    private RasterEvidence() {}
    public static Info inspect(byte[] bytes) {
        if(bytes==null || bytes.length==0 || bytes.length>1_048_576) throw DomainException.invalid("照片须不超过1MiB。");
        byte[] png={(byte)137,80,78,71,13,10,26,10};
        String expected=bytes.length>=8 && Arrays.equals(Arrays.copyOf(bytes,8),png)?"png":bytes.length>=3 && bytes[0]==(byte)255 && bytes[1]==(byte)216 && bytes[2]==(byte)255?"jpeg":null;
        if(expected==null) throw DomainException.invalid("仅接受PNG或JPEG照片，不接受脚本或矢量文件。");
        try(var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers=ImageIO.getImageReaders(stream);if(!readers.hasNext())throw DomainException.invalid("照片格式无法验证。");
            var reader=readers.next();
            try {
                reader.setInput(stream,true,true);String format=reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                if(!format.equals(expected) && !(expected.equals("jpeg") && format.equals("jpg")))throw DomainException.invalid("照片内容和格式不一致。");
                int w=reader.getWidth(0),h=reader.getHeight(0);
                if(w<1 || h<1 || w>8192 || h>8192 || (long)w*h>4_000_000)throw DomainException.invalid("照片最多400万像素，请先压缩。");
                if(reader.read(0)==null)throw DomainException.invalid("照片内容不完整。");
                return new Info("image/"+expected,w,h);
            } finally {reader.dispose();}
        } catch(DomainException e) {throw e;} catch(Exception e) {throw DomainException.invalid("照片损坏或无法读取。");}
    }
}
