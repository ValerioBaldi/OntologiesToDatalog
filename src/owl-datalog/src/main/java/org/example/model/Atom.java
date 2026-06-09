package org.example.model;

import java.util.List;
 

public class Atom {

    private String predicate;

    private List<String> terms;

    public Atom(String predicate, List<String> terms) {
        this.predicate = predicate; //name of the predicate
        this.terms = terms; //list of terms of the atom
    }

    // example: classInst(John, Person) -> predicate: classInst, terms: [John, Person]

    public String getPredicate() {
        return predicate;
    }

    public List<String> getTerms() {
        return terms;
    }
}