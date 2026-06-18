package org.example.Translator;

import org.example.model.*;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.TriplePath;

import java.util.ArrayList;
import java.util.List;

public class QueryTranslator2 {

    // RDF type IRI, used to recognize "?x a ex:Class" patterns
    private static final String RDF_TYPE =
            "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    /**
     * Translates a SPARQL SELECT query string into a Datalog rule
     * of the form:  query_result(X, ...) :- body...
     * and adds it to the given program.
     */
    public void translate(String sparqlQuery, Program program) {

        Query query = QueryFactory.create(sparqlQuery);

        // Collect the projected variables (SELECT ?x ?y ...)
        List<String> projectedVars = query.getResultVars(); // e.g. ["x", "y"]

        // Build the body atoms by visiting the WHERE clause
        List<Atom> bodyAtoms = new ArrayList<>();
        ElementWalker.walk(
                query.getQueryPattern(),
                new ElementVisitorBase() {
                    @Override
                    public void visit(ElementPathBlock el) {
                        for (TriplePath tp : el.getPattern().getList()) {
                            Atom atom = translateTriplePath(tp);
                            if (atom != null) {
                                bodyAtoms.add(atom);
                            }
                        }
                    }
                }
        );

        // Head: query_result(X, Y, ...)  — terms follow SELECT order
        List<String> headTerms = projectedVars.stream()
                .map(this::toDatalogVar)
                .toList();

        Atom head = new Atom("query_result", headTerms);
        Rule queryRule = new Rule(head, bodyAtoms);
        program.addRule(queryRule);
    }

    /**
     * Translates a single triple path into a Datalog Atom.
     *
     * Supported patterns:
     *   ?x  rdf:type  ex:Person   ->  classInst(X, "person")
     *   ?x  ex:knows  ?y          ->  roleInst(X, Y, "knows")
     *   ex:John  rdf:type  ?c     ->  classInst("john", C)
     */
    private Atom translateTriplePath(TriplePath tp) {

        // We only handle simple triples (no property paths)
        if (!tp.isTriple()) {
            System.err.println(
                "Warning: property path ignored -> " + tp);
            return null;
        }

        Triple triple = tp.asTriple();

        String subject   = nodeToTerm(triple.getSubject());
        String predicate = triple.getPredicate().getURI();
        String object    = nodeToTerm(triple.getObject());

        // Pattern: ?x rdf:type ex:ClassName
        if (predicate.equals(RDF_TYPE)) {
            return new Atom("classInst", List.of(subject, object));
        }

        // Pattern: ?x ex:someProperty ?y
        String roleName = normalize(
                triple.getPredicate().getLocalName());
        return new Atom("roleInst", List.of(subject, object, roleName));
    }

    /**
     * Converts an RDF node to a Datalog term string.
     *   - Variables  (?x)        -> uppercase "X"
     *   - Named URIs (ex:Person) -> quoted lowercase "person"
     */
    private String nodeToTerm(org.apache.jena.graph.Node node) {
        if (node.isVariable()) {
            return toDatalogVar(node.getName());   // ?x -> "X"
        }
        if (node.isURI()) {
            return normalize(node.getLocalName()); // ex:Person -> "person"
        }
        // Literal (not expected in instance queries, but handled)
        return "\"" + node.getLiteralLexicalForm() + "\"";
    }

    /**
     * Converts a SPARQL variable name to a Datalog variable:
     * first letter uppercase, rest unchanged.
     * e.g.  "x" -> "X",  "myVar" -> "MyVar"
     */
    private String toDatalogVar(String varName) {
        if (varName == null || varName.isEmpty()) return varName;
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
