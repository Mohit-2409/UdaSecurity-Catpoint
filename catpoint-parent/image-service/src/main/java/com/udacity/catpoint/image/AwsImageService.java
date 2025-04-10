package com.udacity.catpoint.image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AwsImageService implements ImageService {
    private static final Logger log = LoggerFactory.getLogger(AwsImageService.class);
    private RekognitionClient rekognitionClient;

    public AwsImageService() {
        try {
            initializeRekognitionClient();
        } catch (Exception e) {
            log.error("Failed to initialize AWS Rekognition client", e);
            rekognitionClient = null;
        }
    }
    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (is == null) {
                throw new IOException("config.properties file not found");
            }
            props.load(is);
            return props;
        } catch (IOException e) {
            log.error("Failed to load AWS configuration", e);
            return new Properties();
        }
    }
    private void initializeRekognitionClient() {
        Properties props = loadProperties();
        validateConfig(props);
        this.rekognitionClient = RekognitionClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                props.getProperty("aws.id"),
                                props.getProperty("aws.secret"))))
                .region(Region.of(props.getProperty("aws.region")))
                .build();
    }
    private void validateConfig(Properties props) {
        if (!props.containsKey("aws.id") || !props.containsKey("aws.secret") || !props.containsKey("aws.region")) {
            log.error("Missing AWS configuration in config.properties");
            throw new IllegalStateException("Missing AWS configuration");
        }
    }
    @Override
    public boolean imageContainsCat(BufferedImage image, float confidenceThreshold) {
        if (rekognitionClient == null) {
            log.error("AWS Rekognition client not initialized");
            return false;
        }
        if (image == null) {
            log.warn("Null img provided to imageContainsCat");
            return false;
        }
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", os);
            DetectLabelsResponse response = rekognitionClient.detectLabels(
                    DetectLabelsRequest.builder()
                            .image(Image.builder()
                                    .bytes(SdkBytes.fromByteArray(os.toByteArray()))
                                    .build())
                            .minConfidence(confidenceThreshold)
                            .build());
            return response.labels().stream()
                    .anyMatch(label -> "cat".equalsIgnoreCase(label.name()));
        } catch (IOException e) {
            log.error("Failed to process image", e);
            return false;
        } catch (RekognitionException e) {
            log.error("AWS Rekognition error", e);
            return false;
        }
    }
}
