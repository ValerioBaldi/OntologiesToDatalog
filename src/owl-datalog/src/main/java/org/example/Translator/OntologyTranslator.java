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

        addComplementPredicates(ontology, program);

        addSignaturePredicates(ontology, program);

        encodingTopAndBottomPredicates(program);

        createWitnesses(ontology, program);

        TopAndBottomAxioms(program);

        InferenceOfABoxFacts(program);

        PositiveTboxAxioms(program);

        EncodingDisjointedness(program);

        RemoveWitnessesAndAuxiliaryAxioms(program);

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

                
                String individual = getIndividualSymbol(ax.getIndividual());

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

            String s = getIndividualSymbol(ax.getSubject());

            String o = getIndividualSymbol(ax.getObject());

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

        for (OWLDifferentIndividualsAxiom ax :
            ontology.getAxioms(AxiomType.DIFFERENT_INDIVIDUALS)) {

                List<OWLIndividual> inds = ax.getIndividualsAsList();

                for (int i = 0; i < inds.size(); i++) {
                        for (int j = i + 1; j < inds.size(); j++) {

                                if (inds.get(i).isNamed() && inds.get(j).isNamed()) {
                                        String a = getIndividualSymbol(inds.get(i));
                                        String b = getIndividualSymbol(inds.get(j));

                                        program.addFact(new Atom("distinct", List.of(a, b)));
                                        program.addFact(new Atom("distinct", List.of(b, a)));
                                }
                        }
                }
        }

        for (OWLSubClassOfAxiom ax :
                ontology.getAxioms(AxiomType.SUBCLASS_OF)) {


            OWLClassExpression subClass = ax.getSubClass();
            OWLClassExpression supClass = ax.getSuperClass();

            if (subClass.isOWLClass()
                    && supClass.isOWLClass()) {

                String sub =
                        iriMapper.getSymbol(
                                subClass
                                        .asOWLClass()
                                        .getIRI()
                                        .toString()
                        );

                String sup =
                        iriMapper.getSymbol(
                                supClass
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

            else if (subClass.isOWLClass()
                        && supClass instanceof OWLObjectComplementOf complement
                        && complement.getOperand().isOWLClass()) {

                String a = iriMapper.getSymbol(subClass.asOWLClass().getIRI().toString());
                String b = iriMapper.getSymbol(complement.getOperand().asOWLClass().getIRI().toString());

                addConceptDisjointness(program, a, b);
            }
        }

        for (OWLDisjointClassesAxiom ax :
            ontology.getAxioms(AxiomType.DISJOINT_CLASSES)) {

                List<OWLClassExpression> classes = ax.getClassExpressionsAsList();

                for (int i = 0; i < classes.size(); i++) {
                        for (int j = i + 1; j < classes.size(); j++) {

                                if (classes.get(i).isOWLClass()
                                        && classes.get(j).isOWLClass()) {

                                String a = getClassSymbol(classes.get(i).asOWLClass());
                                String b = getClassSymbol(classes.get(j).asOWLClass());

                                addConceptDisjointness(program, a, b);
                                }
                        }
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

        for (OWLDisjointObjectPropertiesAxiom ax :
            ontology.getAxioms(AxiomType.DISJOINT_OBJECT_PROPERTIES)) {

                List<OWLObjectPropertyExpression> props =
                        ax.getProperties().stream().toList();

                for (int i = 0; i < props.size(); i++) {
                        for (int j = i + 1; j < props.size(); j++) {

                                if (!props.get(i).isAnonymous()
                                        && !props.get(j).isAnonymous()) {

                                String r = getRoleSymbol(props.get(i).asOWLObjectProperty());
                                String s = getRoleSymbol(props.get(j).asOWLObjectProperty());

                                addRoleDisjointness(program, r, s);
                                }
                        }
                }
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
                                        "rangeP",
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

        for (OWLReflexiveObjectPropertyAxiom ax :
            ontology.getAxioms(AxiomType.REFLEXIVE_OBJECT_PROPERTY)) {

                if (!ax.getProperty().isAnonymous()) {
                        String r = getRoleSymbol(ax.getProperty().asOWLObjectProperty());
                        program.addFact(new Atom("refl", List.of(r)));
                }
        }

        for (OWLIrreflexiveObjectPropertyAxiom ax :
                ontology.getAxioms(AxiomType.IRREFLEXIVE_OBJECT_PROPERTY)) {

                if (!ax.getProperty().isAnonymous()) {
                        String r = getRoleSymbol(ax.getProperty().asOWLObjectProperty());
                        program.addFact(new Atom("irrefl", List.of(r)));
                }
        }
    }

   private void addSignaturePredicates(OWLOntology ontology, Program program) {

        // Individual signature
        ontology.individualsInSignature().forEach(ind -> {
                if (!ind.isAnonymous()) {
                String e = getIndividualSymbol(ind);

                program.addFact(new Atom("inSignatureI", List.of(e)));
                }
        });

        // Class signature
        ontology.classesInSignature().forEach(cls -> {
                String a = iriMapper.getSymbol(
                        cls.getIRI().toString()
                );

                program.addFact(new Atom("inSignatureC", List.of(a)));
        });

        // Role signature
        ontology.objectPropertiesInSignature().forEach(prop -> {
                String r = iriMapper.getSymbol(
                        prop.getIRI().toString()
                );

                program.addFact(new Atom("inSignatureR", List.of(r)));
        });

        program.addRule(new Rule(
                new Atom("inSignature", List.of("X")),
                List.of(new Atom("inSignatureI", List.of("X")))
        ));

        program.addRule(new Rule(
                new Atom("inSignature", List.of("X")),
                List.of(new Atom("inSignatureC", List.of("X")))
        ));

        program.addRule(new Rule(
                new Atom("inSignature", List.of("X")),
                List.of(new Atom("inSignatureR", List.of("X")))
        ));
}

    private void encodingTopAndBottomPredicates(Program program) {

        // Rules from 16 to 19 encode the semantics of top and bottom concepts and roles

        Rule rule16 = new Rule(
                new Atom(
                        "classInst", 
                        List.of("E", "topC")
                ),
                List.of(new Atom(
                        "inSignatureI", 
                        List.of("E")
                ))
        );

        program.addRule(rule16);

        Rule rule17 = new Rule(
                new Atom(
                        "roleInst", 
                        List.of("E1", "E2", "topR")
                ),
                List.of(
                        new Atom("inSignatureI", List.of("E1")),
                        new Atom("inSignatureI", List.of("E2"))
                )
        );

        program.addRule(rule17);

        // Rows 18 to 23

        program.addFact(new Atom(
                "inSignatureC", 
                List.of("topC")
        ));
        
        program.addFact(new Atom(
                "inSignatureR", 
                List.of("topR")
        ));

        program.addFact(new Atom(
                "inSignatureC", 
                List.of("bottomC")
        ));

        program.addFact(new Atom(
                "inSignatureR", 
                List.of("bottomR")
        ));

        // Introduced domainTopR and domainBottomR to represent the domain of topR and bottomR respectively

        program.addFact(new Atom(
                "domain",
                List.of("domainTopR", "topR")
        ));

        program.addFact(new Atom(
                "inverse",
                List.of("topR", "topR")
        ));

        program.addFact(new Atom(
                "domain",
                List.of("domainBottomR", "bottomR")
        ));

        program.addFact(new Atom(
                "inverse",
                List.of("bottomR", "bottomR")
        ));

        program.addFact(new Atom(
                "subClass",
                List.of("domainTopR", "topC")
        ));

        program.addFact(new Atom(
                "subClass",
                List.of("bottomC", "domainTopR")
        ));
    }

    private void TopAndBottomAxioms(Program program) {

        // Rules from 24 to 31 encode the semantics of top and bottom concepts and roles

        Rule rule24 = new Rule(
                new Atom(
                        "subClass",
                        List.of("C", "topC")
                ),
                List.of(
                        new Atom(
                                "inSignatureC",
                                List.of("C"))
                        )
        );

        program.addRule(rule24);

        Rule rule25 = new Rule(
                new Atom(
                        "subClass", 
                        List.of("bottomC", "C")
                ),
                List.of(new Atom(
                        "inSignatureC", 
                        List.of("C")
                ))
        );

        program.addRule(rule25);

        Rule rule26 = new Rule(
                new Atom(
                        "subClass", 
                        List.of("A", "A")
                ),
                List.of(new Atom(
                        "inSignatureC", 
                        List.of("A")
                ))
        );

        program.addRule(rule26);

        Rule rule27 = new Rule(
                new Atom(
                        "disjC", 
                        List.of("C", "bottomC")
                ),
                List.of(new Atom(
                        "inSignatureC", 
                        List.of("C")
                ))
        );

        program.addRule(rule27);

        Rule rule28 = new Rule(
                new Atom(
                        "subRole", 
                        List.of("R", "topR")
                ),
                List.of(new Atom(
                        "inSignatureR", 
                        List.of("R")
                ))
        );
        
        program.addRule(rule28);

        Rule rule29 = new Rule(
                new Atom(
                        "subRole", 
                        List.of("bottomR", "R")
                ),
                List.of(new Atom(
                        "inSignatureR", 
                        List.of("R")
                ))
        );
        
        program.addRule(rule29);

        Rule rule30 = new Rule(
                new Atom(
                        "subRole", 
                        List.of("R", "R")
                ),
                List.of(new Atom(
                        "inSignatureR", 
                        List.of("R")
                ))
        );

        program.addRule(rule30);

        Rule rule31 = new Rule(
                new Atom(
                        "disjR", 
                        List.of("R", "bottomR")
                ),
                List.of(new Atom(
                        "inSignatureR", 
                        List.of("R")
                ))
        );

        program.addRule(rule31);

        Rule rule32 = new Rule(
                new Atom(
                        "subClass", 
                        List.of("C", "domainTopR")
                ),
                List.of(new Atom(
                        "inSignatureC", 
                        List.of("C")
                ))
        );

        program.addRule(rule32);

        Rule rule33 = new Rule(
                new Atom(
                        "subClass", 
                        List.of("domainBottomR", "C")
                ),
                List.of(new Atom(
                        "inSignatureC", 
                        List.of("C")
                ))
        );

        program.addRule(rule33);

        // 34 and 35 still to be added
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

    private void addComplementPredicates(OWLOntology ontology, Program program) {

        ontology.classesInSignature().forEach(cls -> {
                String e = getClassSymbol(cls);
                String notE = complementOf(e);

                program.addFact(new Atom("complement", List.of(e, notE)));
                program.addFact(new Atom("complement", List.of(notE, e)));
        });

        ontology.objectPropertiesInSignature().forEach(prop -> {
                String e = getRoleSymbol(prop);
                String notE = complementOf(e);

                program.addFact(new Atom("complement", List.of(e, notE)));
                program.addFact(new Atom("complement", List.of(notE, e)));
        });
    }

    private void InferenceOfABoxFacts(Program program) {

        Rule r39 = new Rule(

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

        program.addRule(r39);


        Rule r40 = new Rule(

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

        program.addRule(r40);        

        Rule r41 = new Rule(

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

        program.addRule(r41);

        Rule r42 = new Rule(

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

        program.addRule(r42);

        Rule r43 = new Rule(

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
        program.addRule(r43);

        Rule r44 = new Rule(

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
        program.addRule(r44);

        Rule r45 = new Rule(

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

        program.addRule(r45);
    }

    private void PositiveTboxAxioms(Program program) {

        Rule r54 = new Rule(

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

        program.addRule(r54);

        Rule r55 = new Rule(

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

        program.addRule(r55);

        Rule r56 = new Rule(

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

        program.addRule(r56);

        Rule r57 = new Rule(

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

        program.addRule(r57);

        Rule r58 = new Rule(

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

        program.addRule(r58);

        Rule r59 = new Rule(

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

        program.addRule(r59);

        Rule r60 = new Rule(

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

        program.addRule(r60);
    }

    private void EncodingDisjointedness(Program program) {

        Rule r61 = new Rule(

                new Atom(
                        "disjC",
                        List.of("X", "W")
                ),

                List.of(
                        new Atom(
                                "subClass",
                                List.of("X", "Y")
                        ),

                        new Atom(
                                "complement",
                                List.of("Y", "W")
                        )
                )
        );

        program.addRule(r61);

        Rule r62 = new Rule(

                new Atom(
                        "disjR",
                        List.of("X", "W")
                ),

                List.of(
                        new Atom(
                                "subRole",
                                List.of("X", "Y")
                        ),

                        new Atom(
                                "complement",
                                List.of("Y", "W")
                        )
                )
        );

        program.addRule(r62);
    }

    private void RemoveWitnessesAndAuxiliaryAxioms(Program program) {

        Rule rule67 = new Rule(
                new Atom(
                        "classInstF", 
                        List.of("E", "A")
                ),
                List.of(
                        new Atom(
                                "inSignature", 
                                List.of("E")
                        ),
                        new Atom(
                                "classInst", 
                                List.of("E", "A")
                        ),
                        new Atom(
                                "inSignature", 
                                List.of("A")
                        )
                )
        );

        program.addRule(rule67);

        Rule rule68 = new Rule(
                new Atom(
                        "roleInstF", 
                        List.of("E1", "E2", "A")
                ),
                List.of(
                        new Atom(
                                "inSignature", 
                                List.of("E1")
                        ),
                        new Atom(
                                "inSignature", 
                                List.of("E2")
                        ),
                        new Atom(
                                "roleInst", 
                                List.of("E1", "E2", "A")
                        ),
                        new Atom(
                                "inSignature", 
                                List.of("A")
                        )
                )
        );

        program.addRule(rule68);

        Rule rule69 = new Rule(
                new Atom(
                        "subClassF", 
                        List.of("E1", "E2")
                ),
                List.of(
                        new Atom(
                                "inSignature", 
                                List.of("E1")
                        ),
                        new Atom(
                                "inSignature", 
                                List.of("E2")
                        ),
                        new Atom(
                                "subClass", 
                                List.of("E1", "E2")
                        )
                )
        );

        program.addRule(rule69);

        Rule rule70 = new Rule(
                new Atom(
                        "subRoleF", 
                        List.of("E1", "E2")
                ),
                List.of(
                        new Atom(
                                "inSignature", 
                                List.of("E1")
                        ),
                        new Atom(
                                "inSignature", 
                                List.of("E2")
                        ),
                        new Atom(
                                "subRole", 
                                List.of("E1", "E2")
                        )
                )
        );

        program.addRule(rule70);
     }


     // helper

     private String getIndividualSymbol(OWLIndividual individual) {
        return iriMapper.getSymbol(
                individual.asOWLNamedIndividual().getIRI().toString()
        );
   }

     private String getClassSymbol(OWLClass cls) {
        return iriMapper.getSymbol(cls.getIRI().toString());
  }

    private String getRoleSymbol(OWLObjectProperty prop) {
        return iriMapper.getSymbol(prop.getIRI().toString());
        }

    private String complementOf(String symbol) {
        return "not_" + symbol;
        }

    private void addConceptDisjointness(Program program, String a, String b) {
        String notA = complementOf(a);
        String notB = complementOf(b);

        program.addFact(new Atom("subClass", List.of(a, notB)));
        program.addFact(new Atom("subClass", List.of(b, notA)));
        }

    private void addRoleDisjointness(Program program, String r, String s) {
        String notR = complementOf(r);
        String notS = complementOf(s);

        program.addFact(new Atom("subRole", List.of(r, notS)));
        program.addFact(new Atom("subRole", List.of(s, notR)));
        }
}