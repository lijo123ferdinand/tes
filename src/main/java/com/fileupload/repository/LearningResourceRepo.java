package com.fileupload.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fileupload.model.LearningResource;

public interface LearningResourceRepo extends JpaRepository<LearningResource, Long> {

	LearningResource findByBatchIdAndCourseIdAndTopicId(Long batchId, Long courseId, Long topicId);

	boolean existsByBatchIdAndCourseIdAndTopicId(Long batchId, Long courseId, Long topicId);

}
