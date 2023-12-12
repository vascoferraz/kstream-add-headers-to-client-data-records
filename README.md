## Introduction
This KStream adds headers to Kafka records using the Processor and ProcessorSupplier interfaces.

## Hardware requirements
The following steps were taken on a MacBook Pro (M2 chip) with 32GB of memory and running macOS 14.1.2

## Software requirements
- Kafka cluster (to speed up, use this [Kafka cluster](https://github.com/vascoferraz/kafka-production-secure-deploy-with-kubernetes).
- Docker Desktop (4.25.2 or higher)
- Apache Maven (3.9.5 or higher)

## Clone the repository
```sh
git clone https://github.com/vascoferraz/kstream-add-headers-to-client-data-records
```

## Note about SSL certificate
TODO

## Build the JAR file
```sh
brew install maven
mvn clean package
```

## Create input and output topic
```sh
kubectl exec -it kafka-0 -c kafka -- kafka-topics --create --bootstrap-server kafka.confluent.svc.cluster.local:9092 --command-config /opt/confluentinc/etc/kafka/kafka.properties --topic input_records_without_headers --replication-factor 3 --partitions 3
kubectl exec -it kafka-0 -c kafka -- kafka-topics --create --bootstrap-server kafka.confluent.svc.cluster.local:9092 --command-config /opt/confluentinc/etc/kafka/kafka.properties --topic output_records_with_headers --replication-factor 3 --partitions 3
```

## Create schemas for the input and the output topic
Currently, the KStream is configured to auto register schemas meaning that there is no need to register schemas.

## Build docker image
```sh
export TUTORIAL_HOME=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
docker build -t adoptopenjdk/openjdk11-vf:latest --progress=plain -f "${TUTORIAL_HOME}/Dockerfile" "${TUTORIAL_HOME}"
```

## Deploy the container
```sh
kubectl apply -f ${TUTORIAL_HOME}/manifests/kstream-add-headers-to-client-data-records-deployment.yaml
```

## Create a consumer

## Save Schema Registry pod in an environment variable
TODO

## Create a producer
```sh
kafka-avro-console-producer \
--bootstrap-server kafka.confluent.svc.cluster.local:9092 \
--topic input_records_without_headers \
--property key.schema='{ "type": "record", "name": "ClientDataKey", "namespace": "com.vascoferraz", "connect.name": "com.vascoferraz.ClientDataKey", "fields": [ { "name": "id", "type": "string" } ] }' \
--property value.schema='{ "type": "record", "name": "ClientDataValue", "namespace": "com.vascoferraz", "connect.name": "com.vascoferraz.ClientDataValue", "fields": [ { "name": "id", "type": "string" }, { "name": "name", "type": ["null", "string"], "default": null }, { "name": "age", "type": ["null", "int"], "default": null }, { "name": "email", "type": ["null", "string"], "default": null } ] }' \
--property schema.registry.url=https://schemaregistry.confluent.svc.cluster.local:8081 \
--property basic.auth.credentials.source=USER_INFO \
--property schema.registry.basic.auth.user.info=sr:sr-secret \
--property parse.key=true \
--property key.separator=£ \
--producer-property security.protocol=SASL_SSL \
--producer-property sasl.mechanism=PLAIN \
--producer-property sasl.jaas.config="org.apache.kafka.common.security.plain.PlainLoginModule required username="kafka" password="kafka-secret";"
```

## Send a record with an even age
`{"id": "12345"}£{"id": "12345", "name": {"string":"John Doe"}, "age": {"int":30}, "email": {"string":"john.doe@example.com"}}`

## Send a record with an odd age
`{"id": "12345"}£{"id": "12345", "name": {"string":"John Doe"}, "age": {"int":31}, "email": {"string":"john.doe@example.com"}}`

## Check the records in the consumer
TODO
