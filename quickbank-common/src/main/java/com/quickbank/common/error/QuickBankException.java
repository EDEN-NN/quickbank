package com.quickbank.common.error;

public class QuickBankException extends RuntimeException {

    private final String type;
    private final String title;
    private final int status;
    private final String typeSlug;

    public QuickBankException(String typeSlug, String title, int status, String detail) {
        super(detail);
        this.typeSlug = typeSlug;
        this.type = "https://quickbank.dev/problems/" + typeSlug;
        this.title = title;
        this.status = status;
    }

    public String type() {
        return type;
    }

    public String title() {
        return title;
    }

    public int status() {
        return status;
    }

    public String typeSlug() {
        return typeSlug;
    }
}
