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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.stream.Collectors;

@Designate(ocd = GraphQLServlet.Config.class)
@Component(service = Servlet.class, immediate = true, property = {
        Constants.SERVICE_DESCRIPTION + "=GraphQL API",
        "sling.servlet.methods" + "=" + HttpConstants.METHOD_POST,
        "sling.servlet.methods" + "=" + HttpConstants.METHOD_GET,
        "sling.servlet.resourceTypes" + "=/apps/shaft/graphql"
})
public class GraphQLServlet extends SlingAllMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphQLServlet.class);

    private static String graphqlEndpoint = "";
    @ObjectClassDefinition(name="GraphQL Configurations")
    public @interface Config {
        @AttributeDefinition(name = "GraphQL Base URL", description = "GraphQL URL used for queries")
        String graphql_url() default "https://staging-vdt2zeq-eqo7vpusx3do2.ap-4.magentosite.cloud/graphql";
    }

    @Modified
    @Activate
    protected void activate(Config config){
        graphqlEndpoint = config.graphql_url();
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        String req = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        JsonObject requestObject = new JsonParser().parse(req).getAsJsonObject();

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost post = new HttpPost(graphqlEndpoint);

        StringEntity requestBody = new StringEntity(requestObject.toString(), ContentType.APPLICATION_JSON);
        post.setEntity(requestBody);

        CloseableHttpResponse httpResponseBody = httpClient.execute(post);

        HttpEntity responseEntity = httpResponseBody.getEntity();
        String responseBody = EntityUtils.toString(responseEntity);
        LOGGER.info("Response Status Code : --> {}", httpResponseBody.getStatusLine().getStatusCode());

        response.setContentType("application/json");
        response.getWriter().println(responseBody);
    }
}
