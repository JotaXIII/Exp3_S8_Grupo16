package com.transportista.gestionguias.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StorageProperties {

    @Value("${storage.path}")
    private String storagePath;

    public String getStoragePath() {
        return storagePath;
    }
}
