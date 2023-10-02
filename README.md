# Apache Camel FHIR to Mongo example

## Introduction

This example will read patients from FHIR server URL page by page and store them in MongoDB using Apache Camel route.

You can also specify path to JS file with function to mutate the data before storing.
Example of JS file is located at `src/main/resources/js/mapper.js`

The Camel route is located in the `FHIRToMongoRoute` class.

Please fill in your MongoDB credentials in `application.properties` before build.

By default, the example uses `https://api.logicahealth.org/HevelianTestSandbox/open` as the FHIR server URL.
However, you can edit the `application.properties` file to override the defaults.

## Build

You can build this example using:

```bash
mvn package
```

## Run

You can run this example using:

```bash
java -jar target/fhir-to-mongo-migration-0.0.1.jar
```

or you can specify JS file location

```bash
java -jar target/fhir-to-mongo-migration-0.0.1.jar --javascriptFileLocation=c:/script/mapper.js
```