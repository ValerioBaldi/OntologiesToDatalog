// Datalog serializer for Soufflé syntax


package org.example.serializer;

import java.io.FileWriter;
import java.io.IOException;

import org.example.model.*;

import java.util.stream.Collectors;

public class DatalogSerializer {

    public static void printProgram(Program program) {

        System.out.println("=== FACTS ===");

        for (Atom atom : program.getFacts()) {
            System.out.println(atomToString(atom) + ".");
        }

        System.out.println("\n=== RULES ===");

        for (Rule rule : program.getRules()) {
            System.out.println(ruleToString(rule));
        }
    }

    public static void writeProgramToFile(
            Program program,
            String fileName) throws IOException {

        try (FileWriter writer = new FileWriter(fileName)) {

            writer.write("""
                .decl classInst(e:symbol, c:symbol)
                .decl roleInst(e1:symbol, e2:symbol, r:symbol)
                .decl subClass(c1:symbol, c2:symbol)
                .decl subRole(r1:symbol, r2:symbol)
                .decl domain(c:symbol, r:symbol)
                .decl rangeP(c:symbol, r:symbol)
                .decl inverse(r:symbol, s:symbol)
                .decl distinct(e1:symbol, e2:symbol)
                .decl refl(r:symbol)
                .decl irrefl(r:symbol)

                .decl inSignatureI(x:symbol)
                .decl inSignatureC(x:symbol)
                .decl inSignatureR(x:symbol)
                .decl inSignature(x:symbol)

                .decl classWit(e:symbol, c:symbol)
                .decl subjWit(e:symbol, r:symbol)
                .decl objWit(e:symbol, r:symbol)
                .decl roleWit(e1:symbol, e2:symbol, r:symbol)
                .decl witness(e:symbol)

                .decl complement(x:symbol, y:symbol)
                .decl disjC(c1:symbol, c2:symbol)
                .decl disjR(r1:symbol, r2:symbol)

                .decl classInstF(e:symbol, c:symbol)
                .decl roleInstF(e1:symbol, e2:symbol, r:symbol)
                .decl subClassF(c1:symbol, c2:symbol)
                .decl subRoleF(r1:symbol, r2:symbol)

                .output classInstF
                .output roleInstF
                .output subClassF
                .output subRoleF

            """);

            for (Atom atom : program.getFacts()) {
                writer.write(atomToString(atom) + ".\n");
            }

            writer.write("\n");

            for (Rule rule : program.getRules()) {
                writer.write(ruleToString(rule) + "\n");
            }
        }
    }

    private static String atomToString(Atom atom) {

        return atom.getPredicate()
                + "("
                + atom.getTerms().stream()
                .map(DatalogSerializer::termToSouffle)
                    .collect(java.util.stream.Collectors.joining(", "))
                + ")";
    }

    private static String termToSouffle(String term) {
        if (isVariable(term)) {
            return term;
        }

        return "\"" 
                + term.replace("\\", "\\\\").replace("\"", "\\\"")
                + "\"";
    }

    private static boolean isVariable(String term) {
        return term.matches("[A-Z][A-Za-z0-9_]*");
    }

    private static String ruleToString(Rule rule) {

        StringBuilder sb = new StringBuilder();

        sb.append(atomToString(rule.getHead()));

        sb.append(" :- ");

        for (int i = 0; i < rule.getBody().size(); i++) {

            sb.append(atomToString(rule.getBody().get(i)));

            if (i < rule.getBody().size() - 1) {
                sb.append(", ");
            }
        }

        sb.append(".");

        return sb.toString();
    }

    public String normalize(String name) {

        return name
                .replace("-", "_")
                .replace(" ", "_")
                .toLowerCase();
    }
}