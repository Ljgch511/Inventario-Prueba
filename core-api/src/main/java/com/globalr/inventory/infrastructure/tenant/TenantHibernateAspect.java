package com.globalr.inventory.infrastructure.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Automatically enables Hibernate's global countryFilter inside every @Transactional
 * application service invocation so multitenancy is enforced at ORM level (RNF-ARQ-003).
 */
@Aspect
@Component
public class TenantHibernateAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Before("@within(org.springframework.transaction.annotation.Transactional) || @annotation(org.springframework.transaction.annotation.Transactional)")
    public void applyCountryFilter() {
        UUID countryId = TenantContext.getCountryId();
        if (countryId != null) {
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("countryFilter").setParameter("countryId", countryId);
        }
    }
}
