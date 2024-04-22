package com.fileupload.servicetest;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.fileupload.exception.*;
import com.fileupload.model.*;
import com.fileupload.repository.*;
import com.fileupload.service.S3Service;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class S3ServiceTest {

	@InjectMocks
	private S3Service s3Service;

	@Mock
	private AmazonS3 amazonS3;

	@Mock
	private Resourcerepo resourceRepo;

	@Mock
	private LearningResourceRepo learningResourceRepo;

	@Mock
	private BatchBucketMappingRepository batchBucketMappingRepository;

	private static final long MAX_FILE_SIZE = 52428800;

	@Mock
	@PersistenceContext
	private EntityManager entityManager;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	void testUploadFile_Negative_InvalidFileFormat() throws RuntimeException {
		// Prepare mock data
		MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[100]); // Assuming
																											// invalid
																											// file
																											// format
		Long batchId = 1L;
		Long courseId = 1L;
		Long topicId = 1L;
		String link = null;
		String linkFileName = null;

		// Call the method and expect InvalidFileFormatException
		assertThrows(InvalidFileFormatException.class, () -> s3Service.uploadFiles(Collections.singletonList(file),
				batchId, courseId, topicId, Collections.singletonList(link)));
	}

	@Test
	void testListFiles_Positive() {
		// Prepare mock data
		Query query = mock(Query.class);
		when(entityManager.createQuery(anyString())).thenReturn(query);
		List<Object[]> resultList = new ArrayList<>();
		Object[] objArray = new Object[7];
		objArray[0] = 1L; // resourceId
		objArray[1] = "test.txt"; // filename
		objArray[2] = "pdf"; // filetype
		objArray[3] = "http://example.com"; // link
		objArray[4] = 1L; // batchId
		objArray[5] = 1L; // courseId
		objArray[6] = 1L; // topicId
		resultList.add(objArray);
		when(query.getResultList()).thenReturn(resultList);

		// Call the method and assert the result
		List<Map<String, Object>> result = s3Service.listFiles();
		assertFalse(result.isEmpty());
	}

	@Test
	void testListFiles_Negative_CreateQueryFailure() {
		// Prepare mock data
		when(entityManager.createQuery(anyString())).thenThrow(RuntimeException.class);

		// Call the method and expect an exception
		assertThrows(RuntimeException.class, () -> s3Service.listFiles());
	}

	@Test
	void testUserlistFiles_Positive() {
		// Prepare mock data
		LearningResource learningResource = new LearningResource();
		Resource resource = new Resource();
		resource.setLink("http://example.com");
		resource.setFileType("pdf");
		resource.setFileName("test.pdf");
		learningResource.setResources(Collections.singletonList(resource));
		when(learningResourceRepo.findByBatchIdAndCourseIdAndTopicId(anyLong(), anyLong(), anyLong()))
				.thenReturn(learningResource);

		// Call the method and assert the result
		List<Map<String, Object>> result = s3Service.userlistFiles(1L, 1L, 1L);
		assertFalse(result.isEmpty());
	}

	@Test
	void testUserlistFiles_Negative_NoLearningResourceFound() {
		// Prepare mock data
		when(learningResourceRepo.findByBatchIdAndCourseIdAndTopicId(anyLong(), anyLong(), anyLong())).thenReturn(null);

		// Call the method and assert the result
		List<Map<String, Object>> result = s3Service.userlistFiles(1L, 1L, 1L);
		assertTrue(result.isEmpty());
	}

	@Test
	void testDeleteResource_Negative_ResourceNotFound() {
		// Prepare mock data
		Long resourceId = 1L;
		Long batchId = 1L;

		when(batchBucketMappingRepository.findByBatchId(batchId)).thenReturn(new BatchBucketMapping()); // Mock the
																										// batch mapping
																										// to exist

		when(resourceRepo.findById(resourceId)).thenReturn(Optional.empty()); // Mock the resource to not exist

		// Call the method and expect ResourceNotFoundException
		assertThrows(ResourceNotFoundException.class, () -> s3Service.deleteResource(resourceId, batchId));
	}

	@Test
	void listFiles_Positive() {
		// Mocking data
		Object[] mockResult = { 1L, "file.pdf", "pdf", "https://example.com/file.pdf", 1L, 1L, 1L };
		List<Object[]> mockResults = Collections.singletonList(mockResult);
		TypedQuery<Object[]> mockQuery = mock(TypedQuery.class);
		when(mockQuery.getResultList()).thenReturn(mockResults);
		when(entityManager.createQuery(anyString())).thenReturn(mockQuery);

		// Performing the test
		List<Map<String, Object>> result = s3Service.listFiles();

		// Assertions
		assertFalse(result.isEmpty());
		assertEquals(1, result.size());
		Map<String, Object> resultMap = result.get(0);
		assertEquals(1L, resultMap.get("resourceId"));
		assertEquals("pdf", resultMap.get("filetype")); // Corrected typo here
		assertEquals("https://example.com/file.pdf", resultMap.get("source"));
		assertEquals("file.pdf", resultMap.get("fileName"));
		assertEquals(1L, resultMap.get("batchId"));
		assertEquals(1L, resultMap.get("courseId"));
		assertEquals(1L, resultMap.get("topicId"));
	}

	@Test
	void listFiles_EmptyResult() {
		// Mocking empty result list
		List<Object[]> mockResults = Collections.emptyList();
		TypedQuery<Object[]> mockQuery = mock(TypedQuery.class);
		when(mockQuery.getResultList()).thenReturn(mockResults);
		when(entityManager.createQuery(anyString())).thenReturn(mockQuery);

		// Performing the test
		List<Map<String, Object>> result = s3Service.listFiles();

		// Assertions
		assertTrue(result.isEmpty());
	}

	@Test
	void deleteResource_Success() throws BatchNotFoundException, ResourceNotFoundException {
		// Mocking data
		Long resourceId = 1L;
		Long batchId = 1L;
		String fileName = "example.pdf";
		String bucketName = "example-bucket";

		// Mocking the repository methods
		BatchBucketMapping mapping = new BatchBucketMapping();
		mapping.setBatchId(batchId);
		mapping.setBucketName(bucketName);
		when(batchBucketMappingRepository.findByBatchId(batchId)).thenReturn(mapping);

		Resource resource = new Resource();
		resource.setResourceId(resourceId);
		resource.setFileName(fileName);
		resource.setFileType("pdf");
		when(resourceRepo.findById(resourceId)).thenReturn(Optional.of(resource));

		// Performing the test
		s3Service.deleteResource(resourceId, batchId);

		// Verifying interactions
		verify(resourceRepo, times(1)).deleteById(resourceId);
		verify(amazonS3, times(1)).deleteObject(any(DeleteObjectRequest.class)); // Verify deletion of file object
		verify(amazonS3, times(1)).deleteBucket(eq(bucketName)); // Verify deletion of bucket
		verify(batchBucketMappingRepository, times(1)).delete(mapping);
	}

