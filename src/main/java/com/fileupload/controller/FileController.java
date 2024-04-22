package com.fileupload.controller;

import com.fileupload.exception.BatchNotFoundException;
import com.fileupload.exception.ResourceNotFoundException;
import com.fileupload.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/resource")
public class FileController {

	private final S3Service s3Service;

	@Autowired
	public FileController(S3Service s3Service) {
		this.s3Service = s3Service;
	}

	@PostMapping("/{batchId}/{courseId}/{topicId}")
	public String uploadFile(@PathVariable Long batchId, @PathVariable Long courseId, @PathVariable Long topicId,
			@RequestParam(required = false) List<MultipartFile> files,
			@RequestParam(required = false) List<String> links) throws IOException {

		s3Service.uploadFiles(files, batchId, courseId, topicId, links);
		return "Files uploaded successfully";
	}

	@GetMapping("/resources")
	public List<Map<String, Object>> listAllFiles() {
		return s3Service.listFiles();
	}

	@DeleteMapping("/{resourceId}/batch/{batchId}")
	public String deleteResource(@PathVariable Long resourceId, @PathVariable Long batchId)
			throws BatchNotFoundException, ResourceNotFoundException {
		s3Service.deleteResource(resourceId, batchId);
		return "Resource Deleted Successfully";
	}

	@GetMapping("/batch/{batchId}/course/{courseId}/topic/{topicId}")
	public List<Map<String, Object>> userListFiles(@PathVariable Long batchId, @PathVariable Long courseId,
			@PathVariable Long topicId) {
		return s3Service.userlistFiles(batchId, courseId, topicId);
	}

}
