package mx.edu.cenidet.estadias.config;

import org.apache.fop.apps.FopConfParser;
import org.apache.fop.apps.FopFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.imageio.ImageIO;
import java.net.URI;

@Configuration
public class FopConfig {

    @Bean
    public FopFactory fopFactory() throws Exception {
        // Ensure Java's built-in PNG/JPEG readers are registered in the ImageIO registry
        ImageIO.scanForPlugins();

        // FopFactory.newInstance(URI) uses the URI as a *base URI*, not as a config file,
        // so the config XML was never being parsed. FopConfParser correctly handles both.
        //
        // Spring Boot's RestartClassLoader (DevTools) may not expose META-INF/services
        // from dependency JARs when ServiceLoader initialises ImageImplRegistry, which
        // causes "No ImagePreloader found" for every image format. Switching to FOP's own
        // classloader guarantees xmlgraphics-commons SPI entries are discovered.
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(FopFactory.class.getClassLoader());
            ClassPathResource config = new ClassPathResource("fop/fop-config.xml");
            URI baseUri = new ClassPathResource("fop/fop-config.xml").getURI().resolve(".");
            return new FopConfParser(config.getInputStream(), baseUri)
                    .getFopFactoryBuilder()
                    .build();
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }
}
