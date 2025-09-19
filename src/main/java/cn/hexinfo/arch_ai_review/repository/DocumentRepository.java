package cn.hexinfo.arch_ai_review.repository;

import cn.hexinfo.arch_ai_review.entity.Document;
import cn.hexinfo.arch_ai_review.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    Page<Document> findByUser(User user, Pageable pageable);
    
    List<Document> findByUserOrderByCreatedAtDesc(User user);
    
    Optional<Document> findByIdAndUser(Long id, User user);
    
    Page<Document> findByStatus(Document.DocumentStatus status, Pageable pageable);
}
