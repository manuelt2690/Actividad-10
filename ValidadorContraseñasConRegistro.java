import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import java.util.stream.*;

public class ValidadorContraseñasConRegistro {

    private static final String ARCHIVO_REGISTRO = "registro_contraseñas.txt";
    private static final Path pathRegistro = Paths.get(ARCHIVO_REGISTRO);
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Validador de Contraseñas Concurrente con Registro");
        System.out.println("Ingrese el número de contraseñas a validar:");

        int numContraseñas = scanner.nextInt();
        scanner.nextLine(); // Consumir el salto de línea

        List<Future<ResultadoValidacion>> resultados = new ArrayList<>();

        for (int i = 0; i < numContraseñas; i++) {
            System.out.println("Ingrese la contraseña #" + (i + 1) + ":");
            String contraseña = scanner.nextLine();

            Future<ResultadoValidacion> futuro = executor.submit(() -> {
                ResultadoValidacion resultado = validarContraseña(contraseña);
                registrarResultado(resultado);
                return resultado;
            });

            resultados.add(futuro);
        }

        // Mostrar resultados
        System.out.println("\nResultados de validación:");
        resultados.forEach(f -> {
            try {
                ResultadoValidacion resultado = f.get();
                System.out.printf("Contraseña: %s - %s%n",
                        resultado.contraseña,
                        resultado.esValida ? "VÁLIDA" : "INVÁLIDA");
            } catch (InterruptedException | ExecutionException e) {
                System.out.println("Error al obtener resultado: " + e.getMessage());
            }
        });

        executor.shutdown();
        scanner.close();
    }

    static class ResultadoValidacion implements Serializable {
        String contraseña;
        boolean esValida;
        LocalDateTime fechaValidacion;
        Map<String, Boolean> criterios;

        public ResultadoValidacion(String contraseña, boolean esValida, Map<String, Boolean> criterios) {
            this.contraseña = contraseña;
            this.esValida = esValida;
            this.fechaValidacion = LocalDateTime.now();
            this.criterios = criterios;
        }

        @Override
        public String toString() {
            return String.format("[%s] Contraseña: %s - Válida: %b - Criterios: %s",
                    fechaValidacion, contraseña, esValida, criterios);
        }
    }

    private static ResultadoValidacion validarContraseña(String contraseña) {
        // Expresiones regulares para cada requisito
        Map<String, String> regexMap = Map.of(
                "Longitud", ".{8,}",
                "Especiales", ".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*",
                "Mayúsculas", "^(.*[A-Z].*[A-Z].*)",
                "Minúsculas", "^(.*[a-z].*[a-z].*[a-z].*)",
                "Números", ".*\\d.*"
        );

        // Validar cada criterio usando lambda
        Map<String, Boolean> criterios = regexMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Pattern.matches(e.getValue(), contraseña)
                ));

        boolean esValida = criterios.values().stream().allMatch(Boolean::booleanValue);

        return new ResultadoValidacion(contraseña, esValida, criterios);
    }

    private static void registrarResultado(ResultadoValidacion resultado) {
        try {
            // Usar Files.write con opción APPEND y CREATE
            Files.writeString(
                    pathRegistro,
                    resultado.toString() + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            System.err.println("Error al registrar resultado: " + e.getMessage());
        }
    }

    // Método adicional para mostrar el historial de validaciones
    public static void mostrarRegistro() {
        try {
            System.out.println("\nHistorial de validaciones:");
            Files.lines(pathRegistro).forEach(System.out::println);
        } catch (IOException e) {
            System.out.println("No se pudo leer el archivo de registro: " + e.getMessage());
        }
    }
}