# Camel + DFDL demo

Simple demo showing how to use Apache Daffodil library with Apache Camel.

## Running  on your machine through CLI

To run this demo as a standalone project on your local machine:

* Build the project:

    $ mvn clean package
    
* Run the service:

    $ mvn spring-boot:run

## Testing 

Sample data is provided in the src/main/resource folder

### Parsing

	curl -d @msg.dat -H 'Content-Type: text/plain' http://localhost:8080/v1/xml
		
### Unparsing

	curl -d @msg.xml -H 'Content-Type: application/xml' http://localhost:8080/v1/unparse		