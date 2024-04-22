package com.fileupload.modelTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fileupload.model.LearningResource;
import com.fileupload.model.Resource;

class LearningResourceTest {

	private LearningResource learningResource;
	private List<Resource> resources;

	@BeforeEach
	public void setUp() {
		resources = new ArrayList<>();
		resources.add(new Resource());
		resources.add(new Resource());
		learningResource = new LearningResource(1L, 101L, 201L, 301L, resources);
	}

	@Test
	void testGettersAndSetters() {
		assertEquals(Long.valueOf(1), learningResource.getlearningResourceId());
		assertEquals(Long.valueOf(101), learningResource.getBatchId());
		assertEquals(Long.valueOf(201), learningResource.getCourseId());
		assertEquals(Long.valueOf(301), learningResource.getTopicId());
		assertEquals(resources, learningResource.getResources());

		Long newLrid = 2L;
		Long newBatchId = 102L;
		Long newCourseId = 202L;
		Long newTopicId = 302L;
		List<Resource> newResources = new ArrayList<>();
		LearningResource newLearningResource = new LearningResource();

		newLearningResource.setlearningResourceId(newLrid);
		newLearningResource.setBatchId(newBatchId);
		newLearningResource.setCourseId(newCourseId);
		newLearningResource.setTopicId(newTopicId);
		newLearningResource.setResources(newResources);

		assertEquals(newLrid, newLearningResource.getlearningResourceId());
		assertEquals(newBatchId, newLearningResource.getBatchId());
		assertEquals(newCourseId, newLearningResource.getCourseId());
		assertEquals(newTopicId, newLearningResource.getTopicId());
		assertEquals(newResources, newLearningResource.getResources());
	}

	@Test
	void testNoArgsConstructor() {
		LearningResource emptyLearningResource = new LearningResource();
		assertNull(emptyLearningResource.getlearningResourceId());
		assertNull(emptyLearningResource.getBatchId());
		assertNull(emptyLearningResource.getCourseId());
		assertNull(emptyLearningResource.getTopicId());
		assertNull(emptyLearningResource.getResources());
	}

	@Test
	void testAllArgsConstructor() {
		LearningResource emptyLearningResource = new LearningResource(2L, 102L, 202L, 302L, new ArrayList<>());
		assertEquals(Long.valueOf(2), emptyLearningResource.getlearningResourceId());
		assertEquals(Long.valueOf(102), emptyLearningResource.getBatchId());
		assertEquals(Long.valueOf(202), emptyLearningResource.getCourseId());
		assertEquals(Long.valueOf(302), emptyLearningResource.getTopicId());
		assertNotNull(emptyLearningResource.getResources());
	}

}
