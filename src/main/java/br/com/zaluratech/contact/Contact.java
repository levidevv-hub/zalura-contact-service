package br.com.zaluratech.contact;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contacts", uniqueConstraints = @UniqueConstraint(name = "uq_contact_company_phone", columnNames = {"company_id", "phone"}))
public class Contact {
    @Id
    public UUID id;
    @Column(name = "company_id", nullable = false)
    public UUID companyId;
    @Column(nullable = false, length = 150)
    public String name;
    @Column(nullable = false, length = 20)
    public String phone;
    @Column(name = "marketing_consent", nullable = false)
    public boolean marketingConsent;
    @Column(name = "consent_at")
    public Instant consentAt;
    @Column(nullable = false)
    public boolean blocked;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    protected Contact() {
    }

    public Contact(UUID companyId, String name, String phone, boolean consent) {
        this.id = UUID.randomUUID();
        this.companyId = companyId;
        this.name = name;
        this.phone = phone;
        this.marketingConsent = consent;
        this.consentAt = consent ? Instant.now() : null;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }
}
