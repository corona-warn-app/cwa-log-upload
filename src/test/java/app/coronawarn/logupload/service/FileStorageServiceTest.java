package app.coronawarn.logupload.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.coronawarn.logupload.config.LogUploadConfig;
import app.coronawarn.logupload.config.LogUploadS3Config;
import app.coronawarn.logupload.model.LogEntity;
import app.coronawarn.logupload.repository.LogRepository;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {"els-verify.tls.enabled=false"})
public class FileStorageServiceTest {

    @Autowired
    FileStorageService service;

    @MockBean
    AmazonS3 amazonS3Mock;

    @MockBean
    LogRepository logRepositoryMock;

    @MockBean
    LogUploadConfig logUploadConfigMock;

    @MockBean
    LogUploadS3Config logUploadS3ConfigMock;

    private final static byte[] dummyBytes = new byte[]{0xD, 0xE, 0xA, 0xD, 0xB, 0xE, 0xE, 0xF};
    private final static String dummyFileName = "dummy.zip";
    private final static String dummyHash = "hash123456789";
    private final static String dummyBucket = "niceBucket";
    private final static String dummyLogId = "ABCDEFG";

    @BeforeEach
    void setup() {
        when(logUploadS3ConfigMock.getBucketName()).thenReturn(dummyBucket);
    }

    @Test
    public void testFileUploadToS3Bucket() throws FileStorageService.FileStoreException, IOException {
        InputStream stream = new ByteArrayInputStream(dummyBytes);

        ArgumentCaptor<String> logIdDbCaptor = ArgumentCaptor.forClass(String.class);

        given(logRepositoryMock.findById(logIdDbCaptor.capture()))
            .willAnswer(new Answer<Optional<LogEntity>>() {
                private byte counter = 0;

                @Override
                public Optional<LogEntity> answer(InvocationOnMock invocationOnMock) {
                    counter++;

                    if (counter <= 2) {
                        return Optional.of(new LogEntity(invocationOnMock.getArgument(0), null, null, 0, null, null));
                    } else {
                        return Optional.empty();
                    }
                }
            });

        ArgumentCaptor<String> bucketIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> logIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ObjectMetadata> metadataCaptor = ArgumentCaptor.forClass(ObjectMetadata.class);
        ArgumentCaptor<InputStream> inputStreamArgumentCaptor = ArgumentCaptor.forClass(InputStream.class);

        PutObjectResult putObjectResult = new PutObjectResult();
        putObjectResult.setContentMd5(dummyHash);

        given(amazonS3Mock.putObject(
            bucketIdCaptor.capture(),
            logIdCaptor.capture(),
            inputStreamArgumentCaptor.capture(),
            metadataCaptor.capture()))
            .willReturn(putObjectResult);

        // Mock behaviour of repository
        given(logRepositoryMock.save(any())).will(invocationOnMock -> invocationOnMock.getArgument(0));

        LogEntity returnValue = service.storeFileStream(dummyFileName, dummyBytes.length, stream);
        String expectedLogId = logIdDbCaptor.getAllValues().get(2);

        // check that uploaded file has unused id (the third from the beginning)
        assertEquals(expectedLogId, logIdCaptor.getValue());

        // check that correct stream was uploaded
        assertArrayEquals(dummyBytes, inputStreamArgumentCaptor.getValue().readAllBytes());

        // check that correct content length was set
        assertEquals(dummyBytes.length, metadataCaptor.getValue().getContentLength());

        // check that file is uploaded to correct bucket
        assertEquals(dummyBucket, bucketIdCaptor.getValue());

        assertEquals(expectedLogId, returnValue.getId());
        assertEquals(dummyFileName, returnValue.getFilename());
        assertEquals(dummyHash, returnValue.getHash());
        assertEquals(dummyBytes.length, returnValue.getSize());
    }

