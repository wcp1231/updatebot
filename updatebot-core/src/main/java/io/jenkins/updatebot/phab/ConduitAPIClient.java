package io.jenkins.updatebot.phab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ConduitAPIClient {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String API_TOKEN_KEY = "token";
    private static final String CONDUIT_METADATA_KEY = "__conduit__";

    private final String conduitURL;
    private final String conduitToken;

    public ConduitAPIClient(String conduitURL, String conduitToken) {
        this.conduitURL = "https://" + conduitURL;
        this.conduitToken = conduitToken;
    }

    public String getConduitToken() {
        return conduitToken;
    }

    /**
     * Call the conduit API of Phabricator
     *
     * @param action Name of the API call
     * @param params The data to send to Harbormaster
     * @return The result as a JSONObject
     * @throws IOException         If there was a problem reading the response
     * @throws ConduitAPIException If there was an error calling conduit
     */
    public JsonNode perform(String action, ObjectNode params) throws IOException, ConduitAPIException {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpUriRequest request = createRequest(action, params);

        HttpResponse response;
        try {
            response = client.execute(request);
        } catch (ClientProtocolException e) {
            throw new ConduitAPIException(e.getMessage());
        }

        InputStream responseBody = response.getEntity().getContent();

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new ConduitAPIException(responseBody.toString(), response.getStatusLine().getStatusCode());
        }

        return OBJECT_MAPPER.readTree(responseBody);
    }

    /**
     * Post a URL-encoded "params" key with a JSON-encoded body as per the Conduit API
     *
     * @param action The name of the Conduit method
     * @param params The data to be sent to the Conduit method
     * @return The request to perform
     * @throws UnsupportedEncodingException when the POST data can't be encoded
     * @throws ConduitAPIException          when the conduit URL is misconfigured
     */
    public HttpUriRequest createRequest(String action, ObjectNode params) throws UnsupportedEncodingException,
            ConduitAPIException {
        HttpPost post;
        try {
            post = new HttpPost(
                    new URL(new URL(new URL(conduitURL), "/api/"), action).toURI()
            );
        } catch (MalformedURLException e) {
            throw new ConduitAPIException(e.getMessage());
        } catch (URISyntaxException e) {
            throw new ConduitAPIException(e.getMessage());
        }

        ObjectNode conduitParams = OBJECT_MAPPER.createObjectNode();
        conduitParams.put(API_TOKEN_KEY, conduitToken);
        params.put(CONDUIT_METADATA_KEY, conduitParams);

        List<NameValuePair> formData = new ArrayList<>();
        formData.add(new BasicNameValuePair("params", params.toString()));

        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formData, "UTF-8");
        post.setEntity(entity);

        return post;
    }
}
