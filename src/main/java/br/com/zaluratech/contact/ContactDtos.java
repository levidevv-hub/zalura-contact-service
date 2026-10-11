package br.com.zaluratech.contact;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
public final class ContactDtos {
    private ContactDtos() {}
    public record Request(@NotBlank String name, @NotBlank String phone, boolean marketingConsent) {}
    public record Update(@NotBlank String name, @NotBlank String phone) {}
    public record Imported(@NotBlank String nome, @NotBlank String telefone) {}
    public record Batch(@NotNull List<@Valid Imported> contatos) {}
    public record BatchResult(int received, int created, int duplicates, int invalid, List<String> errors) {}
    public record ConsentRequest(boolean consent) {}
    public record Response(UUID id, UUID companyId, String name,String phone,boolean marketingConsent,Instant consentAt,boolean blocked, Instant createdAt) {
        public static Response from(Contact c) {
            return new Response(c.id,c.companyId,c.name,c.phone,c.marketingConsent,c.consentAt,c.blocked,c.createdAt);
        }
    }
}
