package com.askmydoc.askmydoc.repository;

import com.askmydoc.askmydoc.model.PageChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface PageChunkRepository extends JpaRepository<PageChunk, Long> {
    List<PageChunk> findByDocumentIdIn(Collection<Long> ids);
}

