package com.fileupload.modelTest;

import org.junit.jupiter.api.Test;

import com.fileupload.model.BatchBucketMapping;
import com.fileupload.model.LearningResource;

import static org.junit.jupiter.api.Assertions.*;

class BatchBucketMappingTest {

	@Test
	void testConstructor_Positive() {
		Long batchId = 1L;
		String bucketName = "test-bucket";

		BatchBucketMapping mapping = new BatchBucketMapping(batchId, bucketName);

		assertNotNull(mapping);
		assertEquals(null, mapping.getId()); // Id is auto-generated, expect null
		assertEquals(batchId, mapping.getBatchId());
		assertEquals(bucketName, mapping.getBucketName());
	}

	@Test
	void testSettersAndGetters_Positive() {
		BatchBucketMapping mapping = new BatchBucketMapping();

		Long id = 1L;
		Long batchId = 1L;
		String bucketName = "test-bucket";

		mapping.setId(id);
		mapping.setBatchId(batchId);
		mapping.setBucketName(bucketName);

		assertEquals(id, mapping.getId());
		assertEquals(batchId, mapping.getBatchId());
		assertEquals(bucketName, mapping.getBucketName());
	}

}