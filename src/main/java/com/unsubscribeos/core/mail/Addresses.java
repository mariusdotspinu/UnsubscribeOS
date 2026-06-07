package com.unsubscribeos.core.mail;

/** Parses RFC 5322 {@code From} headers into a display name, address and sender domain. */
public final class Addresses {

    public record Sender(String name, String address, String domain) {}

    private Addresses() {}

    public static Sender parseFrom(String header) {
        if (header == null || header.isBlank()) return new Sender("Unknown", "", "unknown");
        String address = extractAddress(header).toLowerCase();
        String name = extractName(header, address);
        String domain = domainOf(address);
        return new Sender(name, address, domain);
    }

    private static String extractAddress(String header) {
        int open = header.indexOf('<');
        int close = header.indexOf('>');
        if (open >= 0 && close > open) return header.substring(open + 1, close).trim();
        return header.trim();
    }

    private static String extractName(String header, String address) {
        int open = header.indexOf('<');
        String name = open > 0 ? header.substring(0, open).trim() : "";
        name = name.replaceAll("^\"|\"$", "").trim();
        if (!name.isBlank()) return name;
        int at = address.indexOf('@');
        return at > 0 ? address.substring(0, at) : address;
    }

    private static String domainOf(String address) {
        int at = address.indexOf('@');
        String domain = at >= 0 && at < address.length() - 1 ? address.substring(at + 1) : "";
        return domain.isBlank() ? "unknown" : domain;
    }
}
