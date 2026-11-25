package com.quickbank.observability.lab;

public record SpanSpec(String name, String module, String whenEmitted, String attributes) {
}
