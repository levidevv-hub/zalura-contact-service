package br.com.zaluratech.contact;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import static br.com.zaluratech.contact.ContactDtos.*;
@RestController
@RequestMapping("/api/contacts")
public class ContactController {
    private final ContactService service;
    public ContactController(ContactService service) {this.service=service;}
    // TODO SECURITY: X-Company-Id must ONLY be injected by an authenticated trusted gateway.
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Response create(@RequestHeader("X-Company-Id") UUID company,@Valid @RequestBody Request request) {return service.create(company,request);}
    @GetMapping
    public Page<Response> list(@RequestHeader("X-Company-Id") UUID company,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        if(page<0 || size<1 || size>100) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,"Paginação inválida");
        return service.list(company,PageRequest.of(page,size));
    }
    @GetMapping("/{id}")
    public Response get(@RequestHeader("X-Company-Id") UUID company,@PathVariable UUID id) {return service.get(company,id);}
    @PutMapping("/{id}")
    public Response update(@RequestHeader("X-Company-Id") UUID company,@PathVariable UUID id,@Valid @RequestBody Update request) {return service.update(company,id,request);}
    @PatchMapping("/{id}/consent")
    public Response consent(@RequestHeader("X-Company-Id") UUID company,@PathVariable UUID id,@RequestBody ConsentRequest input) {return service.consent(company,id,input.consent());}
    @PatchMapping("/{id}/block")
    public Response block(@RequestHeader("X-Company-Id") UUID company,@PathVariable UUID id,@RequestBody BlockRequest input) {return service.block(company,id,input.blocked());}
    public record BlockRequest(boolean blocked) {}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader("X-Company-Id") UUID company,@PathVariable UUID id) {service.delete(company,id);}
    @PostMapping("/import") public BatchResult importContacts(@RequestHeader("X-Company-Id") UUID company,@Valid @RequestBody Batch input) {return service.importBatch(company,input);}
}
