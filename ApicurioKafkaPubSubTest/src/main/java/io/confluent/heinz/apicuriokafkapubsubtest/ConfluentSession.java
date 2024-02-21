package io.confluent.heinz.apicuriokafkapubsubtest;

/*
 * Copyright Confluent Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.client.RegistryClientFactory;
import io.apicurio.registry.rest.client.exception.NotFoundException;
import io.apicurio.registry.serde.SerdeConfig;
import io.apicurio.registry.serde.avro.AvroKafkaSerializer;
import io.confluent.heinz.avroMsg;
import io.confluent.heinz.avroMsgK;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.errors.SerializationException;

import org.springframework.core.env.Environment;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;




public class ConfluentSession {

    Environment _env;
    private static final String REGISTRY_URL = "http://localhost:8080/apis/registry/v2";
    public ConfluentSession(Environment env) {
        createKafkaSession(env);
        this._env = env;
    }

    private final Log logger = LogFactory.getLog(ConfluentSession.class);
    private int counter = 0;

    private KafkaProducer producer;

    private static String toString(InputStream data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buff = new byte[64];
        int count;
        while ((count = data.read(buff)) != -1) {
            baos.write(buff, 0, count);
        }
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }


    public void createKafkaSession(Environment env) {

        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", env.getProperty("bootstrap.servers"));
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, AvroKafkaSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroKafkaSerializer.class.getName());
        properties.putIfAbsent(SerdeConfig.REGISTRY_URL, REGISTRY_URL);
        properties.setProperty("apicurio.registry.artifact.group-id", env.getProperty("publisher.apicurio.group"));
        properties.setProperty("basic.auth.credentials.source", env.getProperty("basic.auth.credentials.source"));
        properties.setProperty("sasl.mechanism",env.getProperty("sasl.mechanism"));
        properties.setProperty("sasl.jaas.config", env.getProperty("sasl.jaas.config"));
        properties.setProperty("security.protocol", env.getProperty("security.protocol"));
        properties.setProperty("client.dns.lookup", env.getProperty("client.dns.lookup"));
        properties.setProperty("acks", "all");
        properties.setProperty("auto.create.topics.enable", "true");
        properties.setProperty("topic.creation.default.partitions", "3");
        properties.setProperty("auto.register.schema", "true");
        properties.setProperty("enable.idempotence", env.getProperty("enable.idempotence"));



        producer = new KafkaProducer<>(properties);

    }

    public void sendAvroMessage(JsonMsg jMsg) throws FileNotFoundException {

        //toggle count up and down for 0/1
        if (counter == 0){
            counter++;
        } else {
            counter=0;
        }

        RegistryClient client = RegistryClientFactory.create(REGISTRY_URL);
        String schemaDataV = null;  //avro schema for value
        String schemaDataK = null;  //avro schema for key
        try (InputStream latestArtifact = client.getLatestArtifact("HeinzGroup", "ApicurioExample-key")) {
            schemaDataK = toString(latestArtifact);

        } catch (NotFoundException | IOException e) {
            System.err.println("Schema not registered in registry.  Before running this example, please do:");
            System.err.println("  mvn io.apicurio:apicurio-registry-maven-plugin:register@register-artifact");
            System.exit(1);

        }
        try (InputStream latestArtifact = client.getLatestArtifact("HeinzGroup", "ApicurioExample-value")) {
            schemaDataV = toString(latestArtifact);

        } catch (NotFoundException | IOException e) {
            System.err.println("Schema not registered in registry.  Before running this example, please do:");
            System.err.println("  mvn io.apicurio:apicurio-registry-maven-plugin:register@register-artifact");
            System.exit(1);

        }

        System.out.println("+++++++++++++++++++++++++++");
        System.out.println("Value schema data: " + schemaDataV);
        System.out.println("Value schema data: " + schemaDataK);

        Schema schemaV = new Schema.Parser().parse(schemaDataV);
        Schema schemaK = new Schema.Parser().parse(schemaDataK);

        GenericRecord avroRecord = new GenericData.Record(schemaV);
        avroRecord.put("first_name", jMsg.getFirstName());
        avroRecord.put("last_name", jMsg.getLastName());
        avroRecord.put("customer_id", jMsg.getCustomerId() + counter);

        GenericRecord avroMsgKey = new GenericData.Record(schemaK);
        avroMsgKey.put("clientID", jMsg.getCustomerId() + counter);
        avroMsgKey.put("client", "Heinz57");






        //Simpler for avro and automatically includes the schema as part of the transmission
        /*
        avroMsg avroRecord = new avroMsg();
        avroRecord.setFirstName(jMsg.getFirstName());
        avroRecord.setLastName(jMsg.getLastName());
        avroRecord.setCustomerId(jMsg.getCustomerId() + counter);

        avroMsgK  avroMsgKey = new avroMsgK();
        avroMsgKey.setClientID(jMsg.getCustomerId() + counter);
        avroMsgKey.setClient("Heinz57");

         */



        ProducerRecord producerRecord = new ProducerRecord<Object,Object>(_env.getProperty("publisher.topic"), avroMsgKey, avroRecord);

        System.out.println("Schema: " + avroRecord.getSchema().toString(true));
        System.out.println("First: " + avroRecord.get("first_name"));
        System.out.println("Last: " + avroRecord.get("last_name"));
        System.out.println("ID: " + avroRecord.get("customer_id"));

        try {
            producer.send(producerRecord, new MyProducerCallback());
            logger.info("\nSent! Avro key: " + avroMsgKey.toString() + " \nAvro value: " + avroRecord.toString());
        } catch(SerializationException e) {
            System.out.println("Error:");
            e.printStackTrace();
        }





    }

    class MyProducerCallback implements Callback {

        private final Log logger = LogFactory.getLog(MyProducerCallback.class);

        @Override
        public void onCompletion(RecordMetadata recordMetadata, Exception e) {
            if (e != null)
                logger.info("AsynchronousProducer failed with an exception");
            else {
                logger.info("AsynchronousProducer call Success:" + "Sent to partition: " + recordMetadata.partition() + " and offset: " + recordMetadata.offset() + "\n");
            }
        }
    }

}


