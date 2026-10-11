CREATE TABLE contacts (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    marketing_consent BOOLEAN NOT NULL DEFAULT FALSE,
    consent_at TIMESTAMPTZ,
    blocked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_contact_company_phone UNIQUE(company_id, phone)
);
CREATE INDEX idx_contacts_company ON contacts(company_id);
