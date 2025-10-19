package com.instagram.backend.repository.mongo;

import com.instagram.backend.model.document.Reel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReelRepository extends MongoRepository<Reel, String> {
    Page<Reel> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<Reel> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("{'$or': ["
           + "{'caption': {$regex: ?0, $options: 'i'}}, "
           + "{'tags': {$in: ?1}}"
           + "]}")
    Page<Reel> findByCaptionContainingIgnoreCaseOrTagsIn(String query, List<String> tags, Pageable pageable);
}