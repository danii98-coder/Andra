package pe.edu.unaj.andara.service;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unaj.andara.model.CambioPendiente;
import pe.edu.unaj.andara.repository.CambioPendienteRepository;
import pe.edu.unaj.andara.util.EnFx;
import pe.edu.unaj.andara.util.Fechas;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class ConexionService {

    private final CambioPendienteRepository cola;
    private final String urlPrueba;
    private final BooleanProperty redDisponible = new SimpleBooleanProperty(true);
    private final BooleanProperty simulado = new SimpleBooleanProperty(false);
    private final IntegerProperty pendientes = new SimpleIntegerProperty(0);
    private final BooleanBinding enLinea;
    private ScheduledExecutorService vigia;

    public ConexionService(CambioPendienteRepository cola, @Value("${app.conexion.url-prueba:https://www.google.com}") String urlPrueba) {
        this.cola = cola;
        this.urlPrueba = urlPrueba;
        this.enLinea = Bindings.createBooleanBinding(() -> redDisponible.get() && !simulado.get(), redDisponible, simulado);
    }

    public void iniciarMonitoreo() {
        if (vigia != null) {
            return;
        }
        pendientes.set((int) cola.count());
        HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
        vigia = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "andara-conexion");
            t.setDaemon(true);
            return t;
        });
        vigia.scheduleWithFixedDelay(() -> {
            boolean ok;
            try {
                HttpRequest pedido = HttpRequest.newBuilder(URI.create(urlPrueba)).method("HEAD", HttpRequest.BodyPublishers.noBody())
                        .timeout(Duration.ofSeconds(4)).build();
                ok = cliente.send(pedido, HttpResponse.BodyHandlers.discarding()).statusCode() < 500;
            } catch (Exception e) {
                ok = false;
            }
            boolean resultado = ok;
            EnFx.ejecutar(() -> redDisponible.set(resultado));
        }, 2, 15, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void detener() {
        if (vigia != null) {
            vigia.shutdownNow();
        }
    }

    public BooleanBinding enLineaProperty() {
        return enLinea;
    }

    public boolean enLinea() {
        return enLinea.get();
    }

    public boolean sinConexion() {
        return !enLinea.get();
    }

    public BooleanProperty simuladoProperty() {
        return simulado;
    }

    public BooleanProperty redProperty() {
        return redDisponible;
    }

    public boolean isSimulado() {
        return simulado.get();
    }

    public IntegerProperty pendientesProperty() {
        return pendientes;
    }

    public void simularSinConexion(boolean valor) {
        simulado.set(valor);
    }

    @Transactional
    public void encolar(String texto) {
        if (enLinea()) {
            return;
        }
        cola.save(CambioPendiente.builder().texto(texto).creado(Fechas.ahora()).build());
        pendientes.set((int) cola.count());
    }

    @Transactional(readOnly = true)
    public int contarPendientes() {
        return (int) cola.count();
    }

    @Transactional
    public int sincronizar() {
        int n = (int) cola.count();
        cola.deleteAllInBatch();
        pendientes.set(0);
        return n;
    }
}
