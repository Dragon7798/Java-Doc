package com.openAiServlet.job;


import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.gson.JsonArray;
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
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


@Component(service = JobConsumer.class, immediate = true, property = {JobConsumer.PROPERTY_TOPICS + "=" + SendRequestJob.TOPIC_NAME})
public class SendRequestJob implements JobConsumer {

    public static final String TOPIC_NAME = "job-call-gpt";
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private static void zipFolder(File folder, ZipOutputStream zos) throws IOException {
        File[] files = folder.listFiles();
        byte[] buffer = new byte[1024];

        for (File file : files) {
            if (file.isDirectory()) {
                zipFolder(file, zos);
            } else {
                FileInputStream fis = new FileInputStream(file);
                String entryPath = file.getPath().replace("\\", "/");
                String zipEntryName = entryPath.substring(entryPath.indexOf("/") + 1);

                zos.putNextEntry(new ZipEntry(zipEntryName));

                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }

                zos.closeEntry();
                fis.close();
            }
        }
    }

    @Override
    public JobResult process(Job job) {
        final String API_ENDPOINT = "https://api.openai.com/v1/chat/completions";
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost post = new HttpPost(API_ENDPOINT);

        String apiKey = "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";
        LOGGER.info("API KEY : {}",apiKey);
        post.setHeader("Authorization", "Bearer " + apiKey);
        List<String> responseList = new ArrayList<>();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
        LOGGER.info("Executing Job: {}", job);

        try {
            read(job.getProperty("file", File.class), httpClient, post, scheduler, responseList);

            File f = new File(job.getProperty("file", File.class).getAbsolutePath());
            f.setReadable(true);
            f.setWritable(true);

            String outputFolderPath = "D:/output/".concat(f.getName().replaceFirst("[.][^.]+$", ""));
            File zipF = new File(outputFolderPath);
            zipF.mkdirs();
            zipF.setWritable(true);
            zipF.setReadable(true);

            if (f.isFile()) {
                outputFolderPath = "D:/output/".concat(f.getName().replaceFirst("[.][^.]+$", "").concat(".java"));

            }

            String zipFilePath = outputFolderPath + "/" + f.getName().replaceFirst("[.][^.]+$", "") + ".zip";


            try {
                FileOutputStream fos = new FileOutputStream(zipFilePath);
                ZipOutputStream zos = new ZipOutputStream(fos);

                zipFolder(new File(f.getAbsolutePath()), zos);

                zos.close();
                fos.close();

                System.out.println("Folder has been zipped successfully!");
            } catch (IOException e) {
                e.printStackTrace();
            }

            /*******     -----          ********/

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        return JobResult.OK;
    }


    public CompletableFuture<String> sendRequests(Object obj, CloseableHttpClient httpClient, HttpPost post, ScheduledExecutorService scheduler, List<String> sb) throws InterruptedException {

        return CompletableFuture.supplyAsync(() -> {
            try {
                return response(obj, httpClient, post);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, scheduler).whenComplete((res, throwable) -> {
            sb.add(res.concat("\n"));
        });
    }

    public String response(Object obj, CloseableHttpClient httpClient, HttpPost post) throws IOException {

        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", " give me java doc for this method \n " + obj.toString());

        JsonArray msgArrray = new JsonArray();
        msgArrray.add(message);

        JsonObject reqBody = new JsonObject();
        reqBody.add("messages", msgArrray);
        reqBody.addProperty("model", "gpt-3.5-turbo");

        StringEntity requestBody = new StringEntity(reqBody.toString(), ContentType.APPLICATION_JSON);
        post.setEntity(requestBody);

        CloseableHttpResponse httpResponseBody = httpClient.execute(post);

        HttpEntity responseEntity = httpResponseBody.getEntity();
        String responseBody = EntityUtils.toString(responseEntity);
        LOGGER.info("Response Status Code : --> {}", httpResponseBody.getStatusLine().getStatusCode());
        if (httpResponseBody.getStatusLine().getStatusCode() != 200) {
            JsonObject res = new JsonParser().parse(responseBody).getAsJsonObject();
            LOGGER.info("Response : --> {}", res);
            return res.getAsJsonObject("error").get("message").getAsString();
        } else {
            JsonObject res = new JsonParser().parse(responseBody).getAsJsonObject();
            LOGGER.info("Response : --> {}", res);
            return res.get("choices").getAsJsonArray().get(0).getAsJsonObject().get("message").getAsJsonObject().get("content").getAsString();
        }


    }


    private void read(File folder, CloseableHttpClient httpClient, HttpPost post, ScheduledExecutorService scheduler, List<String> sb) throws IOException {

        List<File> files = new ArrayList<>();

        if (!folder.isFile()) {
            files.addAll(Arrays.asList(Objects.requireNonNull(folder.listFiles())));
        } else {
            files.add(folder);
        }

        for (File single : files) {
            if (single.isFile()) {

                if (!(single.getName().split("\\.").length > 1)) {
                    continue;
                } else {
                    if (!(single.getName().split("\\.")[1].equalsIgnoreCase("java"))) {
                        continue;
                    }
                }

                CompilationUnit cu = StaticJavaParser.parse(new File(single.getAbsolutePath()));
                List<MethodDeclaration> array = cu.findAll(MethodDeclaration.class);

                for (int i = 0; i < array.size(); i++) {

                    try {

                        sendRequests(array.get(i), httpClient, post, scheduler, sb);
                        Thread.sleep(20 * 1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    String name = String.valueOf(array.get(i).getName());
                    LOGGER.info("Functon Name --> {}", name);
                    cu.accept(new VoidVisitorAdapter<Void>() {
                        @Override
                        public void visit(MethodDeclaration method, Void arg) {

                            if (method.getNameAsString().equals(name)) {
                                String modMethod = sb.get(sb.size() - 1);
                                LOGGER.info("Modified Method in Response List ---> {}", modMethod);

//                                MethodDeclaration modifiedMethod = StaticJavaParser.parseMethodDeclaration(modMethod);

                                ParseResult<CompilationUnit> parseResult = new JavaParser().parse(modMethod);

                                if (parseResult.isSuccessful()) {
                                    CompilationUnit compilationUnit = parseResult.getResult().orElse(null);
                                    method.setBlockComment(compilationUnit.getAllComments().get(0).asString().replace("/", ""));
                                } else {
                                    method.setBlockComment(modMethod.substring(0, modMethod.lastIndexOf(" */") + 4).replace("/", ""));
                                }

//                                method.setBlockComment(StaticJavaParser.parse(modMethod).getAllComments().get(0).asString());
                                LOGGER.info("After Method changes : --> {}", method);

                            }
                            super.visit(method, arg);
                        }
                    }, null);

                    FileOutputStream fileOutputStream = new FileOutputStream(single);
                    fileOutputStream.write(cu.toString().getBytes());
                    fileOutputStream.close();
                }

            } else {
                read(single, httpClient, post, scheduler, sb);
            }
        }

    }
}