    @Test
    public void testFileUploadToS3BucketFailed() {
        InputStream stream = new ByteArrayInputStream(dummyBytes);

        ArgumentCaptor<String> logIdDbCaptor = ArgumentCaptor.forClass(String.class);

        given(logRepositoryMock.findById(logIdDbCaptor.capture())).willReturn(Optional.empty());
        given(logUploadS3ConfigMock.getBucketName()).willReturn(dummyBucket);


        given(amazonS3Mock.putObject(anyString(), anyString(), any(InputStream.class), any(ObjectMetadata.class)))
            .willThrow(new SdkClientException(""));

        // Nothing should be saved to database
        verify(logRepositoryMock, times(0)).save(any());

        FileStorageService.FileStoreException e =
            assertThrows(
                FileStorageService.FileStoreException.class,
                () -> service.storeFileStream(dummyFileName, dummyBytes.length, stream));

        assertEquals(FileStorageService.FileStoreException.Reason.S3_UPLOAD_FAILED, e.getReason());
    }

    @Test
    public void testFileDownload() throws FileStorageService.FileStoreException, IOException {
        InputStream stream = new ByteArrayInputStream(dummyBytes);

        given(logRepositoryMock.findById(eq(dummyLogId)))
            .willReturn(Optional.of(new LogEntity(dummyLogId, null, dummyFileName, dummyBytes.length, dummyHash, null)));

        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(stream);

        given(amazonS3Mock.getObject(eq(dummyBucket), eq(dummyLogId)))
            .willReturn(s3Object);

        FileStorageService.LogDownloadResponse returnValue = service.downloadFile(dummyLogId);

        // check that correct stream was downloaded
        assertArrayEquals(dummyBytes, returnValue.getInputStream().readAllBytes());

        assertEquals(dummyLogId, returnValue.getLogEntity().getId());
        assertEquals(dummyFileName, returnValue.getLogEntity().getFilename());
        assertEquals(dummyHash, returnValue.getLogEntity().getHash());
        assertEquals(dummyBytes.length, returnValue.getLogEntity().getSize());
    }

    @Test
    public void testFileDownloadFailsBecauseFileNotFoundInDb() {
        given(logRepositoryMock.findById(eq(dummyLogId)))
            .willReturn(Optional.empty());

        FileStorageService.FileStoreException e =
            assertThrows(FileStorageService.FileStoreException.class, () -> service.downloadFile(dummyLogId));

        assertEquals(FileStorageService.FileStoreException.Reason.FILE_NOT_FOUND, e.getReason());
    }

    @Test
    public void testFileDownloadFailsBecauseFileNotFoundInBucket() {
        given(logRepositoryMock.findById(eq(dummyLogId)))
            .willReturn(Optional.of(new LogEntity(dummyLogId, null, dummyFileName, dummyBytes.length, dummyHash, null)));

        given(amazonS3Mock.getObject(eq(dummyBucket), eq(dummyLogId)))
            .willThrow(new SdkClientException(""));

        FileStorageService.FileStoreException e =
            assertThrows(FileStorageService.FileStoreException.class, () -> service.downloadFile(dummyLogId));

        assertEquals(FileStorageService.FileStoreException.Reason.S3_DOWNLOAD_FAILED, e.getReason());
    }

    @Test
    public void testDeleteFile() {
        LogEntity logEntity = new LogEntity();
        logEntity.setId(dummyLogId);
        logEntity.setFilename(dummyFileName);
        logEntity.setHash(dummyHash);
        logEntity.setSize(dummyBytes.length);

        service.deleteFileSafe(logEntity);

        verify(amazonS3Mock).deleteObject(eq(dummyBucket), eq(dummyLogId));
        verify(logRepositoryMock).delete(eq(logEntity));
    }

    @Test
    public void testDeleteFileShouldNotThrowAnException() {
        LogEntity logEntity = new LogEntity();
        logEntity.setId(dummyLogId);
        logEntity.setFilename(dummyFileName);
        logEntity.setHash(dummyHash);
        logEntity.setSize(dummyBytes.length);

        service.deleteFileSafe(logEntity);

        doThrow(new SdkClientException(""))
            .when(amazonS3Mock).deleteObject(eq(dummyBucket), eq(dummyLogId));

        // It should delete database entity if s3 deleting fails.
        verify(logRepositoryMock).delete(eq(logEntity));
    }
}
