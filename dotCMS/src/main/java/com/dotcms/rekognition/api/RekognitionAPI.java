package com.dotcms.rekognition.api;

import com.dotmarketing.util.Config;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
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
import com.dotmarketing.business.DotStateException;

public class RekognitionApi {


  private final AWSCredentials awsCredentials;
  private final AmazonRekognition client;
  private final float minConfidence;
  private final int maxLabels;

  public RekognitionApi() {

    this.awsCredentials = credentials();
    
    this.client = AmazonRekognitionClientBuilder
                    .standard()
                    .withRegion(Regions.US_WEST_2)
                    .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                    .build();
    

    this.maxLabels = Integer.parseInt(Config.getStringProperty("max.labels", "15"));
    this.minConfidence = Float.parseFloat(Config.getStringProperty("min.confidence", "75"));

  }

  public List<String> detectLabels(File file, int maxLabels, float minConfidence) {
    try {
      return _detectLabels(file, maxLabels, minConfidence);
    } catch (Exception e) {
      throw new DotStateException(e.getMessage(), e);
    }

  }


  public List<String> detectLabels(File file) {
    try {
      return _detectLabels(file, maxLabels, minConfidence);
    } catch (Exception e) {
      throw new DotStateException(e.getMessage(), e);
    }

  }


  private List<String> _detectLabels(File file, int maxLabels, float minConfidence) throws IOException {



    try (RandomAccessFile aFile = new RandomAccessFile(file.getAbsolutePath(), "r")) {

      FileChannel inChannel = aFile.getChannel();
      MappedByteBuffer buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, inChannel.size());
      buffer.load();
      Image image = new Image().withBytes(buffer);

      DetectLabelsRequest request =
          new DetectLabelsRequest()
          .withImage(image)
          .withMaxLabels(maxLabels)
          .withMinConfidence(minConfidence);

      DetectLabelsResult result = client.detectLabels(request);
      buffer.clear();


      List<Label> awsLabels = result.getLabels();

      List<String> labels = new ArrayList<>();

      for (Label l : awsLabels) {

        labels.add(l.getName());

      }

      return labels;
    }


  }



  private AWSCredentials credentials() {

    // todo: from secrets
    String key = Config.getStringProperty("aws.key","");

    String secret = Config.getStringProperty("aws.secret","");



    return new BasicAWSCredentials(key, secret);
  }


}
