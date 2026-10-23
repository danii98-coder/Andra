package pe.edu.unaj.andara.ui;

import javafx.application.HostServices;
import org.springframework.stereotype.Component;

@Component
public class Enlaces {

    private HostServices servicios;

    public void setHostServices(HostServices servicios) {
        this.servicios = servicios;
    }

    public void abrir(String url) {
        if (servicios != null && url != null && !url.isBlank()) {
            servicios.showDocument(url);
        }
    }
}
