package com.fileupload.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fileupload.model.BatchBucketMapping;

public interface BatchBucketMappingRepository extends JpaRepository<BatchBucketMapping, Long> {
	BatchBucketMapping findByBatchId(Long batchId);

	void deleteByBatchId(Long batchId);
}
