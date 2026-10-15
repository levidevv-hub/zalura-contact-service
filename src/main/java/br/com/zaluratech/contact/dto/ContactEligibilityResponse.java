package br.com.zaluratech.contact.dto;
import java.util.UUID;
public record ContactEligibilityResponse(UUID id, UUID companyId, String phone, boolean marketingConsent, boolean blocked) {}
