package org.playground;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class AWSTest {

    private static final String MINIO_URL = "http://127.0.0.1:9000";
    private static final String ACCESS_KEY = "minioadmin";
    private static final String SECRET_KEY = "minioadmin";
    private static final String BUCKET_NAME = "images-aws8";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @SneakyThrows
    @Test
    void testAWS() {
        AwsBasicCredentials credentials =
                AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY);

        S3Client s3Client = S3Client.builder()
                .endpointOverride(new URI(MINIO_URL))
//                .region(Region.US_EAST_1)
                .serviceConfiguration(e -> e.pathStyleAccessEnabled(false))
                .overrideConfiguration(c -> {
                    c.putAdvancedOption(SdkAdvancedClientOption.SIGNER,
                            AwsS3V4Signer.create());
                })
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();

        InputStream inputStream = new ClassPathResource("squirl.jpg").getInputStream();
        String uuid = UUID.randomUUID().toString();

        createBucket(s3Client);

        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(BUCKET_NAME)
                        .key(uuid)
                        .contentType("image/jpeg")
                        .build(),
                RequestBody.fromInputStream(inputStream, inputStream.available()));

        String format = String.format("%s/%s/%s", MINIO_URL, BUCKET_NAME, uuid);
        System.out.println(format);

    }

    private void createBucket(S3Client s3Client) {
        HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                .bucket(BUCKET_NAME)
                .build();

        try {
            s3Client.headBucket(headBucketRequest);
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder()
                    .bucket(BUCKET_NAME)
                    .build());
            PutBucketPolicyRequest putBucketPolicyRequest = PutBucketPolicyRequest
                    .builder()
                    .bucket(BUCKET_NAME)
                    .policy(getPublicBucketPolicy(BUCKET_NAME))
                    .build();
            s3Client.putBucketPolicy(putBucketPolicyRequest);
        }
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
