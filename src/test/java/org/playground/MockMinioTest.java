package org.playground;
import com.adobe.testing.s3mock.junit5.S3MockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class MockMinioTest extends MinioTest{

    @RegisterExtension
    public static S3MockExtension S3_MOCK = S3MockExtension.builder()
            .withHttpPort(9000)
            .withSecureConnection(false)
            .build();

    @Test
    void testMinio() {
        String imageUrl = uploadImageToMinio("squirl.jpg", false);
        System.out.println("Public Image URL: " + imageUrl);
    }

}
