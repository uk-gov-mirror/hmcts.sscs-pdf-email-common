package uk.gov.hmcts.reform.sscs.service;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.document.DocumentUploadClientApi;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.exception.UnsupportedDocumentTypeException;

@Service
@Slf4j
public class EvidenceManagementService {

    public static final String OAUTH_2_TOKEN = "oauth2Token";

    private final AuthTokenGenerator authTokenGenerator;
    private final DocumentUploadClientApi documentUploadClientApi;

    @Autowired
    public EvidenceManagementService(
            AuthTokenGenerator authTokenGenerator,
            DocumentUploadClientApi documentUploadClientApi
    ) {
        this.authTokenGenerator = authTokenGenerator;
        this.documentUploadClientApi = documentUploadClientApi;
    }

    public UploadResponse upload(List<MultipartFile> files) {

        String serviceAuthorization = authTokenGenerator.generate();

        try {
            return documentUploadClientApi.upload(OAUTH_2_TOKEN, serviceAuthorization, files);
        } catch (HttpClientErrorException httpClientErrorException) {
            log.error("Doc Store service failed to upload documents...", httpClientErrorException);
            if (null != files) {
                logFiles(files);
            }
            throw new UnsupportedDocumentTypeException(httpClientErrorException);
        }
    }

    private void logFiles(List<MultipartFile> files) {
        files.forEach(file -> {
            log.info("Name: {}", file.getName());
            log.info("OriginalName {}", file.getOriginalFilename());
        });
    }

}
