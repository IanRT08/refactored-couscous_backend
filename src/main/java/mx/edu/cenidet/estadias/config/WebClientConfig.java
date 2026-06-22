package mx.edu.cenidet.estadias.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Value("${awshef.http-client.connect-timeout-seconds:10}")
    private int connectTimeoutSeconds;

    @Value("${awshef.http-client.read-timeout-seconds:15}")
    private int readTimeoutSeconds;

    //RestClient para Ambient Weather API
    //El scheduler de AmbientWeatherSyncService llama cada 60 s,
    @Bean("ambientWeatherRestClient")
    public RestClient ambientWeatherRestClient(
            @Value("${awshef.ambientweather.base-url:https://api.ambientweather.net/v1}")
            String baseUrl) {

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(buildRequestFactory())
                .defaultHeader("Accept", "application/json")
                .build();
    }

    //RestClient para ThingSpeak API
    //El scheduler llama cada 30s
    @Bean("thingSpeakRestClient")
    public RestClient thingSpeakRestClient(
            @Value("${awshef.thingspeak.base-url:https://api.thingspeak.com}")
            String baseUrl) {

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(buildRequestFactory())
                .defaultHeader("Accept", "application/json")
                .build();
    }

    //Factory con timeouts
    private JdkClientHttpRequestFactory buildRequestFactory() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));
        return factory;
    }

}