//    @Test
//    void deleteResource_ResourceNotFound() {
//        // Mocking data
//        Long resourceId = 1L;
//        Long batchId = 1L;
//
//        // Mocking the repository methods
//        when(resourceRepo.findById(resourceId)).thenReturn(Optional.empty());
//
//        // Performing the test and asserting the exception
//        assertThrows(ResourceNotFoundException.class, () -> s3Service.deleteResource(resourceId, batchId));
//    }

	@Test
	void deleteResource_ResourceNotFound() {
		// Mock resource data
		Long resourceId = 1L;
		Long batchId = 1L;

		// Mock repository behavior
		when(resourceRepo.findById(resourceId)).thenReturn(Optional.empty());

		// Call the method and expect ResourceNotFoundException
		assertThrows(ResourceNotFoundException.class, () -> s3Service.deleteResource(resourceId, batchId));
	}

	@Test
	void deleteResource_BatchNotFound() {
		// Mock resource data
		Long resourceId = 1L;
		Long batchId = 1L;

		// Mock repository behavior
		when(resourceRepo.findById(resourceId)).thenReturn(Optional.of(new Resource()));
		when(batchBucketMappingRepository.findByBatchId(batchId)).thenReturn(null);

		// Call the method and expect BatchNotFoundException
		assertThrows(BatchNotFoundException.class, () -> s3Service.deleteResource(resourceId, batchId));
	}

	@Test
	void getFileType_WithoutExtension() throws IOException {
		// Mocking a MultipartFile with an original filename without an extension
		String originalFilename = "example";
		MockMultipartFile file = new MockMultipartFile("file", originalFilename, "application/octet-stream",
				new byte[0]);

		// Performing the test
		String fileType = s3Service.getFileType(file);

		// Assertion
		assertEquals("", fileType);
	}

	@Test
	void userlistFiles_ResourceFound() {
		// Mocking data
		Long batchId = 1L;
		Long courseId = 1L;
		Long topicId = 1L;

		// Mocking the repository methods
		LearningResource learningResource = new LearningResource();
		Resource resource = new Resource();
		resource.setResourceId(1L);
		resource.setFileType("pdf");
		resource.setLink("https://example.com/pdf");
		resource.setFileName("example.pdf");
		learningResource.setResources(Collections.singletonList(resource));
		when(learningResourceRepo.findByBatchIdAndCourseIdAndTopicId(batchId, courseId, topicId))
				.thenReturn(learningResource);

		// Performing the test
		List<Map<String, Object>> result = s3Service.userlistFiles(batchId, courseId, topicId);

		// Assertions
		assertFalse(result.isEmpty());
		assertEquals(1, result.size());
		Map<String, Object> resultMap = result.get(0);
		assertEquals(1L, resultMap.get("resourceId"));
		assertEquals("pdf", resultMap.get("type"));
		assertEquals("https://example.com/pdf", resultMap.get("source"));
		assertEquals("example.pdf", resultMap.get("name"));
	}

	@Test
	void userlistFiles_ResourceNotFound() {
		// Mocking data
		Long batchId = 1L;
		Long courseId = 1L;
		Long topicId = 1L;

		// Mocking the repository methods to return null learningResource
		when(learningResourceRepo.findByBatchIdAndCourseIdAndTopicId(batchId, courseId, topicId)).thenReturn(null);

		// Performing the test
		List<Map<String, Object>> result = s3Service.userlistFiles(batchId, courseId, topicId);

		// Assertions
		assertTrue(result.isEmpty());
	}

	@Test
	void userlistFiles_EmptyResourceList() {
		// Mocking data
		Long batchId = 1L;
		Long courseId = 1L;
		Long topicId = 1L;

		// Mocking the repository methods to return learningResource with empty
		// resources list
		LearningResource learningResource = new LearningResource();
		when(learningResourceRepo.findByBatchIdAndCourseIdAndTopicId(batchId, courseId, topicId))
				.thenReturn(learningResource);

		// Performing the test
		List<Map<String, Object>> result = s3Service.userlistFiles(batchId, courseId, topicId);

		// Assertions
		assertTrue(result.isEmpty());
	}

	@Test
	void isValidFileType_ValidTypes() {
		assertTrue(S3Service.isValidFileType("pdf"));
		assertTrue(S3Service.isValidFileType("ppt"));
		assertTrue(S3Service.isValidFileType("pptx"));
	}

	@Test
	void isValidFileType_InvalidType() {
		assertFalse(S3Service.isValidFileType("txt"));
	}

	@Test
	void generateFileName() {
		// Mocking a MultipartFile
		MultipartFile file = mock(MultipartFile.class);
		when(file.getOriginalFilename()).thenReturn("example.pdf");

		// Performing the test
		String fileName = s3Service.generateFileName(file);

		// Assertion
		assertEquals("example.pdf", fileName);
	}

	@Test
	void getBucketForBatch_ValidMapping() {
		// Mocking data
		Long batchId = 1L;
		BatchBucketMapping mapping = new BatchBucketMapping();
		mapping.setBatchId(batchId);
		mapping.setBucketName("example-bucket");

		// Mocking the repository methods
		when(batchBucketMappingRepository.findByBatchId(batchId)).thenReturn(mapping);

		// Performing the test
		String bucketName = s3Service.getBucketForBatch(batchId);

		// Assertion
		assertEquals("example-bucket", bucketName);
	}

	@Test
	void getBucketForBatch_InvalidMapping() {
		// Mocking data
		Long batchId = 2L;

		// Mocking the repository methods to return null mapping
		when(batchBucketMappingRepository.findByBatchId(batchId)).thenReturn(null);

		// Performing the test and asserting the exception
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> s3Service.getBucketForBatch(batchId));
		assertEquals("No bucket mapped for batch: " + batchId, exception.getMessage());
	}

	@Test
	void getFileType_WithExtension() throws IOException {
		// Mocking a MultipartFile with an original filename containing an extension
		String originalFilename = "example.pdf";
		MockMultipartFile file = new MockMultipartFile("file", originalFilename, "application/pdf", new byte[0]);

		// Performing the test
		String fileType = s3Service.getFileType(file);

		// Assertion
		assertEquals("pdf", fileType);
	}

	@Test
	void createBucket_SuccessfullyCreatesBucket() {
// Mocking data
		Long batchId = 1L;
		String bucketName = "thisbatch-" + batchId;
		BatchBucketMapping mapping = new BatchBucketMapping();
		mapping.setBatchId(batchId);
		mapping.setBucketName(bucketName);

// Mocking repository save method
		when(batchBucketMappingRepository.save(any(BatchBucketMapping.class))).thenReturn(mapping);

// Performing the test
		s3Service.createBucket(batchId);

// Verifying that the bucket was created
		verify(amazonS3).createBucket(bucketName);

// Capturing the argument passed to save method
		ArgumentCaptor<BatchBucketMapping> captor = ArgumentCaptor.forClass(BatchBucketMapping.class);
		verify(batchBucketMappingRepository).save(captor.capture());

// Verifying properties of the captured object
		BatchBucketMapping capturedMapping = captor.getValue();
		assertEquals(batchId, capturedMapping.getBatchId());
		assertEquals(bucketName, capturedMapping.getBucketName());
	}

	@Test
	void doesFileExistInBucket_FileExists_ReturnsTrue() {
// Mock data
		String bucketName = "example-bucket";
		String fileKey = "example-file";

// Mock getObject to return a non-null S3Object
		when(amazonS3.getObject(bucketName, fileKey)).thenReturn(new S3Object());

// Perform the test
		boolean exists = s3Service.doesFileExistInBucket(bucketName, fileKey);

// Assertion
		assertTrue(exists);
	}

	@Test
	void doesFileExistInBucket_FileNotExists_ReturnsFalse() {
// Mock data
		String bucketName = "example-bucket";
		String fileKey = "example-file";

// Mock getObject to return null
		when(amazonS3.getObject(bucketName, fileKey)).thenReturn(null);

// Perform the test
		boolean exists = s3Service.doesFileExistInBucket(bucketName, fileKey);

// Assertion
		assertFalse(exists);
	}

	@Test
	void doesLinkExistForLearningResource_LinkExists_ReturnsTrue() {
// Mock data
		Long batchId = 1L;
		Long courseId = 1L;
		Long topicId = 1L;
		String link = "example.com";
		List<Resource> resources = new ArrayList<>();
		Resource mockResource = mock(Resource.class);
		when(mockResource.getLink()).thenReturn(link);

// Add the mockResource to the resources list
		resources.add(mockResource);

		LearningResource mockLearningResource = mock(LearningResource.class);
		when(learningResourceRepo.findByBatchIdAndCourseIdAndTopicId(batchId, courseId, topicId))
				.thenReturn(mockLearningResource);

// Mock the getResources method of the mockLearningResource
		when(mockLearningResource.getResources()).thenReturn(resources);
		boolean exists = s3Service.doesLinkExistForLearningResource(link, batchId, courseId, topicId);

// Assertion
		assertTrue(exists);
	}

	@Test
	void doesLinkExistForLearningResource_LinkNotExists_ReturnsFalse() {
// Mock data
		Long batchId = 1L;
		Long courseId = 1L;
		Long topicId = 1L;
		String link = "example.com";
		List<Resource> resources = new ArrayList<>();

// Mock learningResourceRepo findByBatchIdAndCourseIdAndTopicId to return a learning resource without the link
		LearningResource mockLearningResource = mock(LearningResource.class);
		when(learningResourceRepo.findByBatchIdAndCourseIdAndTopicId(batchId, courseId, topicId))
				.thenReturn(mockLearningResource);

// Mock the getResources method of the mockLearningResource
		when(mockLearningResource.getResources()).thenReturn(resources);

// Perform the test
		boolean exists = s3Service.doesLinkExistForLearningResource(link, batchId, courseId, topicId);

// Assertion
		assertFalse(exists);
	}

	@Test
	void checkAndCreateBucket_BucketNotExists_CreatesBucket() {
		// Mock data
		Long batchId = 1L;

		// Mock repository findByBatchId to return null
		when(batchBucketMappingRepository.findByBatchId(batchId)).thenReturn(null);

		// Perform the test
		s3Service.checkAndCreateBucket(batchId);

		// Verify that createBucket was called
		verify(amazonS3).createBucket("thisbatch-1");
	}

	@Test
	void checkAndCreateBucket_BucketExists_DoesNotCreateBucket() {
		// Mock data
		Long batchId = 1L;

		// Mock repository findByBatchId to return a mapping
		when(batchBucketMappingRepository.findByBatchId(batchId)).thenReturn(new BatchBucketMapping());

		// Perform the test
		s3Service.checkAndCreateBucket(batchId);

		// Verify that createBucket was not called
		verify(amazonS3, never()).createBucket(anyString());
	}

	@Test
	void uploadFile_WithLink_Success() throws IOException {
		// Create necessary mocks and setup
		List<MultipartFile> files = null;
		Long batchId = 1L;
		Long courseId = 2L;
		Long topicId = 3L;
		String link = "https://example.com/resource";
		String linkFileName = "Example Link";
		LearningResource learningResource = new LearningResource();
		learningResource.setResources(new ArrayList<>()); // Initialize resources list to avoid NullPointerException
		Resource savedResource = new Resource();
		savedResource.setLink(link);
		savedResource.setFileType("link");
		savedResource.setFileName(linkFileName);

		// Mock the behavior of
		// learningResourceRepo.findByBatchIdAndCourseIdAndTopicId() to return a valid
		// LearningResource object
		when(learningResourceRepo.findByBatchIdAndCourseIdAndTopicId(batchId, courseId, topicId))
				.thenReturn(learningResource);

		when(resourceRepo.save(any())).thenReturn(savedResource);

		// Call uploadFile with a link
		List<Resource> result = s3Service.uploadFiles(files, batchId, courseId, topicId,
				Collections.singletonList(link));

		// Verify that the correct methods are called
		verify(resourceRepo, times(1)).save(any());
		verify(learningResourceRepo, times(1)).save(any());

		// Assert the result
		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(savedResource, result.get(0));
	}

	@Test
	void uploadFiles_ResourceAlreadyExistsException() throws RuntimeException {
		// Create necessary mocks and setup
		List<MultipartFile> files = null;
		Long batchId = 1L;
		Long courseId = 2L;
		Long topicId = 3L;
		String link = "https://example.com/resource";
		String linkFileName = "Example Link";
		LearningResource learningResource = new LearningResource();
		Resource existingResource = new Resource();
		existingResource.setLink(link);
		existingResource.setFileName(linkFileName);
		learningResource.setResources(new ArrayList<>());
		learningResource.getResources().add(existingResource);

		when(learningResourceRepo.findByBatchIdAndCourseIdAndTopicId(batchId, courseId, topicId))
				.thenReturn(learningResource);

		// Call uploadFiles with a link that already exists
		assertThrows(ResourceAlreadyExistsException.class,
				() -> s3Service.uploadFiles(files, batchId, courseId, topicId, Collections.singletonList(link)));

		// Verify that the correct methods are called
		verify(learningResourceRepo, never()).save(any());
		verify(resourceRepo, never()).save(any());
	}

	@Test
	void uploadFiles_InvalidFileFormatException() {
		// Create necessary mocks and setup
		MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "test content".getBytes());
		Long batchId = 1L;
		Long courseId = 2L;
		Long topicId = 3L;
		List<MultipartFile> files = Collections.singletonList(file);
		List<String> links = null;
		List<String> linkFileNames = null;

		// Call uploadFiles with a file with an invalid format
		assertThrows(InvalidFileFormatException.class,
				() -> s3Service.uploadFiles(files, batchId, courseId, topicId, links));

		// Verify that no methods are called on resourceRepo and learningResourceRepo
		verify(resourceRepo, never()).save(any());
		verify(learningResourceRepo, never()).save(any());
	}

}
