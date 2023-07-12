package eu.cessda.pasc.oci.configurations;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@ConfigurationPropertiesBinding
public class PathConverter implements Converter<String, Path> {
    @Override
    public Path convert(@NonNull String s) {
        return Path.of(s).normalize();
    }
}
