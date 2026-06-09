package org.example.Translator;

import org.example.model.*;

import org.semanticweb.owlapi.model.*;

import java.io.IOException;
import java.util.List;

public class OntologyTranslator {

    private final IriMapper iriMapper = new IriMapper();

    public Program translate(OWLOntology ontology, String mappingPath) {

        try {
                iriMapper.load(mappingPath);
        } catch (IOException e) {
                System.out.println("Mapping file not found. A new one will be created at " + mappingPath);
        }

        Program program = new Program();

        OntologyEncoding(ontology, program);

        createWitnesses(ontology, program);

        InferenceOfABoxFacts(program);

        PositiveTboxAxioms(program);

        try {
                iriMapper.save(mappingPath);
        } catch (IOException e) {
                System.out.println("Error saving IRI mapping: " + e.getMessage());
        }

        return program;
    }

    private void OntologyEncoding(OWLOntology ontology,
                            Program program) {

        for (OWLClassAssertionAxiom ax :
                ontology.getAxioms(AxiomType.CLASS_ASSERTION)) {

            if (ax.getClassExpression().isOWLClass()
                    && ax.getIndividual().isNamed()) {

                
                String individual = iriMapper.getSymbol(
                        ax.getIndividual()
                                .asOWLNamedIndividual()
                                .getIRI()
                                .toString()
                );

                String cls = iriMapper.getSymbol(
                        ax.getClassExpression()
                                .asOWLClass()
                                .getIRI()
                                .toString()
                );

                program.addFact(
                        new Atom(
                                "classInst",
                                List.of(individual, cls)
                        )
                );
            }
        }
        for (OWLObjectPropertyAssertionAxiom ax :
                ontology.getAxioms(
                        AxiomType.OBJECT_PROPERTY_ASSERTION)) {

            String s = 
                    iriMapper.getSymbol(
                        ax.getSubject()
                                .asOWLNamedIndividual()
                                .getIRI()
                                .toString()
                     );

            String o =
                iriMapper.getSymbol(
                    ax.getObject()
                            .asOWLNamedIndividual()
                            .getIRI()
                            .toString()
                );

            String r =
                    iriMapper.getSymbol(
                        ax.getProperty()
                                .asOWLObjectProperty()
                                .getIRI()
                                .toString()
                    );

            program.addFact(
                    new Atom(
                            "roleInst",
                            List.of(s, o, r)
                    )
            );
        }

        for (OWLSubClassOfAxiom ax :
                ontology.getAxioms(AxiomType.SUBCLASS_OF)) {

            if (ax.getSubClass().isOWLClass()
                    && ax.getSuperClass().isOWLClass()) {

                String sub =
                        iriMapper.getSymbol(
                                ax.getSubClass()
                                        .asOWLClass()
                                        .getIRI()
                                        .toString()
                        );

                String sup =
                        iriMapper.getSymbol(
                                ax.getSuperClass()
                                                .asOWLClass()
                                                .getIRI()
                                                .toString()
                        );

                program.addFact(
                        new Atom(
                                "subClass",
                                List.of(sub, sup)
                        )
                );
            }
        }

        for (OWLSubObjectPropertyOfAxiom ax :
                ontology.getAxioms(
                        AxiomType.SUB_OBJECT_PROPERTY)) {

            String sub =
                    iriMapper.getSymbol(
                        ax.getSubProperty()
                                .asOWLObjectProperty()
                                .getIRI()
                                .toString()
                    );

            String sup =
                    iriMapper.getSymbol(
                        ax.getSuperProperty()
                                .asOWLObjectProperty()
                                .getIRI()
                                .toString()
                    );

            program.addFact(

                    new Atom(
                            "subRole",
                            List.of(sub, sup)
                    )
            );
        }

        for (OWLObjectPropertyDomainAxiom ax :
                        ontology.getAxioms(AxiomType.OBJECT_PROPERTY_DOMAIN)) {

                if (!ax.getProperty().isAnonymous()
                        && ax.getDomain().isOWLClass()) {

                        String role = iriMapper.getSymbol(
                                ax.getProperty()
                                        .asOWLObjectProperty()
                                        .getIRI()
                                        .toString()
                        );

                        String domain = iriMapper.getSymbol(
                                ax.getDomain()
                                        .asOWLClass()
                                        .getIRI()
                                        .toString()
                        );

                        program.addFact(
                                new Atom(
                                        "domain",
                                        List.of(domain, role)
                                )
                        );
                }
                }

                for (OWLObjectPropertyRangeAxiom ax :
                        ontology.getAxioms(AxiomType.OBJECT_PROPERTY_RANGE)) {

                if (!ax.getProperty().isAnonymous()
                        && ax.getRange().isOWLClass()) {

                        String role = iriMapper.getSymbol(
                                ax.getProperty()
                                        .asOWLObjectProperty()
                                        .getIRI()
                                        .toString()
                        );

                        String range = iriMapper.getSymbol(
                                ax.getRange()
                                        .asOWLClass()
                                        .getIRI()
                                        .toString()
                        );

                        program.addFact(
                                new Atom(
                                        "range",
                                        List.of(range, role)
                                )
                        );
                }
                }

                for (OWLInverseObjectPropertiesAxiom ax :
                        ontology.getAxioms(AxiomType.INVERSE_OBJECT_PROPERTIES)) {

                OWLObjectPropertyExpression first =
                        ax.getFirstProperty();

                OWLObjectPropertyExpression second =
                        ax.getSecondProperty();

                if (!first.isAnonymous() && !second.isAnonymous()) {

                        String r = iriMapper.getSymbol(
                                first.asOWLObjectProperty()
                                        .getIRI()
                                        .toString()
                        );

                        String s = iriMapper.getSymbol(
                                second.asOWLObjectProperty()
                                        .getIRI()
                                        .toString()
                        );

                        program.addFact(
                                new Atom(
                                        "inverse",
                                        List.of(r, s)
                                )
                        );

                        program.addFact(
                                new Atom(
                                        "inverse",
                                        List.of(s, r)
                                )
                        );
                }
        }
    }

