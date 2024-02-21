package io.confluent.heinz.apicuriokafkapubsubtest.apicurioconsumer;

import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.client.RegistryClientFactory;
import io.apicurio.registry.rest.client.exception.NotFoundException;
import io.apicurio.registry.serde.SerdeConfig;
import io.apicurio.registry.serde.avro.AvroKafkaDeserializer;

import io.confluent.heinz.avroMsg;
import io.confluent.heinz.avroMsgK;
import org.apache.avro.Schema;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.consumer.*;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;






@Component
public class ApiCurioConsumer {
    //Environment _env;
    private static final String REGISTRY_URL = "http://localhost:8080/apis/registry/v2";
    private final Log logger = LogFactory.getLog(ApiCurioConsumer.class);
    private KafkaConsumer consumer;

    private static String toString(InputStream data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buff = new byte[64];
        int count;
        while ((count = data.read(buff)) != -1) {
            baos.write(buff, 0, count);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    public ApiCurioConsumer(Environment env){
        logger.info("Check for brokers: " + env.getProperty("bootstrap.servers"));
        createKafkaConsumer(env);
    }

    public void createKafkaConsumer(Environment env) {
        AtomicBoolean running = new AtomicBoolean(true);

        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", env.getProperty("bootstrap.servers"));
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, env.getProperty("publisher.topic"));
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, AvroKafkaDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, AvroKafkaDeserializer.class.getName());
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
        properties.setProperty("enable.idempotence", env.getProperty("enable.idempotence"));
        properties.setProperty("apicurio.registry.use-specific-avro-reader", "true");
        properties.setProperty("apicurio.registry.avro-datum-provider","io.apicurio.registry.serde.avro.DefaultAvroDatumProvider");




        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Stopping Consumer");
            running.set(false);
        }));


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

        Schema schemaV = new Schema.Parser().parse(schemaDataV);
        Schema schemaK = new Schema.Parser().parse(schemaDataK);

        System.out.println("================= Key Schema from Apicurio: \n" + schemaK.toString());
        System.out.println("================= Value Schema from Apicurio: \n" + schemaV.toString());





        /*
        //Below makes use of the Generic Record Avro format, to operate you must comment out from above the line:
        // properties.setProperty("apicurio.registry.use-specific-avro-reader", "true");
        try (final Consumer<GenericRecord, GenericRecord> consumer = new KafkaConsumer<GenericRecord, GenericRecord>(properties)) {
            consumer.subscribe(Arrays.asList(env.getProperty("publisher.topic")));

            while (running.get()) {
                ConsumerRecords<GenericRecord, GenericRecord> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<GenericRecord, GenericRecord> record : records) {
                    System.out.println("++++++++++++++++++++++++++++++++++++++++++++++");
                    System.out.println("Value: " + record.value().toString());
                    System.out.println("Key: " + record.key().toString());

                    System.out.println("first name : " + record.value().get("first_name"));


                }
            }

        }



         */


        //Using Specific Avro format
        try (final Consumer<avroMsgK, avroMsg> consumer = new KafkaConsumer<avroMsgK, avroMsg>(properties)) {
            consumer.subscribe(Collections.singletonList(env.getProperty("publisher.topic")));

            while (running.get()) {
                ConsumerRecords<avroMsgK, avroMsg> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<avroMsgK, avroMsg> record : records) {
                    System.out.println("++++++++++++++++++++++++++++++++++++++++++++++");
                    avroMsgK avromsgK = record.key();
                    avroMsg avromsg = record.value();
                    System.out.println("Key: " + avromsgK.toString());
                    System.out.println("Value: " + avromsg.toString());
                    System.out.println("LastName in Value: " + avromsg.getLastName());
                    System.out.println("Schema: " + avromsgK.getSchema().toString(true));

                }
            }
        }
    }
}
