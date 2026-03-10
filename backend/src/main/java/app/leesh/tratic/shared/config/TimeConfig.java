package app.leesh.tratic.shared.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import app.leesh.tratic.shared.time.Sleeper;
import app.leesh.tratic.shared.time.ThreadSleeper;

@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public Sleeper sleeper() {
        return new ThreadSleeper();
    }
}
