package uk.gov.hmcts.reform.sscs.service.conversion;

import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WordDocumentConverter implements FileToPdfConverter {
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private final String endpoint;
    private final String accessKey;
    private final OkHttpClient httpClient;

    @Autowired
    public WordDocumentConverter(OkHttpClient httpClient,
                                 @Value("${docmosis.convert.endpoint}") String endpoint,
                                 @Value("${docmosis.accessKey}") String accessKey) {
        this.httpClient = httpClient;
        this.endpoint = endpoint;
        this.accessKey = accessKey;
    }

    @Override
    public List<String> accepts() {
        return Lists.newArrayList(
                "text/plain",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/x-tika-ooxml",
                "application/x-tika-msoffice"
        );
    }

    @Override
    public File convert(File file) throws IOException {
        log.info("Converting file...");
        final String convertedFileName = String.format("%s.pdf", FilenameUtils.getBaseName(file.getName()));
        log.info("convertedFileName:", convertedFileName);

        final String originalFileName = file.getName();
        log.info("originalFileName:", originalFileName);

        log.info("accessKey:", accessKey);
        log.info("outputName:", convertedFileName);

        MultipartBody requestBody = new MultipartBody
                .Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("accessKey", accessKey)
                .addFormDataPart("outputName", convertedFileName)
                .addFormDataPart("file", originalFileName, RequestBody.create(file, okhttp3.MediaType.get(PDF_CONTENT_TYPE)))
                .build();


        log.info("About to make request...");


        final Request request = new Request.Builder()
                .header("Accept", PDF_CONTENT_TYPE)
                .url(endpoint)
                .method("POST", requestBody)
                .build();

        log.info("About to receive response");

        final Response response = httpClient.newCall(request).execute();

        log.info("Response: " + response);


        final File convertedFile = File.createTempFile("stitch-conversion", ".pdf");

        log.info("convertedFile: " + convertedFile);


        Files.write(convertedFile.toPath(), Objects.requireNonNull(response.body()).bytes());

        log.info("About to return converted file");

        return convertedFile;
    }
}
