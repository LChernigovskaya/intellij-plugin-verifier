package com.jetbrains.pluginverifier;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author Sergey Evdokimov
 */
public class Uploader {

  public static void main(String[] args) throws IOException {
    CloseableHttpClient httpclient = HttpClients.createDefault();

    try {
      HttpPost httppost = new HttpPost(args[0]);

      String filePartName = args[1];
      File file = new File(args[2]);

      MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
      entityBuilder.addPart(filePartName, new FileBody(file));

      for (int i = 3; i < args.length; i += 2) {
        entityBuilder.addTextBody(args[i], args[i + 1]);
      }

      httppost.setEntity(entityBuilder.build());

      System.out.println("Executing request: " + httppost.getRequestLine());
      CloseableHttpResponse response = httpclient.execute(httppost);
      try {
        System.out.println("----------------------------------------");
        System.out.println(response.getStatusLine());
        EntityUtils.consume(response.getEntity());

        if (response.getStatusLine().getStatusCode() != 200) {
          System.exit(2);
        }
      }
      finally {
        response.close();
      }
    }
    finally {
      httpclient.close();
    }

  }
}
