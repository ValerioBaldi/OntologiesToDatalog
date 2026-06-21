package org.example;

import org.example.model.Program;
import org.example.serializer.DatalogSerializer;
import org.example.Translator.OntologyTranslator;
import org.example.Translator.QueryTranslator;
import org.example.Translator.IriMapper;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {

        try {

                OWLOntologyManager manager =
                        OWLManager.createOWLOntologyManager();

                OWLOntology ontology =
                        manager.loadOntologyFromOntologyDocument(
                                new File("ontology/animal_taxonomy_ontology.owl")
                        );

                String mappingPath = "output/iri-mapping.properties";

                OntologyTranslator translator =
                        new OntologyTranslator();

                Program program =
                        translator.translate(ontology, mappingPath);


                String queryString = Files.readString(
                        Paths.get("queries/queryAnimal.sparql")
                );
                
                QueryTranslator queryTranslator = new QueryTranslator();
                queryTranslator.translate(
                        queryString,
                        program,
                        mappingPath
                );

                DatalogSerializer.printProgram(program);

                        DatalogSerializer.writeProgramToFile(
                                program,
                                "output/output.dl"
                        );

                        System.out.println(
                                "\nProgramma Datalog salvato in output/output.dl"
                        );

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}