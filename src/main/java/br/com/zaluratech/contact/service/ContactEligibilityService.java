package br.com.zaluratech.contact.service;
import br.com.zaluratech.contact.ContactService;
import br.com.zaluratech.contact.dto.ContactEligibilityResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;
@Service
public class ContactEligibilityService implements IContactEligibilityService {
    private final ContactService contacts;
    public ContactEligibilityService(ContactService contacts) { this.contacts = contacts; }
    @Override public ContactEligibilityResponse get(UUID companyId, UUID contactId) {
        var contact = contacts.get(companyId, contactId);
        return new ContactEligibilityResponse(contact.id(), contact.companyId(), contact.phone(), contact.marketingConsent(), contact.blocked());
    }
}
