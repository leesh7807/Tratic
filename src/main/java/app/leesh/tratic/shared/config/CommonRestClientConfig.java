package app.leesh.tratic.shared.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.client.RestClient;

import app.leesh.tratic.shared.logging.OutboundLoggingInterceptor;

@Configuration
public class CommonRestClientConfig {

        @SuppressWarnings("null")
        @Bean
        public RestClient.Builder commonRestClientBuilder(
                        @NonNull OutboundLoggingInterceptor outboundLoggingInterceptor) {

                HttpClient httpClient = HttpClient.newBuilder()
                                .connectTimeout(Duration.ofSeconds(2))
                                .build();
                JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
                requestFactory.setReadTimeout(Duration.ofSeconds(5));

                return RestClient.builder()
                                .requestFactory(requestFactory)
                                .requestInterceptor(outboundLoggingInterceptor)
                                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        }
}
