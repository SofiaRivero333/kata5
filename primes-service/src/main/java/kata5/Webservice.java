package kata5;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public final class Webservice {
    private final String id;
    private final String host;
    private final int port;
    private final String consul;
    private final String serviceName;
    private final Gson gson = new Gson();

    public Webservice(String id, String host, int port,
                      String consul, String serviceName) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.consul = consul;
        this.serviceName = serviceName;
    }

    public void start() throws IOException, InterruptedException {
        Javalin app = Javalin.create(this::init)
                .start("0.0.0.0", port);

        try {
            register();
            System.out.println("Registrado: " + id + " en " + serviceName);
        } catch (IOException | InterruptedException | RuntimeException e) {
            app.stop();
            throw e;
        }
    }

    private void init(JavalinConfig config) {
        config.routes.get("/", this::primes);
        config.routes.get("/health", context -> context.result("OK"));
    }

    private void primes(Context context) {
        context.header("X-Node", id);
        context.header("X-Service", serviceName);

        int n;

        try {
            n = Integer.parseInt(context.queryParam("n"));
        } catch (NumberFormatException e) {
            context.status(400)
                    .result("n debe ser un entero entre 0 y 1000.");
            return;
        }

        if (n < 0 || n > 1000) {
            context.status(400)
                    .result("n debe ser un entero entre 0 y 1000.");
            return;
        }

        context.contentType("application/json")
                .result(gson.toJson(primes(n)));
    }

    private int[] primes(int n) {
        int[] values = new int[n];
        int count = 0;
        int candidate = 2;

        while (count < n) {
            if (isPrime(candidate) && isPrime(candidate + 2)) {
                values[count++] = candidate;
            }
            candidate++;
        }

        return values;
    }

    private boolean isPrime(int number) {
        for (int divisor = 2; divisor <= number / divisor; divisor++) {
            if (number % divisor == 0) {
                return false;
            }
        }

        return number >= 2;
    }

    private void register() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(consul)
                                .resolve("/v1/agent/service/register"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(registrationJson()))
                .build();

        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build()) {

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() / 100 != 2) {
                throw new IOException(
                        "Consul rechazó el registro: HTTP "
                                + response.statusCode()
                );
            }
        }
    }

    private String registrationJson() {
        return gson.toJson(Map.of(
                "ID", id,
                "Name", serviceName,
                "Address", host,
                "Port", port,
                "Check", Map.of(
                        "HTTP", "http://" + host + ":" + port + "/health",
                        "Interval", "3s",
                        "Timeout", "1s",
                        "DeregisterCriticalServiceAfter", "10m"
                )
        ));
    }
}