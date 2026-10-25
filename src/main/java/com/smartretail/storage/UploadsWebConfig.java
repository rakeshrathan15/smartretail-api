package com.smartretail.storage;

import java.time.Duration;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Serves files from {@link LocalFileStorage}. Names are random UUIDs, so they can be cached for long. */
@Configuration
public class UploadsWebConfig implements WebMvcConfigurer {

    private final LocalFileStorage storage;
    private final StorageProperties properties;

    public UploadsWebConfig(LocalFileStorage storage, StorageProperties properties) {
        this.storage = storage;
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(properties.publicPath() + "/**")
                .addResourceLocations(storage.root().toUri().toString())
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());
    }
}
