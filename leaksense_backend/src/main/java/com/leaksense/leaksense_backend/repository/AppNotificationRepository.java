package com.leaksense.leaksense_backend.repository;

import com.leaksense.leaksense_backend.entity.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {

    long countByIsReadFalse();

    List<AppNotification> findTop50ByOrderByCreatedAtDesc();

    List<AppNotification> findByIsReadFalseOrderByCreatedAtDesc();

    @Modifying
    @Transactional
    @Query("UPDATE AppNotification n SET n.isRead = true WHERE n.isRead = false")
    int markAllAsRead();
}
