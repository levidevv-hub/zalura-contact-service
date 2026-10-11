package br.com.zaluratech.contact;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ContactRepository extends JpaRepository<Contact, UUID> {
    Optional<Contact> findByCompanyIdAndId(UUID companyId, UUID id);

    Optional<Contact> findByCompanyIdAndPhone(UUID companyId, String phone);

    Page<Contact> findByCompanyId(UUID companyId, Pageable pageable);
}
