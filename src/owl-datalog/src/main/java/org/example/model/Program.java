package org.example.model;

import java.util.ArrayList;
import java.util.List;

// datalog program instance
public class Program {

    private List<Atom> facts = new ArrayList<>();

    private List<Rule> rules = new ArrayList<>();

    public void addFact(Atom atom) {
        facts.add(atom);
    }

    public void addRule(Rule rule) {
        rules.add(rule);
    }

    public List<Atom> getFacts() {
        return facts;
    }

    public List<Rule> getRules() {
        return rules;
    }
}