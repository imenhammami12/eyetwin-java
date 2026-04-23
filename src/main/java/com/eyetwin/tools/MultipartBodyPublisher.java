package com.eyetwin.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class MultipartBodyPublisher {

    private MultipartBodyPublisher() {
    }

    public static HttpRequest.BodyPublisher build(File file, String uploadPreset, String mimeType, String boundary)
            throws IOException {

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        writeTextPart(output, boundary, "upload_preset", uploadPreset);
        writeFilePart(output, boundary, "file", file, mimeType);

        output.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        return HttpRequest.BodyPublishers.ofByteArray(output.toByteArray());
    }

    private static void writeTextPart(ByteArrayOutputStream output, String boundary,
                                      String fieldName, String value) throws IOException {
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + fieldName + "\"\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        output.write(value.getBytes(StandardCharsets.UTF_8));
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static void writeFilePart(ByteArrayOutputStream output, String boundary,
                                      String fieldName, File file, String mimeType) throws IOException {
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + fieldName
                + "\"; filename=\"" + file.getName() + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Type: " + mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(Files.readAllBytes(file.toPath()));
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }
}