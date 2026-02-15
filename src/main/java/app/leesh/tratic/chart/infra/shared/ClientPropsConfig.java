package app.leesh.tratic.chart.infra.shared;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

import app.leesh.tratic.chart.infra.shared.ClientPropsConfig.BinanceProps;
import app.leesh.tratic.chart.infra.shared.ClientPropsConfig.UpbitProps;

@Configuration
@EnableConfigurationProperties({ UpbitProps.class, BinanceProps.class })
public class ClientPropsConfig {
    @ConfigurationProperties(prefix = "clients.upbit")
    public record UpbitProps(@NonNull String baseUrl, int maxCandleCountPerRequest,
            @NonNull Duration fastFailWaitThreshold) {
    }

    @ConfigurationProperties(prefix = "clients.binance")
    public record BinanceProps(@NonNull String baseUrl, int maxCandlesPerCall,
            @NonNull Duration fastFailWaitThreshold) {
    }
}
