package org.example.Translator;

import java.util.HashMap;
import java.util.Map;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class IriMapper {
    private final Map<String, String> iriToSymbol = new HashMap<>();
    private final Map<String, String> symbolToIri = new HashMap<>();
    private int counter = 0;

    public String getSymbol(String iri) {
        return iriToSymbol.computeIfAbsent(iri, this::createSymbol);
    }

    private String createSymbol(String iri) {
        String base = extractLocalName(iri)
                .replaceAll("[^A-Za-z0-9_]", "_");

        if (base.isBlank() || !Character.isLetter(base.charAt(0))) {
            base = "iri";
        }
        
        if (!base.isEmpty()) {
        base = Character.toLowerCase(base.charAt(0))
                + base.substring(1);
    }

        String symbol = base;
        while (symbolToIri.containsKey(symbol)) {
            symbol = base + "_" + (++counter);
        }

        symbolToIri.put(symbol, iri);
        return symbol;
    }

    private String extractLocalName(String iri) {
        int hash = iri.lastIndexOf('#');
        int slash = iri.lastIndexOf('/');
        int idx = Math.max(hash, slash);
        return idx >= 0 ? iri.substring(idx + 1) : iri;
    }

    public String lookup(String iri) {
        return iriToSymbol.get(iri);
    }

    public void load(String filePath) throws IOException {
        Properties properties = new Properties();

        try (FileInputStream input = new FileInputStream(filePath)) {
            properties.load(input);
        }

        iriToSymbol.clear();
        symbolToIri.clear();

        for (String iri : properties.stringPropertyNames()) {
            String symbol = properties.getProperty(iri);

            iriToSymbol.put(iri, symbol);
            symbolToIri.put(symbol, iri);
        }
    }

    public void save(String filePath) throws IOException {
        Properties properties = new Properties();

        for (Map.Entry<String, String> entry : iriToSymbol.entrySet()) {
            properties.setProperty(entry.getKey(), entry.getValue());
        }

        try (FileOutputStream output = new FileOutputStream(filePath)) {
            properties.store(output, "IRI to Datalog symbol mapping");
        }
    }

}