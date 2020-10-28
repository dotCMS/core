package com.dotcms.rekognition.api;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Label;
import java.util.stream.Collectors;

public class RekognitionAPI {


  private final AWSCredentials awsCredentials;
  private final AmazonRekognition client;

  public RekognitionAPI(final String awsKey, final String awsSecret) {

    this.awsCredentials = new BasicAWSCredentials(awsKey, awsSecret);
    
    this.client = AmazonRekognitionClientBuilder
                    .standard()
                    .withRegion(Regions.US_WEST_2)
                    .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                    .build();

  }

  public List<String> generateTags(final File file, final int maxLabels, final float minConfidence) throws IOException {

    try (final RandomAccessFile aFile = new RandomAccessFile(file.getAbsolutePath(), "r")) {

      final FileChannel inChannel = aFile.getChannel();
      final MappedByteBuffer buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, inChannel.size());
      buffer.load();
      final Image image = new Image().withBytes(buffer);

      final DetectLabelsRequest request =
          new DetectLabelsRequest()
          .withImage(image)
          .withMaxLabels(maxLabels)
          .withMinConfidence(minConfidence);

      final DetectLabelsResult result = client.detectLabels(request);
      buffer.clear();

      return result.getLabels().stream().map(Label::getName).collect(Collectors.toList());
    }


  }


}
