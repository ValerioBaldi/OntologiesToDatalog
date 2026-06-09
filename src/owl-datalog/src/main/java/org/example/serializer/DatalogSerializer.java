package org.example.serializer;

import java.io.FileWriter;
import java.io.IOException;

import org.example.model.*;

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
                + String.join(", ", atom.getTerms())
                + ")";
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