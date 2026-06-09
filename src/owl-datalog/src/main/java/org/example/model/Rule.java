package org.example.model;

import java.util.List;

// datalog rule
public class Rule {

    private Atom head;

    private List<Atom> body;

    public Rule(Atom head, List<Atom> body) {
        this.head = head;
        this.body = body;
    }

    // example: parent(X, Y) :- father(X, Y). -> head: parent(X, Y), body: [father(X, Y)]

    public Atom getHead() {
        return head;
    }

    public List<Atom> getBody() {
        return body;
    }
}