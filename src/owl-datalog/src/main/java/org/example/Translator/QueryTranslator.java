package org.example.Translator;

import org.example.model.*;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class QueryTranslator {

    private static final String RDF_TYPE =
            "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    private final IriMapper iriMapper = new IriMapper();

    public void translate(String sparqlQuery, Program program, String mappingPath) {

        try {
                iriMapper.load(mappingPath);
        } catch (IOException e) {
                System.out.println("Mapping file not found. A new one will be created at " + mappingPath);
        }

        Query query = QueryFactory.create(sparqlQuery);

        List<String> projectedVars;

        if (query.isQueryResultStar()) {
            projectedVars = collectVariables(query.getQueryPattern())
                    .stream()
                    .toList();
        } else {
            projectedVars = query.getResultVars();
        }

        List<String> headTerms = projectedVars.stream()
                .map(this::toDatalogVar)
                .toList();

        Atom head = new Atom("query_result", headTerms);

        List<List<Atom>> alternatives =
                translateElement(query.getQueryPattern(), iriMapper);

        for (List<Atom> body : alternatives) {
            program.addRule(new Rule(head, body));
        }

        try {
            iriMapper.save(mappingPath);
        } catch (Exception e) {
            System.err.println("Errore durante il salvataggio della mappatura: " + e.getMessage());
        }
    }

    private List<List<Atom>> translateElement(Element element, IriMapper iriMapper) {

        if (element instanceof ElementGroup group) {

            List<List<Atom>> result = new ArrayList<>();
            result.add(new ArrayList<>());

            for (Element child : group.getElements()) {

                List<List<Atom>> childAlternatives =
                        translateElement(child, iriMapper);

                result = combine(result, childAlternatives);
            }

            return result;
        }

        if (element instanceof ElementPathBlock pathBlock) {

            List<Atom> atoms = new ArrayList<>();

            for (TriplePath tp : pathBlock.getPattern().getList()) {
                Atom atom = translateTriplePath(tp, iriMapper);

                if (atom != null) {
                    atoms.add(atom);
                }
            }

            List<List<Atom>> result = new ArrayList<>();
            result.add(atoms);
            return result;
        }

        if (element instanceof ElementUnion union) {

            List<List<Atom>> result = new ArrayList<>();

            for (Element branch : union.getElements()) {
                result.addAll(translateElement(branch, iriMapper));
            }

            return result;
        }

        System.err.println("Warning: unsupported SPARQL element ignored -> "
                + element.getClass().getSimpleName());
        if (element instanceof ElementFilter) {
            throw new UnsupportedOperationException("FILTER non supportato: " + element);
        }

        if (element instanceof ElementOptional) {
            throw new UnsupportedOperationException("OPTIONAL non supportato: " + element);
        }

        if (element instanceof ElementBind) {
            throw new UnsupportedOperationException("BIND non supportato: " + element);
        }

        if (element instanceof ElementMinus) {
            throw new UnsupportedOperationException("MINUS non supportato: " + element);
        }

        if (element instanceof ElementSubQuery) {
            throw new UnsupportedOperationException("Subquery SPARQL non supportata: " + element);
        }

        if (element instanceof ElementNamedGraph) {
            throw new UnsupportedOperationException("GRAPH non supportato: " + element);
        }

        throw new UnsupportedOperationException(
                "Elemento SPARQL non supportato: "
                        + element.getClass().getSimpleName()
                        + " -> " + element
        );
    }

    private List<List<Atom>> combine(
            List<List<Atom>> left,
            List<List<Atom>> right
    ) {

        List<List<Atom>> result = new ArrayList<>();

        for (List<Atom> l : left) {
            for (List<Atom> r : right) {

                List<Atom> merged = new ArrayList<>();
                merged.addAll(l);
                merged.addAll(r);

                result.add(merged);
            }
        }

        return result;
    }

    private Atom translateTriplePath(TriplePath tp, IriMapper iriMapper) {

        if (!tp.isTriple()) {
            throw new UnsupportedOperationException(
                    "Property path SPARQL non supportato: " + tp
            );
        }

        Triple triple = tp.asTriple();

        String subject = nodeToTerm(triple.getSubject(), iriMapper);

        if (!triple.getPredicate().isURI()) {
            throw new UnsupportedOperationException(
                    "Predicato non-URI non supportato: " + triple.getPredicate()
            );
        }

        String predicate = triple.getPredicate().getURI();
        String object = nodeToTerm(triple.getObject(), iriMapper);

        if (predicate.equals(RDF_TYPE)) {
            return new Atom("classInst", List.of(subject, object));
        }

        String roleName = iriMapper.getSymbol(triple.getPredicate().getURI());

        return new Atom("roleInst", List.of(subject, object, roleName));
    }

    
    private Set<String> collectVariables(Element element) {

        Set<String> vars = new LinkedHashSet<>();

        ElementWalker.walk(
                element,
                new ElementVisitorBase() {
                    @Override
                    public void visit(ElementPathBlock el) {
                        for (TriplePath tp : el.getPattern().getList()) {

                            if (!tp.isTriple()) {
                                continue;
                            }

                            Triple triple = tp.asTriple();

                            collectVariableFromNode(
                                    triple.getSubject(),
                                    vars
                            );

                            collectVariableFromNode(
                                    triple.getPredicate(),
                                    vars
                            );

                            collectVariableFromNode(
                                    triple.getObject(),
                                    vars
                            );
                        }
                    }
                }
        );

        return vars;
    }

    private void collectVariableFromNode(Node node, Set<String> vars) {
        if (node.isVariable()) {
            vars.add(node.getName());
        }
    }

    private String nodeToTerm(Node node, IriMapper iriMapper) {

        if (node.isVariable()) {
            return toDatalogVar(node.getName());
        }

        if (node.isURI()) {
            return iriMapper.getSymbol(node.getURI());
        }

        if (node.isLiteral()) {
            return "\"" + node.getLiteralLexicalForm() + "\"";
        }

        return normalize(node.toString());
    }

    private String toDatalogVar(String varName) {
        if (varName == null || varName.isEmpty()) {
            return varName;
        }

        return Character.toUpperCase(varName.charAt(0))
                + varName.substring(1);
    }

    private String normalize(String name) {
        return name
                .replace("-", "_")
                .replace(" ", "_")
                .toLowerCase();
    }
}