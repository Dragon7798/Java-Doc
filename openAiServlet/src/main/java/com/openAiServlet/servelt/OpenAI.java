/**
 * This class is a servlet used to send requests to the OpenAI API endpoint for generating text or editing images.
 * It accepts GET requests with a JSON body containing information about the request, including the model to use,
 * the type of input (text or file), and the prompt or file path to use as input. If the input is text, additional
 * information about max tokens to generate can be included. The servlet then sends a POST request to the API
 * endpoint with the provided information, including an authorization token. It parses the response from the API
 * and returns the generated or edited text in a JSON response.
 *
 * @author (Team AI)
 * @version (1.0)
 */
package com.openAiServlet.servelt;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.Servlet;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Component(service = Servlet.class, immediate = true, property = {
        Constants.SERVICE_DESCRIPTION + "=ESAPI Filter",
        "sling.servlet.methods" + "=" + HttpConstants.METHOD_POST,
        "sling.servlet.methods" + "=" + HttpConstants.METHOD_GET,
        "sling.servlet.resourceTypes" + "=/apps/shaft/gpt"
})
public class OpenAI extends SlingAllMethodsServlet {
    private static final String encryptionKey = "MySecretKey12345";
    private final Logger LOGGER = LoggerFactory.getLogger(OpenAI.class);
    @Reference
    JobManager jobManager;

