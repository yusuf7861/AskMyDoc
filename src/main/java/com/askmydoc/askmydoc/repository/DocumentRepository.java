package com.askmydoc.askmydoc.repository;

import com.askmydoc.askmydoc.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {}
