package com.fileupload.modelTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fileupload.model.LearningResource;
import com.fileupload.model.Resource;

class ResourceTest {

	private Resource resource;
	private LearningResource learningResource;

	@BeforeEach
	public void setUp() {
		learningResource = new LearningResource();
		resource = new Resource(1L, "http://example.com", "pdf", "example.pdf", learningResource);
	}

	@Test
	void testGettersAndSetters() {
		assertEquals(Long.valueOf(1), resource.getResourceId());
		assertEquals("http://example.com", resource.getLink());
		assertEquals("pdf", resource.getFileType());
		assertEquals("example.pdf", resource.getFileName());
		assertEquals(learningResource, resource.getLearningResource());

		Long newResourceId = 2L;
		String newLink = "http://new-example.com";
		String newFileType = "ppt";
		String newFileName = "new-example.ppt";
		LearningResource newLearningResource = new LearningResource();

		resource.setResourceId(newResourceId);
		resource.setLink(newLink);
		resource.setFileType(newFileType);
		resource.setFileName(newFileName);
		resource.setLearningResource(newLearningResource);

		assertEquals(newResourceId, resource.getResourceId());
		assertEquals(newLink, resource.getLink());
		assertEquals(newFileType, resource.getFileType());
		assertEquals(newFileName, resource.getFileName());
		assertEquals(newLearningResource, resource.getLearningResource());
	}

	@Test
	void testNoArgsConstructor() {
		Resource emptyResource = new Resource();
		assertNull(emptyResource.getResourceId());
		assertNull(emptyResource.getLink());
		assertNull(emptyResource.getFileType());
		assertNull(emptyResource.getFileName());
		assertNull(emptyResource.getLearningResource());
	}

	@Test
	void testAllArgsConstructor() {
		Resource emptyResource = new Resource(3L, "http://another-example.com", "docx", "another-example.docx",
				new LearningResource());
		assertEquals(Long.valueOf(3), emptyResource.getResourceId());
		assertEquals("http://another-example.com", emptyResource.getLink());
		assertEquals("docx", emptyResource.getFileType());
		assertEquals("another-example.docx", emptyResource.getFileName());
		assertNotNull(emptyResource.getLearningResource());
	}

}