    private static String encryptText(String plainText, String key) throws Exception {
        SecretKeySpec secretKey = generateSecretKey(key);
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    private static String decryptText(String encryptedText, String key) throws Exception {
        SecretKeySpec secretKey = generateSecretKey(key);
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    private static SecretKeySpec generateSecretKey(String key) throws Exception {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        keyBytes = sha.digest(keyBytes);
        keyBytes = Arrays.copyOf(keyBytes, 16); // AES-128 key length
        return new SecretKeySpec(keyBytes, "AES");
    }

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

    public static File convertBase64ToFile(String base64String, String outputFilePath) throws IOException {
        // Remove the "data:image/png;base64," prefix if present
        String base64Data = base64String.replaceAll("data:[^,]+,", "");

        // Decode the Base64 string
        byte[] decodedBytes = Base64.getDecoder().decode(base64Data);

        // Write the decoded bytes to the output file
        File outputFile = new File(outputFilePath);

//        Files.copy(fileInputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        InputStream fileInputStream = new ByteArrayInputStream(decodedBytes);
        Files.copy(fileInputStream, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        return outputFile;
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {

        String key = request.getRequestParameter("token").toString().replaceAll(" ", "+");
        try {
            String decryptedText = decryptText(key, encryptionKey);
            String jobId = decryptedText.split("@")[0];
            String path = decryptedText.split("@")[1];

            Job job = jobManager.getJobById(jobId);

            if (Objects.nonNull(job)) {
                response.setContentType("application/json");
                JsonObject res = new JsonObject();
                res.addProperty("status", "Your documentation is in progress. Please wait. Do not close this window. Refresh to download or check status");
                response.getWriter().println(res);
            } else {
                try {

                    String ZFP = "";
                    File f = new File(path);
                    f.setReadable(true);
                    f.setWritable(true);

                    if (f.isDirectory()) {
                        String outputFolderPath = "D:/output/".concat(f.getName());
                        File zipF = new File(outputFolderPath);
                        zipF.mkdirs();
                        zipF.setWritable(true);
                        zipF.setReadable(true);

                        String zipFilePath = outputFolderPath + "/" + f.getName() + ".zip";
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

                        ZFP = zipFilePath;

                    } else {
                        String outputFolderPath = "D:/output/".concat(f.getName().replaceFirst("[.][^.]+$", ""));
                        String zipFilePath = outputFolderPath + "/" + f.getName().replaceFirst("[.][^.]+$", "") + ".zip";

                        FileOutputStream fos = new FileOutputStream(zipFilePath);

                        ZipOutputStream zipOut = new ZipOutputStream(fos);

                        File fileToZip = new File(f.getAbsolutePath());
                        FileInputStream fis = new FileInputStream(fileToZip);
                        ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
                        zipOut.putNextEntry(zipEntry);

                        byte[] bytes = new byte[1024];
                        int length;
                        while ((length = fis.read(bytes)) >= 0) {
                            zipOut.write(bytes, 0, length);
                        }

                        zipOut.closeEntry();
                        fis.close();
                        zipOut.close();
                        fos.close();

                        ZFP = zipFilePath;
                    }

                    /*******     -----          ********/


                    response.setContentType("application/zip");

                    String headerValue = String.format("attachment; filename=\"%s\"", "project.zip");
                    response.setHeader("Content-Disposition", headerValue);


                    OutputStream outputStream = response.getOutputStream();
                    InputStream inputStream = new FileInputStream(ZFP);

                    byte[] buffer = new byte[4096];
                    int bytesRead;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    inputStream.close();
                    outputStream.close();

                    /***
                     *
                     * Code to delete file after download
                     * File Will be Deleted 10 minutes after download
                     *
                     * **/
                    File file = new File(ZFP);
                    ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
                    executorService.schedule(() -> {
                        boolean deleted = file.delete();
                        if (deleted) {
                            System.out.println("File deleted successfully.");
                        } else {
                            System.out.println("Failed to delete the file.");
                        }
                    }, 10, TimeUnit.MINUTES);


                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }

// keep till 10 mins then deleted
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Method to handle GET requests containing a JSON body with information about the desired
     * text generation or image editing.
     *
     * @param request  the incoming HTTP request
     * @param response the outgoing HTTP response
     * @throws IOException if an error occurs while processing the request
     */
    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {

        response.setContentType("application/json");
        if (request.getContentType().contains("multipart/form-data")) {
            if (request.getRequestParameterList().size() > 0) {
                String type = request.getRequestParameterList().get(0).getName();
                if (type.equalsIgnoreCase("file")) {
                    RequestParameter[] fileParameters = request.getRequestParameters(type);
                    if (Objects.nonNull(fileParameters)) {

                        for (RequestParameter fileParameter : fileParameters) {
                            String fileName = fileParameter.getFileName();
                            try (InputStream fileInputStream = fileParameter.getInputStream()) {
                                File tempFile = new File(fileName);
                                Files.copy(fileInputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                                String jobId = callJob(tempFile);
                                JsonObject res = new JsonObject();
                                res.addProperty("statusUrl", request.getRequestURL() + "?token=" + jobId);
                                response.getWriter().println(res);
                            } catch (Exception e) {
                                throw new EOFException("Failed to Read the File");
                            }
                        }

                    }
                } else if (type.equalsIgnoreCase("project")) {

                    RequestParameter fileParameter = request.getRequestParameter(type);
                    InputStream inputStream = fileParameter.getInputStream();
                    String destinationDirectory = "D:\\projects";

                    File destDir = new File(destinationDirectory);
                    destDir.setWritable(true);
                    destDir.setReadable(true);

                    try (ZipInputStream zipIn = new ZipInputStream(inputStream)) {
                        ZipEntry ze;
                        while ((ze = zipIn.getNextEntry()) != null) {
                            String filePath = destinationDirectory + ze.getName();
                            if (!ze.isDirectory()) {
                                new File(filePath).getParentFile().mkdirs();

                                try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                                    byte[] buffer = new byte[4096];
                                    int bytesRead;
                                    while ((bytesRead = zipIn.read(buffer)) != -1) {
                                        outputStream.write(buffer, 0, bytesRead);
                                    }
                                } catch (IOException e) {
//                                throw new ServiceException("Failed to Read the File", -1, false);
                                    throw new EOFException("Failed to Read the File");
                                }
                            }
                            zipIn.closeEntry();
                        }
                        destDir = new File(destDir + fileParameter.getFileName().replaceFirst("[.][^.]+$", ""));
                        String jobId = callJob(destDir);
                        JsonObject res = new JsonObject();
                        res.addProperty("statusUrl", request.getRequestURL() + "?token=" + jobId);
                        response.getWriter().println(res);

                    } catch (Exception e) {
//                    throw new ServiceException("Failed to Unzip the File", -1, false);
                        throw new EOFException("Failed to Read the File");
                    }
                }
            } else {
                JsonObject res = new JsonObject();
                res.addProperty("statusUrl", request.getRequestURL() + "?token=");
                response.getWriter().println(res);
            }
        } else if (request.getContentType().contains("application/json")) {
            try {
                String req = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
                JsonObject body = new JsonParser().parse(req).getAsJsonObject();

                String fileString = body.get("key").getAsString();
                String fileName = body.get("fileName").getAsString();

                File path = convertBase64ToFile(fileString, "D:\\Download\\" + fileName);

                String jobId = callJob(path);

                JsonObject res = new JsonObject();
                res.addProperty("statusUrl", request.getRequestURL() + "?token=" + jobId);
                response.getWriter().println(res);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }

    }

    String callJob(File file) throws Exception {

        Map<String, Object> payload = new HashMap<>();
        payload.put("file", file);

        LOGGER.info("job-push-query-data job payload : {}", payload);

        String inputKey = this.jobManager.addJob("job-call-gpt", payload).getId().concat("@").concat(file.getAbsolutePath());
        String encryptedText = encryptText(inputKey, encryptionKey);
        System.out.println("Encrypted Text: " + encryptedText);


        return encryptedText;


    }
}

