package br.com.zaluratech.contact.service;
import br.com.zaluratech.contact.dto.ContactEligibilityResponse;
import java.util.UUID;
public interface IContactEligibilityService { ContactEligibilityResponse get(UUID companyId, UUID contactId); }
