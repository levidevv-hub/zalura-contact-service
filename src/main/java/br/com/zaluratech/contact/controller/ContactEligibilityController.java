package br.com.zaluratech.contact.controller;
import br.com.zaluratech.contact.service.IContactEligibilityService;
import br.com.zaluratech.contact.dto.ContactEligibilityResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class ContactEligibilityController {
    private final IContactEligibilityService service;
    public ContactEligibilityController(IContactEligibilityService service) { this.service = service; }
    @GetMapping("/internal/companies/{companyId}/contacts/{contactId}/eligibility")
    public ContactEligibilityResponse get(@PathVariable UUID companyId, @PathVariable UUID contactId) {
        return service.get(companyId, contactId);
    }
}
