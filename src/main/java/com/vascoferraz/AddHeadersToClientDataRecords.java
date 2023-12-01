package com.vascoferraz;

import com.vascoferraz.processors.HeaderProcessorSupplier;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class AddHeadersToClientDataRecords {

    private StreamsBuilder builder;
    private KStream<ClientDataKey, ClientDataValue> clientDataIn;
    final String inputTopic= "input_records_without_headers";
    final String outputTopic = "output_records_with_headers";

    protected Properties buildStreamsProperties() {
        Properties props = new Properties();

        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "kstream-add-headers-to-client-data-records");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka.confluent.svc.cluster.local:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "https://schemaregistry.confluent.svc.cluster.local:8081");
        props.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, true);
        props.put("avro.remove.java.properties", "true");
        props.put("basic.auth.credentials.source", "USER_INFO");
        props.put("basic.auth.user.info", "sr:sr-secret");
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        props.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"kafka\" password=\"kafka-secret\";");

        return props;
    }

    protected static Map<String, String> getSerdeConfig() {
        final HashMap<String, String> map = new HashMap<>();
        map.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "https://schemaregistry.confluent.svc.cluster.local:8081");
        map.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, "true");
        map.put("avro.remove.java.properties", "true");
        map.put("basic.auth.credentials.source", "USER_INFO");
        map.put("basic.auth.user.info", "sr:sr-secret");

        return map;
    }

    public static SpecificAvroSerde<ClientDataKey> getClientDataKeySerde() {
        SpecificAvroSerde<ClientDataKey> serde = new SpecificAvroSerde<>();
        serde.configure(getSerdeConfig(), true);
        return serde;
    }

    public static SpecificAvroSerde<ClientDataValue> getClientDataValueSerde() {
        SpecificAvroSerde<ClientDataValue> serde = new SpecificAvroSerde<>();
        serde.configure(getSerdeConfig(), false);
        return serde;
    }


    public Topology buildTopology() {

        builder = new StreamsBuilder();

        loadClientDataTopics();

        KStream<ClientDataKey, ClientDataValue> clientDataOut = processClientData();

        clientDataOut.to(outputTopic, Produced.with(getClientDataKeySerde(), getClientDataValueSerde()));

        return builder.build();
    }


    private void loadClientDataTopics() {

        clientDataIn = builder.stream(inputTopic, Consumed
                        .<ClientDataKey, ClientDataValue>with(getClientDataKeySerde(), getClientDataValueSerde())
                        .withName("ClientData")
        );
    }

    private KStream<ClientDataKey, ClientDataValue> processClientData() {
        // Split the stream into two branches based on odd and even ages
        KStream<ClientDataKey, ClientDataValue>[] branches = clientDataIn
                .map((k, v) -> new KeyValue<>(new ClientDataKey(k.getId()), v))
                .branch(
                        (key, value) -> value.getAge() % 2 != 0, // Odd age
                        (key, value) -> value.getAge() % 2 == 0  // Even age
                );

        // Process for odd ages
        branches[0]
                .process(new HeaderProcessorSupplier("AgeIsOdd", "true"));

        // Process for even ages
        branches[1]
                .process(new HeaderProcessorSupplier("AgeIsEven", "true"));

        // Merge the two branches back into a single stream
        return branches[0].merge(branches[1]);
    }

    private void run() {
        Properties streamProps = this.buildStreamsProperties();
        Topology topology = this.buildTopology();

        final KafkaStreams streams = new KafkaStreams(topology, streamProps);
        final CountDownLatch latch = new CountDownLatch(1);

        // Attach shutdown handler to catch Control-C.
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close(Duration.ofSeconds(5));
                latch.countDown();
            }
        });

        try {
            streams.cleanUp();
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }

        System.exit(0);
    }

    public static void main(String[] args) {
        new AddHeadersToClientDataRecords().run();
    }
}