package uk.gov.hmcts.reform.sscs.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.document.DocumentDownloadClientApi;
import uk.gov.hmcts.reform.document.DocumentUploadClientApi;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.exception.UnsupportedDocumentTypeException;

@Service
public class EvidenceManagementService {

    public static final String DUMMY_OAUTH_2_TOKEN = "oauth2Token";

    private final AuthTokenGenerator authTokenGenerator;
    private final DocumentUploadClientApi documentUploadClientApi;
    private final DocumentDownloadClientApi documentDownloadClientApi;

    @Autowired
    public EvidenceManagementService(
        AuthTokenGenerator authTokenGenerator,
        DocumentUploadClientApi documentUploadClientApi,
        DocumentDownloadClientApi documentDownloadClientApi
    ) {
        this.authTokenGenerator = authTokenGenerator;
        this.documentUploadClientApi = documentUploadClientApi;
        this.documentDownloadClientApi = documentDownloadClientApi;
    }

    public UploadResponse upload(List<MultipartFile> files) {

        String serviceAuthorization = authTokenGenerator.generate();

        try {
            return documentUploadClientApi
                    .upload(DUMMY_OAUTH_2_TOKEN, serviceAuthorization, files);
        } catch (HttpClientErrorException httpClientErrorException) {
            throw new UnsupportedDocumentTypeException(httpClientErrorException);
        }
    }

    public byte[] download(String documentId) {
        String serviceAuthorization = authTokenGenerator.generate();

        try {
            ResponseEntity<Resource> responseEntity =  documentDownloadClientApi.downloadBinary(DUMMY_OAUTH_2_TOKEN, serviceAuthorization, documentId);

            ByteArrayResource resource = (ByteArrayResource) responseEntity.getBody();
            return resource.getByteArray();
        } catch (HttpClientErrorException httpClientErrorException) {
            throw new UnsupportedDocumentTypeException(httpClientErrorException);
        }
    }
}
