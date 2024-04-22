package com.fileupload.service;

import com.fileupload.exception.*;
import com.fileupload.model.*;
import com.fileupload.repository.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class S3Service {

	private final AmazonS3 amazonS3;
	private final Resourcerepo resourceRepo;
	private final LearningResourceRepo learningResourceRepo;
	private final BatchBucketMappingRepository batchBucketMappingRepository;
	private final EntityManager entityManager;

	public S3Service(AmazonS3 amazonS3, Resourcerepo resourceRepo, LearningResourceRepo learningResourceRepo,
			BatchBucketMappingRepository batchBucketMappingRepository, EntityManager entityManager) {
		this.amazonS3 = amazonS3;
		this.resourceRepo = resourceRepo;
		this.learningResourceRepo = learningResourceRepo;
		this.batchBucketMappingRepository = batchBucketMappingRepository;
		this.entityManager = entityManager;
	}

	public List<Resource> uploadFiles(List<MultipartFile> files, Long batchId, Long courseId, Long topicId,
			List<String> links) throws IOException {
		List<Resource> savedResources = new ArrayList<>();

		// Loop through the maximum of files and links size
		int maxSize = Math.max(files != null ? files.size() : 0, links != null ? links.size() : 0);
		for (int i = 0; i < maxSize; i++) {
			MultipartFile file = null;
			String link = null;

			// Get the file, link, and link filename if they exist at the current index
			if (files != null && i < files.size()) {
				file = files.get(i);
			}
			if (links != null && i < links.size()) {
				link = links.get(i);
			}

			try {
				Resource savedResource = null;
				Resource resource = new Resource();
				LearningResource learningResource = learningResourceRepo.findByBatchIdAndCourseIdAndTopicId(batchId,
						courseId, topicId);

				// If a file is present, process it
				if (file != null) {
					checkAndCreateBucket(batchId);

					String fileType = getFileType(file);
					if (!isValidFileType(fileType)) {
						throw new InvalidFileFormatException("Invalid file format. Only PDF or PPT files are allowed.");
					}

					String fileName = generateFileName(file);
					String bucketName = getBucketForBatch(batchId);
					String fileKey = fileName;
					boolean fileExistsInBucket = doesFileExistInBucket(bucketName, fileKey);

					if (learningResource != null) {
						for (Resource existingResource : learningResource.getResources()) {
							// Check if existingResource or its fileName is null before comparing
							if (existingResource != null && existingResource.getFileName() != null
									&& existingResource.getFileName().equals(fileName)) {
								throw new ResourceAlreadyExistsException(
										"Resource already exists for the same batch, course, and topic.");
							}
						}
					}

					if (!fileExistsInBucket) {
						ObjectMetadata metadata = new ObjectMetadata();
						metadata.setContentType(file.getContentType());
						metadata.setContentLength(file.getSize());
						amazonS3.putObject(bucketName, fileKey, file.getInputStream(), metadata);
					}

					String directLink = getObjectDirectLink(bucketName, fileKey);
					resource.setLink(directLink);
					resource.setFileType(fileType);
					resource.setFileName(fileName);

					savedResource = resourceRepo.save(resource);
				}
				// If only a link is present, process it
				else if (link != null) {

					if (doesLinkExistForLearningResource(link, batchId, courseId, topicId)) {
						throw new ResourceAlreadyExistsException(
								"Resource already exists for the same batch, course, and topic.");
					}

					savedResource = new Resource();
					savedResource.setLink(link);
					savedResource.setFileType("link");

					// Since there's no file, we won't have a filename, so set it to null or an
					// appropriate default value
					savedResource.setFileName(null); // Or you can set it to some default value if needed

					savedResource = resourceRepo.save(savedResource);
				}

				// Save the resource to learningResource if it's not null
				if (learningResource == null) {
					learningResource = new LearningResource();
					learningResource.setBatchId(batchId);
					learningResource.setCourseId(courseId);
					learningResource.setTopicId(topicId);
					learningResource.setResources(new ArrayList<>());
					learningResource = learningResourceRepo.save(learningResource);
				}

				// Add the saved resource to learningResource and save it
				if (savedResource != null) {
					learningResource.getResources().add(savedResource);
					savedResource.setLearningResource(learningResource);
					learningResourceRepo.save(learningResource);
				}

				savedResources.add(savedResource);
			} catch (InvalidFileFormatException ex) {
				throw new InvalidFileFormatException("Invalid file format. Only PDF or PPT files are allowed.");
			}

		}
		return savedResources;

	}

	public static boolean isValidFileType(String fileType) {
		return "pdf".equalsIgnoreCase(fileType) || "ppt".equalsIgnoreCase(fileType)
				|| "pptx".equalsIgnoreCase(fileType);
	}

	public String generateFileName(MultipartFile file) {
		return file.getOriginalFilename();
	}

	public String getBucketForBatch(Long batchId) {
		BatchBucketMapping mapping = batchBucketMappingRepository.findByBatchId(batchId);
		if (mapping == null) {
			throw new IllegalArgumentException("No bucket mapped for batch: " + batchId);
		}
		return mapping.getBucketName();
	}

	public List<Map<String, Object>> listFiles() {
		String query = "SELECT DISTINCT r.resourceId, r.fileName, r.filetype, r.link, lr.batchId, lr.courseId, lr.topicId "
				+ "FROM Resource r " + "JOIN r.learningResource lr";
		Query nativeQuery = entityManager.createQuery(query);

		List<Object[]> results = nativeQuery.getResultList();
		List<Map<String, Object>> resourceLinks = new ArrayList<>();
		for (Object[] result : results) {
			Map<String, Object> resourceMap = new HashMap<>();
			Long resourceId = (Long) result[0];

			String filetype = (String) result[2];
			String fileLink = (String) result[3];
			Long batchId = (Long) result[4];
			Long courseId = (Long) result[5];
			Long topicId = (Long) result[6];

			if ("link".equalsIgnoreCase(filetype)) {
				resourceMap.put("resourceId", resourceId);
				resourceMap.put("filetype", filetype);
				resourceMap.put("source", fileLink);
				resourceMap.put("batchId", batchId);
				resourceMap.put("courseId", courseId);
				resourceMap.put("topicId", topicId);
			} else {
				String filename = (String) result[1];
				resourceMap.put("resourceId", resourceId);
				resourceMap.put("filetype", filetype);
				resourceMap.put("source", fileLink);
				resourceMap.put("fileName", filename);
				resourceMap.put("batchId", batchId);
				resourceMap.put("courseId", courseId);
				resourceMap.put("topicId", topicId);
			}

			resourceLinks.add(resourceMap);
		}

		return resourceLinks;
	}

	public String getFileType(MultipartFile file) throws NullPointerException {
		String originalFilename = file.getOriginalFilename();
		String[] parts = originalFilename.split("\\.");
		if (parts.length > 1) {
			return parts[parts.length - 1];
		} else {
			return ""; // Return empty string if no extension found
		}
	}

	public List<Map<String, Object>> userlistFiles(Long batchId, Long courseId, Long topicId) {
		LearningResource learningResource = learningResourceRepo.findByBatchIdAndCourseIdAndTopicId(batchId, courseId,
				topicId);
		if (learningResource == null) {
			return Collections.emptyList();
		}

		List<Resource> resources = learningResource.getResources();
		if (resources == null || resources.isEmpty()) {
			return Collections.emptyList();
		}

		List<Map<String, Object>> resourceMaps = new ArrayList<>();

		for (Resource resource : resources) {
			Map<String, Object> resourceMap = new HashMap<>();
			String resourceLink = resource.getLink();
			String fileType = resource.getFileType();

			resourceMap.put("resourceId", resource.getResourceId());
			resourceMap.put("type", fileType);
			resourceMap.put("source", resourceLink);

			if (!"link".equalsIgnoreCase(fileType)) {
				resourceMap.put("name", resource.getFileName());
			}

			resourceMaps.add(resourceMap);
		}

		return resourceMaps;
	}

	public void deleteResource(Long resourceId, Long batchId) throws BatchNotFoundException, ResourceNotFoundException {

		Optional<Resource> resourceOptional = resourceRepo.findById(resourceId);

		if (resourceOptional.isPresent()) {
			Resource resource = resourceOptional.get();
			String fileType = resource.getFileType();

			// Delete the resource entry from the database
			resourceRepo.deleteById(resourceId);

			// Check if the resource was a link
			if ("link".equalsIgnoreCase(fileType)) {
				// If it's a link, handle learning resource deletion and return
				handleLinkDeletion(resource, batchId);
				return;
			}

			// Handle file deletion
			handleFileDeletion(resource, batchId);
		} else {
			throw new ResourceNotFoundException("No resource found with ID: " + resourceId);
		}
	}

	private void handleLinkDeletion(Resource resource, Long batchId) throws BatchNotFoundException {
		LearningResource learningResource = resource.getLearningResource();
		if (learningResource != null) {
			// If no remaining resources, delete the learning resource entry
			List<Resource> remainingResources = learningResource.getResources();
			if (remainingResources.isEmpty()) {
				learningResourceRepo.deleteById(learningResource.getlearningResourceId());
				deleteBatchBucketMappingAndBucket(batchId);
			}
		}
	}

	private void handleFileDeletion(Resource resource, Long batchId) throws BatchNotFoundException {
		String fileKey = resource.getFileName();
		BatchBucketMapping mapping = batchBucketMappingRepository.findByBatchId(batchId);
		if (mapping == null) {
			throw new BatchNotFoundException("No batch found with ID: " + batchId);
		}
		String bucketName = mapping.getBucketName();

		// Delete the file from S3 bucket
		amazonS3.deleteObject(new DeleteObjectRequest(bucketName, fileKey));

		// Check if the resource has an associated learning resource
		LearningResource learningResource = resource.getLearningResource();
		if (learningResource != null) {
			// If no remaining resources, delete the learning resource entry
			List<Resource> remainingResources = learningResource.getResources();
			if (remainingResources.isEmpty()) {
				learningResourceRepo.deleteById(learningResource.getlearningResourceId());
				deleteBatchBucketMappingAndBucket(batchId);
			}
		} else {
			// If there's no associated learning resource, delete the bucket
			deleteBatchBucketMappingAndBucket(batchId);
		}
	}

	private void deleteBatchBucketMappingAndBucket(Long batchId) throws BatchNotFoundException {
		BatchBucketMapping mapping = batchBucketMappingRepository.findByBatchId(batchId);
		if (mapping != null) {
			String bucketName = mapping.getBucketName();
			batchBucketMappingRepository.delete(mapping); // Delete the mapping entry
			amazonS3.deleteBucket(bucketName); // Delete the bucket
		} else {
			throw new BatchNotFoundException("No batch found with ID: " + batchId);
		}
	}

	public void createBucket(Long batchId) {
		String bucketName = "thisbatch-" + batchId;
		amazonS3.createBucket(bucketName);

		// Disable Block Public Access settings
		PublicAccessBlockConfiguration blockConfiguration = new PublicAccessBlockConfiguration()
				.withBlockPublicAcls(false).withIgnorePublicAcls(false).withBlockPublicPolicy(false)
				.withRestrictPublicBuckets(false);
		SetPublicAccessBlockRequest blockRequest = new SetPublicAccessBlockRequest().withBucketName(bucketName)
				.withPublicAccessBlockConfiguration(blockConfiguration);
		amazonS3.setPublicAccessBlock(blockRequest);

		// Set the bucket policy
		String bucketPolicy = "{\n" + "    \"Version\": \"2012-10-17\",\n" + "    \"Statement\": [\n" + "        {\n"
				+ "            \"Effect\": \"Allow\",\n" + "            \"Principal\": \"*\",\n"
				+ "            \"Action\": [\n" + "                \"s3:GetObject\"\n" + "            ],\n"
				+ "            \"Resource\": [\n" + "                \"arn:aws:s3:::" + bucketName + "/*\"\n"
				+ "            ]\n" + "        }\n" + "    ]\n" + "}";

		amazonS3.setBucketPolicy(bucketName, bucketPolicy);

		BatchBucketMapping mapping = new BatchBucketMapping();
		mapping.setBatchId(batchId);
		mapping.setBucketName(bucketName);
		batchBucketMappingRepository.save(mapping);
	}

	public void checkAndCreateBucket(Long batchId) {
		if (batchBucketMappingRepository.findByBatchId(batchId) == null) {
			createBucket(batchId);
		}
	}

	public boolean doesFileExistInBucket(String bucketName, String fileKey) {
		try {
			S3Object s3Object = amazonS3.getObject(bucketName, fileKey);
			return s3Object != null;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean doesLinkExistForLearningResource(String link, Long batchId, Long courseId, Long topicId) {
		LearningResource learningResource = learningResourceRepo.findByBatchIdAndCourseIdAndTopicId(batchId, courseId,
				topicId);
		if (learningResource != null) {
			return learningResource.getResources().stream().anyMatch(resource -> link.equals(resource.getLink()));
		}
		return false;
	}

	public String getObjectDirectLink(String bucketName, String fileName) {
		return "https://" + bucketName + ".s3.amazonaws.com/" + fileName;
	}
}