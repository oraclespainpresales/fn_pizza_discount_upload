package com.example.fn;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.*;

public class UploadDiscountCampaigns {

    private final String ordsBaseUrl      = System.getenv().get("DB_ORDS_BASE");
    private final String ordsService      = System.getenv().get("DB_ORDS_SERVICE");
    private final String ordsServiceOauth = System.getenv().get("DB_ORDS_SERVICE_OAUTH");
    private final HttpClient httpClient   = HttpClient.newHttpClient();

    public static class Campaign {
        public String id            = "";
        public String demozone      = "";
        @JsonAlias("paymentmethod")
        public String paymentMethod = "";
        @JsonAlias("min_amount")
        public String minAmount     = "";
        public String discount      = "";
        @JsonAlias("date_bgn")
        public String dateBgn       = "";
        @JsonAlias("date_end")
        public String dateEnd       = "";
        @JsonIgnore
        public List<String> links;

        public String toString() {
            StringBuilder stb = new StringBuilder("{");
            stb.append("'id':'").append(id).append("',");
            stb.append("'demozone':'").append(demozone).append("',");
            stb.append("'paymentMethod':'").append(paymentMethod).append("',");
            stb.append("'min_amount':'").append(minAmount).append("',");
            stb.append("'discount':'").append(discount).append("',");
            stb.append("'date_ini':'").append(dateBgn).append("',");
            stb.append("'date_end':'").append(dateEnd).append("'");
            stb.append("}");
            return stb.toString();
        }
    }

    public String handleRequest(String input) {
        StringBuilder ordsServiceUrl = new StringBuilder(ordsBaseUrl).append(ordsService).append("/");//.append(input);
        //Campaign campaign            = null;
        String responseMess          = "";
        try {
            System.err.println("inside Load Discount Campaign Function!");
            System.err.println("ORDS URL: " + ordsServiceUrl.toString());
            //campaign = getCampaignDiscount (ordsServiceUrl, input);
            responseMess = setCampaignDiscount (ordsServiceUrl, input);
        }
        catch (URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return responseMess;
    }

    /************************GET TOKEN ********************************** 
     * get the appropiate token to Oauth access with ORDS to ATP DB
     * this is an only 1h hour Token
     * use CLIENT_ID and CLIENT_SECRET to get the token
    */
    private String getAuthToken() {
        String authToken           = "";
        String clientId            = "";
        String clientSecret        = "";
        StringBuilder authTokenURL = new StringBuilder(ordsBaseUrl).append(ordsServiceOauth);

        try {
            clientId     = System.getenv().get("DB_ORDS_CLIENT_ID");
            clientSecret = System.getenv().get("DB_ORDS_CLIENT_SECRET");

            StringBuilder authString   = new StringBuilder(clientId).append(":").append(clientSecret);
            StringBuilder authEncoded  = new StringBuilder("Basic ").append(Base64.getEncoder().encodeToString(authString.toString().getBytes()));

            //System.err.println("ORDS URL token: " + authTokenURL.toString());
            //System.err.println("ORDS URL 64B  : " + authEncoded.toString());

            HttpRequest request = HttpRequest.newBuilder(new URI(authTokenURL.toString()))
                    .header("Authorization", authEncoded.toString())
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                    .build();

            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            ObjectMapper mapper = new ObjectMapper();
            TypeReference<HashMap<String, String>> typeRef = new TypeReference<HashMap<String, String>>() {};
            HashMap<String, String> result = mapper.readValue(responseBody, typeRef);
            authToken = result.get("access_token");
        }
        catch (URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return authToken;
    }

    /************************getCampaignDiscount ********************************** 
     * get one row from ATP
     * input must be an id number or field value in json format
    */
    /* private Campaign getCampaignDiscount (StringBuilder ordsServiceUrl, String input) 
        throws URISyntaxException,IOException,InterruptedException {
        Campaign campaign = null;
        
        HttpRequest request = HttpRequest.newBuilder(new URI(ordsServiceUrl.toString()))
                    .header("Authorization", "Bearer " + getAuthToken())
                    .GET()
                    .build();

        HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.err.println("Response HTTP:::" +response.statusCode());
        if( response.statusCode() == HttpURLConnection.HTTP_NOT_FOUND ) {
            System.err.println("Campaign with id " + input + " not found!");
            campaign = new Campaign();
            campaign.demozone = "NOT FOUND RECORD " + input;
        }
        else {
            campaign = new ObjectMapper().readValue(response.body(), Campaign.class);
        }
        
        return campaign;
    } */

    /************************setCampaignDiscount ********************************** 
     * insert one row in ATP from a json format data
    */
    private String setCampaignDiscount (StringBuilder ordsServiceUrl, String input) 
        throws URISyntaxException,IOException,InterruptedException {
        String responseMess = "";
        
        HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(ordsServiceUrl.toString()))
                    .header("Authorization", "Bearer " + getAuthToken())
                    .header("Content-Type", "application/json")
                    .POST(BodyPublishers.ofString(input))
                    .build();

        System.err.println("HOLA CARA DE COLA");
        HttpResponse<Void> response = this.httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        System.err.println("Response HTTP:::" +response.statusCode());//+ " " + response.body());
        if( response.statusCode() == HttpURLConnection.HTTP_CREATED) {
            responseMess = new StringBuilder ("[")
                            .append(input)
                            .append("] - INSERTED")
                            .toString();
        }
        else {
            if (response.statusCode() == HttpURLConnection.HTTP_UNAVAILABLE) {
                responseMess = new StringBuilder ("[")
                            .append(input)
                            .append("] - ERROR insertion!! - ")
                            .append(response.statusCode())
                            .append(" Check ATP, it must be started!!!")
                            .toString();
            }
            else{
                responseMess = new StringBuilder ("[")
                            .append(input)
                            .append("] - ERROR insertion!! - ")
                            .append(response.statusCode())
                            .toString();
            }
        }
        return responseMess;
    }
}