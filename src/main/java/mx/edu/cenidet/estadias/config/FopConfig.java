package mx.edu.cenidet.estadias.config;

import org.apache.fop.apps.FopFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.net.URI;

@Configuration
public class FopConfig {

    @Bean
    public FopFactory fopFactory() throws Exception {
        URI configUri = new ClassPathResource("fop/fop-config.xml").getURI();
        return FopFactory.newInstance(configUri);
    }
}
