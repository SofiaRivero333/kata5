package kata5;

public class Main {

    public static void main(String[] args) {
        if (args.length != 5) {
            System.err.println(
                    "Uso: Main id host puerto consul nombreServicio"
            );
            System.exit(1);
        }

        try {
            new Webservice(
                    args[0],
                    args[1],
                    Integer.parseInt(args[2]),
                    args[3],
                    args[4]
            ).start();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Registro interrumpido.");
            System.exit(1);

        } catch (Exception e) {
            System.err.println(
                    "No se pudo arrancar el servicio: " + e.getMessage()
            );
            System.exit(1);
        }
    }
}