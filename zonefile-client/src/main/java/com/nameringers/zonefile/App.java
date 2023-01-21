package com.nameringers.zonefile;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import software.amazon.awssdk.core.sync.RequestBody;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.client.fluent.Request;
import org.apache.http.client.ClientProtocolException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.GZIPOutputStream;

public class App implements RequestHandler<Map<String, String>, String> {
    private final S3Client s3Client;
    private final Gson gson;

    public App() {
        s3Client = DependencyFactory.s3Client();
        gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    public String handleRequest(Map<String, String> event, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("EVENT: " + gson.toJson(event));

        final String zone = event.get("zone");
        final String zoneFilesDir = "/mnt/zonefiles";
        final String archivePath = Paths.get(zoneFilesDir, zone + ".zip").normalize().toString();

        downloadZoneFile(zone, System.getenv("API_TOKEN"), archivePath);

        final String extractedArchivePath = Paths.get(zoneFilesDir, zone).normalize()
                .toString();

        try {
            unzipArchive(archivePath, extractedArchivePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final String zoneFilePath = Paths.get(extractedArchivePath, zone + ".txt").normalize().toString();
        final String compressedZoneFilePath = Paths.get(zoneFilesDir, zone + ".txt.gz").normalize().toString();

        compressZoneFile(zoneFilePath, compressedZoneFilePath);

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(System.getenv("BUCKET"))
                .key(zone + ".txt.gz")
                .contentType("text/plain")
                .contentEncoding("gzip")
                .build();

        s3Client.putObject(objectRequest,
                RequestBody.fromBytes(getObjectFile(compressedZoneFilePath)));

        return "200 OK";
    }

    private static Void downloadZoneFile(String zone, String apiToken, String destPath) {
        final String url = "https://" + Paths
                .get("domains-monitor.com/api/v1/", apiToken, "/get/", zone, "/list/zip/")
                .normalize().toString();
        try {
            Request.Get(url)
                    .execute()
                    .saveContent(new File(destPath));
        } catch (ClientProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    private static Void unzipArchive(String archivePath, String destPath) throws IOException {
        final byte[] buffer = new byte[1024];
        final ZipInputStream zis = new ZipInputStream(new FileInputStream(archivePath));
        ZipEntry zipEntry = zis.getNextEntry();

        while (zipEntry != null) {
            final File newFile = newFile(new File(destPath), zipEntry);

            if (zipEntry.isDirectory()) {
                newFile.mkdirs();
            } else {
                File parent = newFile.getParentFile();
                parent.mkdirs();

                final FileOutputStream fos = new FileOutputStream(newFile);

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
            }

            zipEntry = zis.getNextEntry();
        }

        zis.closeEntry();
        zis.close();

        return null;
    }

    public static File newFile(File destDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destDir, zipEntry.getName());
        String destDirPath = destDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    private static byte[] getObjectFile(String filePath) {
        FileInputStream fileInputStream = null;
        byte[] bytesArray = null;

        try {
            File file = new File(filePath);
            bytesArray = new byte[(int) file.length()];
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(bytesArray);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return bytesArray;
    }

    private static void compressZoneFile(String srcPath, String destPath) {
        try {
            FileInputStream fis = new FileInputStream(srcPath);
            FileOutputStream fos = new FileOutputStream(destPath);
            GZIPOutputStream gzipOS = new GZIPOutputStream(fos);
            byte[] buffer = new byte[1024];

            int len;
            while ((len = fis.read(buffer)) != -1) {
                gzipOS.write(buffer, 0, len);
            }

            gzipOS.close();
            fos.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
