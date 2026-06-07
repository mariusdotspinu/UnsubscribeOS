package com.unsubscribeos.ui.view.dashboard;

import com.unsubscribeos.core.model.MailDomain;

import java.util.Comparator;

/** The orderings offered by the dashboard's sort control (Strategy pattern via comparators). */
enum DomainSort {

    MOST_EMAILS("Most emails",
            Comparator.comparingInt(MailDomain::count).reversed().thenComparing(MailDomain::domain)),
    NAME_ASC("Name A–Z",
            Comparator.comparing(MailDomain::domain, String.CASE_INSENSITIVE_ORDER)),
    NAME_DESC("Name Z–A",
            Comparator.comparing(MailDomain::domain, String.CASE_INSENSITIVE_ORDER).reversed());

    private final String label;
    private final Comparator<MailDomain> comparator;

    DomainSort(String label, Comparator<MailDomain> comparator) {
        this.label = label;
        this.comparator = comparator;
    }

    Comparator<MailDomain> comparator() {
        return comparator;
    }

    @Override
    public String toString() {
        return label;
    }
}
