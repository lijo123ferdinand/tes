package com.fileupload.model;

import jakarta.persistence.*;

@Entity
@Table(name = "batch_bucket_mapping")
public class BatchBucketMapping {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "batch_id", unique = true)
	private Long batchId;

	@Column(name = "bucket_name")
	private String bucketName;

	public BatchBucketMapping() {
	}

	public BatchBucketMapping(Long batchId, String bucketName) {
		this.batchId = batchId;
		this.bucketName = bucketName;

	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getBatchId() {
		return batchId;
	}

	public void setBatchId(Long batchId) {
		this.batchId = batchId;
	}

	public String getBucketName() {
		return bucketName;
	}

	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

}