    private void createWitnesses(OWLOntology ontology,
                                Program program) {

        for (OWLClass cls :
                ontology.getClassesInSignature()) {

            String c = iriMapper.getSymbol(cls.getIRI().toString());

            String w = "w_" + c;

            program.addFact(
                    new Atom(
                            "classWit",
                            List.of(w, c)
                    )
            );

            program.addFact(
                    new Atom(
                            "classInst",
                            List.of(w, c)
                    )
            );
            program.addFact(
                new Atom(
                        "witness",
                        List.of(w)
                )
            );
        }

        for (OWLObjectProperty prop :
                ontology.getObjectPropertiesInSignature()) {

            String r = iriMapper.getSymbol(prop.getIRI().toString());

            String ws = "ws_" + r;

            String wo = "wo_" + r;

            program.addFact(
                    new Atom(
                            "subjWit",
                            List.of(ws, r)
                    )
            );

            program.addFact(
                    new Atom(
                            "objWit",
                            List.of(wo, r)
                    )
            );

            program.addFact(
                    new Atom(
                            "witness",
                            List.of(ws)
                    )
            );

            program.addFact(
                    new Atom(
                            "witness",
                            List.of(wo)
                    )
            );

            program.addFact(
                new Atom(
                        "roleWit",
                        List.of(ws, wo, r)
                )
            );

            program.addFact(
                    new Atom(
                            "roleInst",
                            List.of(ws, wo, r)
                    )
            );
        }
    }

    private void InferenceOfABoxFacts(Program program) {

        Rule r1 = new Rule(

                new Atom(
                        "classInst",
                        List.of("E", "B")
                ),

                List.of(

                        new Atom(
                                "classInst",
                                List.of("E", "A")
                        ),

                        new Atom(
                                "subClass",
                                List.of("A", "B")
                        )
                )
        );

        program.addRule(r1);


        Rule r2 = new Rule(

                new Atom(
                        "classInst",
                        List.of("E1", "A")
                ),

                List.of(

                        new Atom(
                                "roleInst",
                                List.of("E1", "E2", "R")
                        ),

                        new Atom(
                                "domain",
                                List.of("W", "R")
                        ),

                        new Atom(
                                 "subClass",
                                List.of("W", "A")
                        )
                )
        );

        program.addRule(r2);        

        Rule r3 = new Rule(

                new Atom(
                        "classInst",
                        List.of("E2", "A")
                ),

                List.of(
                        new Atom(
                                "roleInst",
                                List.of("E1", "E2", "R")
                        ),

                        new Atom(
                                "inverse",
                                List.of("S", "R")
                        ),

                        new Atom(
                                "domain",
                                List.of("W", "S")
                        ),
                        new Atom(
                                "subClass",
                                List.of("W", "A")
                        )
                )
        );

        program.addRule(r3);

        Rule r4 = new Rule(

                new Atom(
                        "roleInst",
                        List.of("E1", "E2", "S")
                ),

                List.of(
                        new Atom(
                                "roleInst",
                                List.of("E1", "E2", "R")
                        ),

                        new Atom(
                                "subRole",
                                List.of("R", "S")
                        )
                )
        );

        program.addRule(r4);

        Rule r5 = new Rule(

                new Atom(
                        "roleInst",
                        List.of("E2", "E1", "S")
                ),

                List.of(
                        new Atom(
                                "roleInst",
                                List.of("E1", "E2", "R")
                        ),

                        new Atom(
                                "inverse",
                                List.of("R1", "R")
                        ),
                        new Atom(
                                "subRole",
                                List.of("R1", "S")
                        )
                )
        );
        program.addRule(r5);

        Rule r6 = new Rule(

                new Atom(
                        "roleInst",
                        List.of("E2", "E1", "S")
                ),

                List.of(
                        new Atom(
                                "roleInst",
                                List.of("E1", "E2", "R")
                        ),

                        new Atom(
                                "inverse",
                                List.of("S1", "S")
                        ),
                        new Atom(
                                "subRole",
                                List.of("R", "S1")
                        )
                )
        );
        program.addRule(r6);

        Rule r7 = new Rule(

                new Atom(
                        "roleInst",
                        List.of("E1", "E2", "S")
                ),

                List.of(
                        new Atom(
                                "roleInst",
                                List.of("E1", "E2", "R")
                        ),

                        new Atom(
                                "subRole",
                                List.of("R1", "S1")
                        ),
                        new Atom(
                                "inverse",
                                List.of("R1", "R")
                        ),
                        new Atom(
                                "inverse",
                                List.of("S1", "S")
                        )
                )
        );

        program.addRule(r7);
    }

