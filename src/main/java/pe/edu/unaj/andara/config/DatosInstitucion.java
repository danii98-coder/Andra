package pe.edu.unaj.andara.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.institucion")
@Getter
@Setter
public class DatosInstitucion {
    private String nombre = "Universidad Nacional de Juliaca";
    private String sigla = "UNAJ";
    private String telefono = "(051) 323200";
    private String anexos = "0000 / 0000";
    private String correoMesaPartes = "mesadepartes@unaj.edu.pe";
    private String correoSoporte = "soporte.academico@unaj.edu.pe";
    private String correoDefensoria = "defensoria@unaj.edu.pe";
    private String urlYoutube = "https://www.youtube.com/@universidadnacionaldejulia3566";
    private String urlFacebook = "https://www.facebook.com/UNAJ.Pagina.Oficial";
    private String urlDocumentos = "https://sigu.unaj.edu.pe";
}
