package com.puyuanmaoshan.platform.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebMvcConfig.class);

    @Value("${upload.base-path:./uploads}")
    private String uploadBasePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Resolve relative path to absolute, then build file: URI
        Path absolutePath = Paths.get(uploadBasePath).toAbsolutePath().normalize();
        String location = absolutePath.toUri().toString();
        log.info("Mapping /uploads/** -> {}", location);

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
