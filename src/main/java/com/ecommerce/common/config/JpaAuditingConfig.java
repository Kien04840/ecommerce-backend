package com.ecommerce.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Cấu hình kích hoạt cơ chế JPA Auditing cho toàn bộ hệ thống.
 * <p>
 * Cho phép Spring Data JPA tự động điền các trường thời gian
 * {@link org.springframework.data.annotation.CreatedDate} và
 * {@link org.springframework.data.annotation.LastModifiedDate}
 * trên các thực thể kế thừa {@link com.ecommerce.common.entity.BaseEntity}.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
