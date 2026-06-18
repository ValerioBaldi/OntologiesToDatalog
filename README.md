# OntologiesToDatalog 
Project to encode RDF ontologies and Sparql queries  in datalog programs to be executed on dlv2. <br>

Structure (src/owl-datalog/): <br>
  -ontology contains the input ontology (.owl)  <br>
  -output contains the datalog program produced (.dl) and the mapping of the IRIs saved in a properties file <br>
  -queries contains the sparql queries used in the project, (.sparql) <br>
  -src/main/java/org/example: <br>
    -serializer contains the class to print the datalog program produced into a file (which is the .dl file inside output) <br>
    -Translator contains a class to translate an ontology(OntologyTranslator.java), a class to translate a query (QueryTranslator.java), and a class to map IRIs (IriMapper.java)<br>
    -model contains the class to define the encoding for datalog atoms (Atom.java), rules (Rule.java) and programs (Program.java) <br>
    -main.java <br>

