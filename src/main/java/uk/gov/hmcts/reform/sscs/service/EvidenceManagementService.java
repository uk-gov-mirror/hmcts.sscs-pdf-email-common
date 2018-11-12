package uk.gov.hmcts.reform.sscs.service;

import java.net.URI;
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
import uk.gov.hmcts.reform.document.DocumentMetadataDownloadClientApi;
import uk.gov.hmcts.reform.document.DocumentUploadClientApi;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.exception.UnsupportedDocumentTypeException;

@Service
public class EvidenceManagementService {

    public static final String S2S_TOKEN = "oauth2Token";

    private final AuthTokenGenerator authTokenGenerator;
    private final DocumentUploadClientApi documentUploadClientApi;
    private final DocumentDownloadClientApi documentDownloadClientApi;
    private final DocumentMetadataDownloadClientApi documentMetadataDownloadClient;

    @Autowired
    public EvidenceManagementService(
        AuthTokenGenerator authTokenGenerator,
        DocumentUploadClientApi documentUploadClientApi,
        DocumentDownloadClientApi documentDownloadClientApi,
        DocumentMetadataDownloadClientApi documentMetadataDownloadClient
    ) {
        this.authTokenGenerator = authTokenGenerator;
        this.documentUploadClientApi = documentUploadClientApi;
        this.documentDownloadClientApi = documentDownloadClientApi;
        this.documentMetadataDownloadClient = documentMetadataDownloadClient;
    }

    public UploadResponse upload(List<MultipartFile> files) {

        String serviceAuthorization = authTokenGenerator.generate();

        try {
            return documentUploadClientApi
                    .upload(S2S_TOKEN, serviceAuthorization, files);
        } catch (HttpClientErrorException httpClientErrorException) {
            throw new UnsupportedDocumentTypeException(httpClientErrorException);
        }
    }

    public byte[] download(URI documentSelf) {
        String serviceAuthorization = authTokenGenerator.generate();

        try {
            Document documentMetadata = documentMetadataDownloadClient.getDocumentMetadata(
                S2S_TOKEN,
                serviceAuthorization,
                documentSelf.getPath()
            );

            ResponseEntity<Resource> responseEntity =  documentDownloadClientApi.downloadBinary(
                S2S_TOKEN,
                serviceAuthorization,
                URI.create(documentMetadata.links.binary.href).getPath()
            );

            ByteArrayResource resource = (ByteArrayResource) responseEntity.getBody();
            return resource.getByteArray();
        } catch (HttpClientErrorException httpClientErrorException) {
            throw new UnsupportedDocumentTypeException(httpClientErrorException);
        }
    }
}
