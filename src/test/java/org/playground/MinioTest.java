package org.playground;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.*;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MinioTest {

    private static final String MINIO_URL = "http://127.0.0.1:9000";
    private static final String ACCESS_KEY = "minioadmin";
    private static final String SECRET_KEY = "minioadmin";
    private static final String BUCKET_NAME = "images";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testMinio() {
        String imageUrl = uploadImageToMinio("squirl.jpg", true);
        System.out.println("Public Image URL: " + imageUrl);
    }

    @SneakyThrows
    protected String uploadImageToMinio(String resourceName, boolean createPolicy) {
        MinioClient minioClient = MinioClient.builder()
                .endpoint(MINIO_URL)
                .credentials(ACCESS_KEY, SECRET_KEY)
                .build();

        boolean isBucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(BUCKET_NAME).build());
        if (!isBucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET_NAME).build());
            if (createPolicy) {
                // Make the uploaded image publicly accessible
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                        .bucket(BUCKET_NAME)
                        .config(getPublicBucketPolicy(BUCKET_NAME))
                        .build()
                );

            }
        }
        InputStream inputStream = new ClassPathResource(resourceName).getInputStream();
        String uuid = UUID.randomUUID().toString();
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(BUCKET_NAME)
                        .object(uuid)
                        .stream(inputStream, inputStream.available(), -1)
                        .contentType("image/jpeg")
                        .build()
        );
        return String.format("%s/%s/%s", MINIO_URL, BUCKET_NAME, uuid);
    }

    @SneakyThrows
    private String getPublicBucketPolicy(String bucketName) {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of(
                "Version", "2012-10-17",
                "Statement", List.of(Map.of(
                        "Effect", "Allow",
                        "Principal", Map.of("AWS", "*"),
                        "Action", "s3:GetObject",
                        "Resource", String.format("arn:aws:s3:::%s/*", bucketName)
                ))
        ));
    }

}
