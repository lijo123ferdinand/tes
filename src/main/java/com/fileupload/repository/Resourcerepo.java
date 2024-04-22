package com.fileupload.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fileupload.model.Resource;

public interface Resourcerepo extends JpaRepository<Resource, Long> {

}
