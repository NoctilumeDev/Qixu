package dev.noctilume.qixu;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class QixuApplication {
    public static void main(String[] args) { SpringApplication.run(QixuApplication.class, args); }
    @Bean Clock clock() { return Clock.systemUTC(); }
}
