package uos.jhoffjann.ObjFinder.Logic;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.File;
import java.util.Date;

/**
 * Created by jhoffjann on 13.11.14.
 */
public class Upload {

    /**
     * Manages the Upload and waiting for Response
     * @param URL the URL to upload to
     * @param image the image
     * @return the response
     */
    public static String[] upload(String URL, File image) {
        String[] token = new String[2];
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(URL);

            MultipartEntityBuilder entity = MultipartEntityBuilder.create();
            entity.addTextBody("name", new Date() + "");
            entity.addBinaryBody("file", image);

            httpPost.setEntity(entity.build());
            HttpResponse response = httpClient.execute(httpPost);

            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != 200) {
                token[0] = "Error: " + statusCode + " Something in the uploading process went wrong";
                return token;
            }

            if (response.getEntity() != null) {
                HttpEntity responseEntity = response.getEntity();
                String resStr = EntityUtils.toString(responseEntity);

                // parse to JSON
                JSONObject result = new JSONObject(resStr);
                token[0] = result.getString("message");
                token[1] = result.getString("name");
                responseEntity.consumeContent();
                return token;
            }
            token[0] = "Something went terrible wrong";
            return token;
        } catch (Exception e) {
            e.printStackTrace();
            token[0] = "The upload server is not reachable. We're sorry";
            return token;
        }
    }
}
