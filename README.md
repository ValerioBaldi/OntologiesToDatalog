# OntologiesToDatalog 
Project to encode RDF ontologies and Sparql queries  in datalog programs to be executed on dlv2. <br>

Structure (src/owl-datalog/): <br>
  -ontology contains the input ontology (.owl), animal_taxonomy_ontology.owl is the ontology currently used.  <br>
  -output contains the datalog program produced (.dl) and the mapping of the IRIs saved in a properties file, outputDLV2.dl is the past result of running the previous output.dl (using oldDatalogSerializer.java) on DLV-2. <br>
  -queries contains the sparql queries used in the project, (.sparql), queryAnimal.sparql is the query currently used.<br>
  -src/main/java/org/example: <br>
    -serializer contains the class to print the datalog program produced into a file (which is the .dl file inside output), the actual serializer is thought to run on soufflè, to go back to DLV-2 switch its name with oldDatalogSerializer.java. <br>
    -Translator contains a class to translate an ontology(OntologyTranslator.java), a class to translate a query (QueryTranslator.java), and a class to map IRIs (IriMapper.java)<br>
    -model contains the class to define the encoding for datalog atoms (Atom.java), rules (Rule.java) and programs (Program.java) <br>
    -main.java <br>
    

