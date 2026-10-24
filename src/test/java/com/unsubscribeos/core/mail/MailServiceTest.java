package com.unsubscribeos.core.mail;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MailServiceTest {

    @Test
    void domainOfNamedAndBareAddresses() {
        assertEquals("shop.com", MailService.domainOf("\"Shop\" <Deals@Shop.com>"));
        assertEquals("shop.com", MailService.domainOf("deals@shop.com"));
    }

    @Test
    void domainOfUnknownWhenMissing() {
        assertEquals("unknown", MailService.domainOf(null));
        assertEquals("unknown", MailService.domainOf(""));
        assertEquals("unknown", MailService.domainOf("no-at-sign"));
        assertEquals("unknown", MailService.domainOf("user@"));
    }
}
