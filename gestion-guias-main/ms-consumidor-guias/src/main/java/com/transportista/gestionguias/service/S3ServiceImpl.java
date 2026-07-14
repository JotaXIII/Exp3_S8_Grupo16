package com.transportista.gestionguias.service;

import java.io.File;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.transportista.gestionguias.config.S3Properties;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3ServiceImpl implements S3Service {

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    public S3ServiceImpl(S3Client s3Client, S3Properties s3Properties) {
        this.s3Client = s3Client;
        this.s3Properties = s3Properties;
    }

    @Override
    public String subirArchivo(File archivo, String transportista, String nombreArchivo) {
        String s3Key = LocalDate.now().getYear()
                + "/"
                + transportista
                + "/"
                + nombreArchivo;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(s3Properties.getBucketName())
                .key(s3Key)
                .contentType("application/pdf")
                .build();

        s3Client.putObject(request, RequestBody.fromFile(archivo));

        return s3Key;
    }

    @Override
    public byte[] descargarArchivo(String s3Key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(s3Properties.getBucketName())
                .key(s3Key)
                .build();

        ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
        return response.asByteArray();
    }

    @Override
    public void actualizarArchivo(File archivo, String s3Key) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(s3Properties.getBucketName())
                .key(s3Key)
                .contentType("application/pdf")
                .build();

        s3Client.putObject(request, RequestBody.fromFile(archivo));
    }

    @Override
    public void eliminarArchivo(String s3Key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(s3Properties.getBucketName())
                .key(s3Key)
                .build();

        s3Client.deleteObject(request);
    }
}
