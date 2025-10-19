package com.instagram.backend.repository.mongo;

import com.instagram.backend.model.document.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends MongoRepository<Post, String> {
    
    @Query("{'$or': ["
           + "{'caption': {$regex: ?0, $options: 'i'}}, "
           + "{'tags': {$in: ?1}}"
           + "]}")
    Page<Post> findByCaptionContainingIgnoreCaseOrTagsIn(String query, List<String> tags, Pageable pageable);
    Page<Post> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<Post> findByUserIdInOrderByCreatedAtDesc(List<Long> userIds, Pageable pageable);
    Page<Post> findByTagsInOrderByCreatedAtDesc(List<String> tags, Pageable pageable);
}