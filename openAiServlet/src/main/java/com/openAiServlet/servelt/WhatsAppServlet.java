package com.openAiServlet.servelt;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import javax.servlet.Servlet;
import java.io.IOException;


@Component(service = Servlet.class, immediate = true, property = {Constants.SERVICE_DESCRIPTION + "=ESAPI Filter", "sling.servlet.methods" + "=" + HttpConstants.METHOD_GET, "sling.servlet.resourceTypes" + "=/apps/shaft/whatsapp"})
public class WhatsAppServlet extends SlingAllMethodsServlet {
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {

        // Replace these values with your actual credentials
        String aadhaarNumber = "999941057058";
        String txnId = "azzsasa555";
        String auaCode = "public";
        String licenseKey = "MAvSQG0jKTW4XxQc2cI-oXZYxYH-zi7IWmsQY1q3JNLlC8VOWOHYGj8";
        String timestamp = "2021-09-30 12:00:00";

        String rc = "Y";
        String version = "2.5";
        String tid = "";

        try {
            /** URL : https://<host>/<ver>/<ac>/<uid[0]>/<uid[1]>/<asalk>
             *
             * AUTH : http://auth.uidai.gov.in/1.6/<1st-digit-of-uid>/<2nd-digit-of-uid>/
             *
             *
             *
             * INPUT DATA :
             *
             * <Auth uid="" rc="" tid="" ac="" sa="" ver="" txn="" lk="">
             * <Uses pi="" pa="" pfa="" bio="" bt="" pin="" otp=""/>
             * <Device rdsId="" rdsVer="" dpId=""dc="" mi=""mc=""/>
             * <Skey ci="">encrypted and encoded session key</Skey>
             * <Hmac>SHA-256 Hash of Pid block, encrypted and then encoded</Hmac>
             * <Data type="X|P">encrypted PID block</Data>
             * <Signature>Digital signature of AUA</Signature>
             * </Auth>
             *
             *
             * ***/
            String apiUrl = "http://developer.uidai.gov.in/uidkyc/2.5/public/0/0/";
            String asalk = "MCNYL7FpPgjEhx7HBp9tu59Vdm4FnYGlxuqHctfAeNNaCufVafshqzQ";

            apiUrl += asalk;
            System.out.println(apiUrl);

            String authTag = "<Auth uid=\"" + aadhaarNumber + "\" rc=\"" + rc + "\" tid=\"" + tid + "\" ac=\"" + auaCode + "\" sa=\"" + auaCode + "\" ver=\"" + version + "\" txn=\"" + txnId + "\" lk=\"" + licenseKey + "\">";
            String uses = "";

            System.out.println(authTag);

            String xmlRequest = authTag + "\n" + "</Auth>";

            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost post = new HttpPost(apiUrl);

            StringEntity requestBody = new StringEntity(xmlRequest, ContentType.APPLICATION_XML);
            post.setEntity(requestBody);

            CloseableHttpResponse httpResponseBody = httpClient.execute(post);

            HttpEntity responseEntity = httpResponseBody.getEntity();
            String responseBody = EntityUtils.toString(responseEntity);

            System.out.println(responseBody);

            if (httpResponseBody.getStatusLine().getStatusCode() != 200) {
                JsonObject res = new JsonParser().parse(responseBody).getAsJsonObject();

            } else {
                JsonObject res = new JsonParser().parse(responseBody).getAsJsonObject();

            }


        } catch (IOException e) {
            response.getWriter().println(e.getCause());
        }

    }

}
