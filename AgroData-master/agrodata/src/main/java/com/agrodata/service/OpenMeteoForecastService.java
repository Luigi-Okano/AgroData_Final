package com.agrodata.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
public class OpenMeteoForecastService {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoForecastService.class);
    private static final Duration SUCCESS_CACHE_TTL = Duration.ofHours(1);
    private static final Duration FAILURE_CACHE_TTL = Duration.ofMinutes(2);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(4);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();
    private final JsonMapper jsonMapper;
    private final java.util.Map<Coordinates, CachedForecast> cache = new java.util.HashMap<>();

    public OpenMeteoForecastService(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public synchronized Optional<Double> probabilityOfRainNext24Hours(double latitude, double longitude) {
        Instant now = Instant.now();
        cache.entrySet().removeIf(entry ->
                !now.isBefore(entry.getValue().fetchedAt().plus(entry.getValue().ttl())));
        Coordinates coordinates = new Coordinates(latitude, longitude);
        CachedForecast cached = cache.get(coordinates);
        if (cached != null) {
            return cached.probability();
        }

        Optional<Double> probability = fetchProbability(coordinates);
        Duration ttl = probability.isPresent() ? SUCCESS_CACHE_TTL : FAILURE_CACHE_TTL;
        cache.put(coordinates, new CachedForecast(probability, Instant.now(), ttl));
        return probability;
    }

    private Optional<Double> fetchProbability(Coordinates coordinates) {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + coordinates.latitude()
                + "&longitude=" + coordinates.longitude()
                + "&hourly=precipitation_probability&forecast_days=2&timezone=auto";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", "AgroData/1.0")
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.warn("Open-Meteo respondeu HTTP {} ao consultar a previsão de chuva.", response.statusCode());
                return Optional.empty();
            }

            JsonNode root = jsonMapper.readTree(response.body());
            JsonNode times = root.path("hourly").path("time");
            JsonNode probabilities = root.path("hourly").path("precipitation_probability");
            if (!times.isArray() || !probabilities.isArray() || times.size() != probabilities.size()) {
                log.warn("Open-Meteo retornou uma previsão horária de chuva em formato inesperado.");
                return Optional.empty();
            }

            ZoneId zone = ZoneId.of(root.path("timezone").stringValue());
            LocalDateTime now = LocalDateTime.now(zone);
            LocalDateTime end = now.plusHours(24);
            Double maximum = null;
            for (int i = 0; i < times.size(); i++) {
                LocalDateTime forecastTime = LocalDateTime.parse(times.get(i).stringValue());
                JsonNode probability = probabilities.get(i);
                if (!forecastTime.isBefore(now) && forecastTime.isBefore(end) && probability.isNumber()) {
                    maximum = maximum == null ? probability.asDouble() : Math.max(maximum, probability.asDouble());
                }
            }
            if (maximum == null) {
                log.warn("Open-Meteo não retornou probabilidades de chuva para as próximas 24 horas.");
                return Optional.empty();
            }
            return Optional.of(maximum);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Consulta à previsão Open-Meteo interrompida.");
            return Optional.empty();
        } catch (IOException | RuntimeException e) {
            log.warn("Não foi possível consultar a previsão Open-Meteo: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private record Coordinates(double latitude, double longitude) {}
    private record CachedForecast(Optional<Double> probability, Instant fetchedAt, Duration ttl) {}
}
