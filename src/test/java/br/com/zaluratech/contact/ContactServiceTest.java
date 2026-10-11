package br.com.zaluratech.contact;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class ContactServiceTest {
    @Test
    void normalizesPhone() {
        assertEquals(
                "5585999998888",
                ContactService.phone("(85) 99999-8888")
        );
    }

    @Test
    void rejectsInvalidPhone() {
        assertThrows(ResponseStatusException.class, () -> ContactService.phone("123"));
    }

    @Test
    void trimsName() {
        assertEquals("Ana", ContactService.name("  Ana  "));
    }

    @Test
    void normalizesPhoneWithAndWithoutCountryCode() {
        String withoutDdi = ContactService.phone("85999998888");
        String withDdi = ContactService.phone("5585999998888");

        assertEquals("5585999998888", withoutDdi);
        assertEquals(withoutDdi, withDdi);
    }
}