    private void PositiveTboxAxioms(Program program) {

        Rule r1 = new Rule(

                new Atom(
                        "subClass",
                        List.of("A", "B")
                ),

                List.of(
                        new Atom(
                                "classWit",
                                List.of("E", "A")
                        ),

                        new Atom(
                                "classInst",
                                List.of("E", "B")
                        )
                )
        );

        program.addRule(r1);

        Rule r2 = new Rule(

                new Atom(
                        "subClass",
                        List.of("W", "A")
                ),

                List.of(
                        new Atom(
                                "domain",
                                List.of("W", "R")
                        ),

                        new Atom(
                                "subjWit",
                                List.of("E", "R")
                        ),
                        new Atom(
                                "classInst",
                                List.of("E", "A")
                        )
                )
        );

        program.addRule(r2);

        Rule r3 = new Rule(

                new Atom(
                        "subClass",
                        List.of("W", "A")
                ),

                List.of(
                        new Atom(
                                "inverse",
                                List.of("R", "S")
                        ),

                        new Atom(
                                "domain",
                                List.of("W", "S")
                        ),

                        new Atom(
                                "objWit",
                                List.of("E", "R")
                        ),
                        new Atom(
                                "classInst",
                                List.of("E", "A")
                        )
                )
        );

        program.addRule(r3);

        Rule r4 = new Rule(

                new Atom(
                        "subRole",
                        List.of("R", "S")
                ),

                List.of(
                        new Atom(
                                "subjWit",
                                List.of("E1", "R")
                        ),

                        new Atom(
                                "objWit",
                                List.of("E2", "R")
                        ),
                        new Atom(
                                "roleInst",
                                List.of("E1", "E2", "S")
                        )
                )
        );

        program.addRule(r4);

        Rule r5 = new Rule(

                new Atom(
                        "subRole",
                        List.of("R", "S1")
                ),

                List.of(
                        new Atom(
                                "inverse",
                                List.of("S1", "S")
                        ),

                        new Atom(
                                "subjWit",
                                List.of("E1", "R")
                        ),

                        new Atom(
                                "objWit",
                                List.of("E2", "R")
                        ),
                        new Atom(
                                "roleInst",
                                List.of("E2", "E1", "S")
                        )
                )
        );

        program.addRule(r5);

        Rule r6 = new Rule(

                new Atom(
                        "subRole",
                        List.of("R1", "S")
                ),

                List.of(
                        new Atom(
                                "inverse",
                                List.of("R1", "R")
                        ),

                        new Atom(
                                "subjWit",
                                List.of("E1", "R")
                        ),

                        new Atom(
                                "objWit",
                                List.of("E2", "R")
                        ),
                        new Atom(
                                "roleInst",
                                List.of("E2", "E1", "S")
                        )
                )
        );

        program.addRule(r6);

        Rule r7 = new Rule(

                new Atom(
                        "subRole",
                        List.of("R1", "S1")
                ),

                List.of(
                        new Atom(
                                "inverse",
                                List.of("R1", "R")
                        ),

                        new Atom(
                                "inverse",
                                List.of("S1", "S")
                        ),

                        new Atom(
                                "subjWit",
                                List.of("E1", "R")
                        ),

                        new Atom(
                                "objWit",
                                List.of("E2", "R")
                        ),
                        new Atom(
                                "roleInst",
                                List.of("E1", "E2", "S")
                        )
                )
        );

        program.addRule(r7);
    }
}