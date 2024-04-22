package com.fileupload.model;

import jakarta.persistence.*;
import jakarta.persistence.Entity;
import java.util.List;

import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "learningResource")
public class LearningResource {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long learningResourceId;

	@NotNull(message = "Batch Id cannot be null")
	@Column(name = "batchId")
	private Long batchId;

	@NotNull(message = "provide courseId")
	@Column(name = "courseId")
	private Long courseId;

	@NotNull(message = "provide topicId")
	@Column(name = "topicId")
	private Long topicId;

	@OneToMany(mappedBy = "learningResource", cascade = CascadeType.ALL)
	private List<Resource> resources;

	public LearningResource() {
	}

	public LearningResource(Long learningResourceId, Long batchId, Long courseId, Long topicId,
			List<Resource> resources) {
		this.learningResourceId = learningResourceId;
		this.batchId = batchId;
		this.courseId = courseId;
		this.topicId = topicId;
		this.resources = resources;
	}

	public Long getlearningResourceId() {
		return learningResourceId;
	}

	public void setlearningResourceId(Long learningResourceId) {
		this.learningResourceId = learningResourceId;
	}

	public Long getBatchId() {
		return batchId;
	}

	public void setBatchId(Long batchId) {
		this.batchId = batchId;
	}

	public Long getCourseId() {
		return courseId;
	}

	public void setCourseId(Long courseId) {
		this.courseId = courseId;
	}

	public Long getTopicId() {
		return topicId;
	}

	public void setTopicId(Long topicId) {
		this.topicId = topicId;
	}

	public List<Resource> getResources() {
		return resources;
	}

	public void setResources(List<Resource> resources) {
		this.resources = resources;
	}

}
