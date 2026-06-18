package mx.edu.cenidet.estadias.config;

import org.apache.fop.apps.FopFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.net.URI;

// Configura el FopFactory singleton inyectado en PdfReportService.
// El fop-config.xml en resources/fop/ define la fuente de imágenes
// y el directorio de fuentes institucionales (logo UTEZ/CENIDET).
//
// resources/fop/fop-config.xml (mínimo):
// <fop version="1.0">
//   <renderers>
//     <renderer mime="application/pdf">
//       <fonts><auto-detect/></fonts>
//     </renderer>
//   </renderers>
// </fop>
@Configuration
public class FopConfig {

    @Bean
    public FopFactory fopFactory() throws Exception {
        URI configUri = new ClassPathResource("fop/fop-config.xml").getURI();
        return FopFactory.newInstance(configUri);
    }
}
