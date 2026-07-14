package com.transportista.gestionguias.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class S3Properties {

    @Value("${aws.region}")
    private String region;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public String getRegion() {
        return region;
    }

    public String getBucketName() {
        return bucketName;
    }
}
