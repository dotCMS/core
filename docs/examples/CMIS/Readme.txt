---------- Accessing dotCMS via CMIS in Java ----------


To run the example use the following commands:

1) Compile the sample CMIS class:

javac -classpath lib/chemistry-opencmis-client-api-0.13.0.jar:lib/chemistry-opencmis-client-bindings-0.13.0.jar:lib/chemistry-opencmis-client-impl-0.13.0.jar:lib/chemistry-opencmis-commons-api-0.13.0.jar:lib/chemistry-opencmis-commons-impl-0.13.0.jar:lib/commons-logging-1.1.1.jar:lib/slf4j-api-1.7.12.jar:lib/slf4j-simple-1.7.12.jar:lib/stax2-api-3.1.4.jar:lib/woodstox-core-asl-4.4.0.jar DotCMSHelloCMIS.java

2) Execute the class:

java -cp lib/chemistry-opencmis-client-api-0.13.0.jar:lib/chemistry-opencmis-client-bindings-0.13.0.jar:lib/chemistry-opencmis-client-impl-0.13.0.jar:lib/chemistry-opencmis-commons-api-0.13.0.jar:lib/chemistry-opencmis-commons-impl-0.13.0.jar:lib/commons-logging-1.1.1.jar:lib/slf4j-api-1.7.12.jar:lib/slf4j-simple-1.7.12.jar:lib/stax2-api-3.1.4.jar:lib/woodstox-core-asl-4.4.0.jar:. DotCMSHelloCMIS


Both the source file DotCMSHelloCMIS.java and all needed libraries are included 
here. The .JAR files are under the "/lib" folder.


FOR WINDOWS:
For the "javac" and "java" commands, remember to change the ":" (colon) 
character to ";" (semi-colon) as Windows uses a different separator character.