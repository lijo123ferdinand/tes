package com.fileupload.controllerTest;

import com.fileupload.controller.FileController;
import com.fileupload.exception.BatchNotFoundException;
import com.fileupload.exception.ResourceNotFoundException;
import com.fileupload.model.Resource;
import com.fileupload.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileControllerTest {

	@InjectMocks
	private FileController fileController;

	@Mock
	private S3Service s3Service;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	void testUploadFile_Positive() throws IOException {
		// Mocking the behavior of S3Service to return a list of saved resources
		when(s3Service.uploadFiles(anyList(), anyLong(), anyLong(), anyLong(), anyList()))
				.thenReturn(Collections.singletonList(new Resource()));

		// Creating a mock multipart file
		MockMultipartFile multipartFile = new MockMultipartFile("file", "test.txt", "text/plain",
				"Hello, World!".getBytes());

		// Calling the uploadFile method
		String result = fileController.uploadFile(1L, 1L, 1L, Collections.singletonList(multipartFile), null);

		// Verifying the result
		assertEquals("Files uploaded successfully", result);
	}

	@Test
	void testUploadFile_Negative_FileUploadFailure() throws IOException {
		// Mocking the behavior of S3Service to throw an IOException when uploading
		// files
		when(s3Service.uploadFiles(anyList(), anyLong(), anyLong(), anyLong(), anyList())).thenThrow(IOException.class);

		// Call the uploadFile method
		String result = fileController.uploadFile(1L, 1L, 1L, Collections.emptyList(), null);

		// Verify that the result is not equal to "File upload failed"
		assertNotEquals("File upload failed", result);
	}

	@Test
	void testListAllFiles_Positive() {
		when(s3Service.listFiles()).thenReturn(Collections.emptyList());

		List<Map<String, Object>> result = fileController.listAllFiles();

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void testListAllFiles_Negative_FileListingFailure() {
		// Mock S3Service to throw an exception when listing files
		when(s3Service.listFiles()).thenThrow(RuntimeException.class); // or any other appropriate exception

		// Call the listAllFiles method and expect an exception
		assertThrows(RuntimeException.class, new org.junit.jupiter.api.function.Executable() {
			@Override
			public void execute() throws Throwable {
				fileController.listAllFiles();
			}
		});
	}

	@Test
	void testUserListFiles_Positive() {
		when(s3Service.userlistFiles(anyLong(), anyLong(), anyLong())).thenReturn(Collections.emptyList());

		List<Map<String, Object>> result = fileController.userListFiles(1L, 1L, 1L);

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void testUserListFiles_Negative_NoFiles() {
		when(s3Service.userlistFiles(anyLong(), anyLong(), anyLong())).thenReturn(Collections.emptyList());

		List<Map<String, Object>> result = fileController.userListFiles(1L, 1L, 1L);

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void testDeleteResource_Success() throws BatchNotFoundException, ResourceNotFoundException {
		Long resourceId = 1L;
		Long batchId = 1L;

		// Mock successful deletion
		doNothing().when(s3Service).deleteResource(resourceId, batchId);

		// Call the deleteResource method
		String result = fileController.deleteResource(resourceId, batchId);

		// Verify the interaction
		verify(s3Service).deleteResource(resourceId, batchId);

		// Assertions
		assertEquals("Resource Deleted Successfully", result);
	}

	@Test
	void testDeleteResource_BatchNotFoundException() throws BatchNotFoundException, ResourceNotFoundException {
		Long resourceId = 1L;
		Long batchId = 1L;

		// Mock exception
		doThrow(BatchNotFoundException.class).when(s3Service).deleteResource(resourceId, batchId);

		// Call the deleteResource method and expect an exception
		assertThrows(BatchNotFoundException.class, new org.junit.jupiter.api.function.Executable() {
			@Override
			public void execute() throws Throwable {
				fileController.deleteResource(resourceId, batchId);
			}
		});
	}

	@Test
	void testDeleteResource_ResourceNotFoundException() throws BatchNotFoundException, ResourceNotFoundException {
		Long resourceId = 1L;
		Long batchId = 1L;

		// Mock exception
		doThrow(ResourceNotFoundException.class).when(s3Service).deleteResource(resourceId, batchId);

		// Call the deleteResource method and expect an exception
		assertThrows(ResourceNotFoundException.class, new org.junit.jupiter.api.function.Executable() {
			@Override
			public void execute() throws Throwable {
				fileController.deleteResource(resourceId, batchId);
			}
		});
	}
}
