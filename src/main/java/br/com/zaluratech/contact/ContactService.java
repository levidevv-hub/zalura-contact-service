package br.com.zaluratech.contact;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

import static br.com.zaluratech.contact.ContactDtos.*;

@Service
public class ContactService {

    private final ContactRepository repo;

    public ContactService(ContactRepository repo) {
        this.repo = repo;
    }

    static String phone(String raw) {
        if (raw == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Telefone obrigatório");
        }

        String digits = raw.replaceAll("\\D", "");

        if (digits.length() == 13 && digits.startsWith("55")) {
            digits = digits.substring(2);
        }

        if (!digits.matches("[1-9][0-9]{10}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Telefone brasileiro inválido: use DDD + 9 dígitos");
        }

        return "55" + digits;
    }

    static String name(String raw) {
        if (raw == null || raw.isBlank() || raw.strip().length() > 150)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome inválido");
        return raw.strip();
    }

    @Transactional
    public Response create(UUID company, Request input) {

        String p = phone(input.phone());
        if (repo.findByCompanyIdAndPhone(company, p).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Contato duplicado");
        return Response.from(repo.saveAndFlush(new Contact(company, name(input.name()), p, input.marketingConsent())));
    }

    @Transactional(readOnly = true)
    public Page<Response> list(UUID company, Pageable page) {
        return repo.findByCompanyId(company, page).map(Response::from);
    }

    @Transactional(readOnly = true)
    public Response get(UUID company, UUID id) {
        return Response.from(existing(company, id));
    }

    private Contact existing(UUID company, UUID id) {
        return repo.findByCompanyIdAndId(company, id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contato não encontrado"));
    }

    @Transactional
    public Response update(UUID company, UUID id, Update input) {
        Contact c = existing(company, id);
        String p = phone(input.phone());
        repo.findByCompanyIdAndPhone(company, p).filter(other -> !other.id.equals(id)).ifPresent(other -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Contato duplicado");
        });
        c.name = name(input.name());
        c.phone = p;
        c.updatedAt = Instant.now();
        return Response.from(repo.saveAndFlush(c));
    }

    @Transactional
    public Response consent(UUID company, UUID id, boolean approved) {
        Contact c = existing(company, id);
        c.marketingConsent = approved;
        c.consentAt = approved ? Instant.now() : null;
        c.updatedAt = Instant.now();
        return Response.from(c);
    }

    @Transactional
    public Response block(UUID company, UUID id, boolean blocked) {
        Contact c = existing(company, id);
        c.blocked = blocked;
        c.updatedAt = Instant.now();
        return Response.from(c);
    }

    @Transactional
    public void delete(UUID company, UUID id) {
        repo.delete(existing(company, id));
    }

    @Transactional
    public BatchResult importBatch(UUID company, Batch request) {
        if (request.contatos().size() > 1000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Máximo 1000 contatos por lote");
        int created = 0, duplicates = 0, invalid = 0;
        List<String> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int line = 0;
        for (Imported item : request.contatos()) {
            line++;
            try {
                String p = phone(item.telefone()), n = name(item.nome());
                if (!seen.add(p) || repo.findByCompanyIdAndPhone(company, p).isPresent()) {
                    duplicates++;
                    continue;
                }
                // A importação NÃO prova consentimento para marketing.
                repo.save(new Contact(company, n, p, false));
                created++;
            } catch (ResponseStatusException e) {
                invalid++;
                errors.add("Item " + line + ": " + e.getReason());
            }
        }
        repo.flush();
        return new BatchResult(request.contatos().size(), created, duplicates, invalid, errors);
    }
}
