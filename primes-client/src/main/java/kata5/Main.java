package kata5;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class Main {
    private static final String BASE_URL = "http://localhost:8080";

    public static void main(String[] args) {
        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3)).build()) {
            while (true) {
                query(client, "/api/v1/primes/?n=8");
                query(client, "/api/v2/primes/?n=8");
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void query(HttpClient client, String path) throws InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString());
            String node = response.headers().firstValue("X-Node").orElse("sin nodo");
            System.out.println(path + " | HTTP " + response.statusCode()
                    + " | Node: " + node + " | Body: " + response.body());
        } catch (IOException e) {
            System.out.println(path + " | No se pudo consultar NGINX.");
        }
    }
}