package com.fileupload.model;

import jakarta.persistence.*;

@Entity
@Table(name = "resource")
public class Resource {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long resourceId;

	@Lob
	@Column(length = 1500)
	private String link;
	private String filetype;
	private String fileName;

	@ManyToOne
	@JoinColumn(name = "learning_resource_id")
	private LearningResource learningResource;

	public Resource() {
	}

	public Resource(Long resourceId, String link, String filetype, String fileName, LearningResource learningResource) {
		this.resourceId = resourceId;
		this.link = link;
		this.filetype = filetype;
		this.fileName = fileName;
		this.learningResource = learningResource;
	}

	public Long getResourceId() {
		return resourceId;
	}

	public void setResourceId(Long resourceId) {
		this.resourceId = resourceId;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getFileType() {
		return filetype;
	}

	public void setFileType(String filetype) {
		this.filetype = filetype;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public LearningResource getLearningResource() {
		return learningResource;
	}

	public void setLearningResource(LearningResource learningResource) {
		this.learningResource = learningResource;
	}

}
