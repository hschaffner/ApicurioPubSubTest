# Confluent Publisher/Consumer using Apicurio Registry to Manage the Schemas

Confluent Schema Registry provides a robust, industrial-strength registry for managing message schemas. Over the past several years 
there have been migrations from other Kafka implementations where they were using Apicurio as the 
registry for the Kafka applications. One of the most common registry was found to be the 
Open Source Apicurio Registry. 

We have prepared a comparison document that compares Confluent Schema Registry and Apicurio registry. For Confluent employees, this document
can be found at:

https://docs.google.com/document/d/1TPVKHAIzN7ZPynVxKyvpqSJVmoJ2FCCTQU4tLzjGXJY/edit?usp=sharing

This document also explains a lot of the benefits and use cases for using a registry to store schemas. This project is
quoted and referenced in the document. This is a Spring Boot sample that produces and consumes Avro messages against Confluent Cloud,
 however, the Apicurio Registry is used in place of Confluent Schema Registry. 

This project assumes that Apicurio Registry is installed as a local Docker Container with registry access via localhost.

The project is compiled using:

**mvn clean package**

In the pom.xml file you will notice that there is a Maven Plugin that automatically registers
the project Avro schemas with Apicurio registry. 

You can start the producer with:

**maven spring-boot:run**

The automatically creates a REST POST consumer that takes the REST message and uses it to post to Confluent Cloud. You can find an example of the 
"curl" command and a sample JSON message that is used to post data to Confluent in the
"PostRestData.sh" script.

The consumer can be started by changing to the "ApicurioConsumer" directory and again running:

**mvn spring-boot:run**

The consumer application makes use of Avro Specific client deserialization, and therefore is deserialized into a getter/setter POJO that is created automatically by 
the Maven Avro Plugin. The plugin can be manually executed using:

**mvn generate-sources**

The consumer code also shows how to make use of the Generic Avro format (it is commented out in the source code).

The publisher "ConfluentSession.java" makes use of the Generic Avro format to publish the Kafka record.

