## Introduction
The purpose of this KStream is to add headers to Kafka records that hold client data using the Processor and ProcessorSupplier interfaces. The schemas for the [key](src/main/avro/client-data-key.avsc) and [value](src/main/avro/client-data-value.avsc) are both Avro and very simple, consisting of just four fields: `id`, `name`, `age`, and `email`.

The input topic contains records without any headers. The KStream, through the Processor and ProcessorSupplier interfaces, adds headers based on the age. If the age is odd, the following header is added to the record: `headerKey: AgeIsOdd`, `headerValue: true`. If the age is even, the following header is added to the record: `headerKey: AgeIsEven`, `headerValue: true`.

## Hardware requirements
The following steps were taken on a MacBook Pro (M2 chip) with 32GB of memory and running macOS 14.1.2

## Software requirements
- Kafka cluster (to speed up the setup, use this [Kafka cluster](https://github.com/vascoferraz/kafka-production-secure-deploy-with-kubernetes)).
- Docker Desktop (4.25.2 or higher)
- Apache Maven (3.9.5 or higher)

## Clone the repository
```sh
git clone https://github.com/vascoferraz/kstream-add-headers-to-client-data-records
```

## Note about SSL certificate
This KStream is configured to use an encrypted SSL communication with the Kafka cluster. If you are using this [Kafka cluster](https://github.com/vascoferraz/kafka-production-secure-deploy-with-kubernetes) just replace the KStream certificate, located [here](certificates/server.pem) with the one generated by the Kafka cluster located in ./certificates/generated/server.pem

## Build the JAR file
```sh
brew install maven
mvn clean package
```

## Change Kubernetes namespace
If you are using the previously referred [Kafka cluster](https://github.com/vascoferraz/kafka-production-secure-deploy-with-kubernetes), you should deploy this KStream in the following namespace: `confluent`.
```sh
kubectl config set-context --current --namespace=confluent
```

## Create input and output topic
```sh
kubectl exec -it kafka-0 -c kafka -- kafka-topics --create --bootstrap-server kafka.confluent.svc.cluster.local:9092 --command-config /opt/confluentinc/etc/kafka/kafka.properties --topic input_records_without_headers --replication-factor 3 --partitions 3
```
```sh
kubectl exec -it kafka-0 -c kafka -- kafka-topics --create --bootstrap-server kafka.confluent.svc.cluster.local:9092 --command-config /opt/confluentinc/etc/kafka/kafka.properties --topic output_records_with_headers --replication-factor 3 --partitions 3
```

## Create schemas for the input and the output topic
Currently, the KStream is configured to auto-register schemas meaning that there is no need to register the schemas manually.

## Build docker image
```sh
export TUTORIAL_HOME=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
docker build -t adoptopenjdk/openjdk11-vf:latest --progress=plain -f "${TUTORIAL_HOME}/Dockerfile" "${TUTORIAL_HOME}"
```

## Deploy the container
```sh
kubectl apply -f ${TUTORIAL_HOME}/manifests/kstream-add-headers-to-client-data-records-deployment.yaml
```

## Enter the Schema Registry pod
```sh
kubectl exec -it schemaregistry-0 -c schemaregistry -- bash
```

## Create a producer in the Schema Registry pod
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
```sh
{"id": "12345"}£{"id": "12345", "name": {"string":"Brune Wayne"}, "age": {"int":28}, "email": {"string":"bruce.wayne@wayne-enterprises.com"}}
```
## Send a record with an odd age
```sh
{"id": "12345"}£{"id": "12345", "name": {"string":"Lucius Fox"}, "age": {"int":61}, "email": {"string":"lucius.fox@wayne-enterprises.com"}}
```
## Create a consumer
```sh
kafka-avro-console-consumer \
--bootstrap-server kafka.confluent.svc.cluster.local:9092 \
--topic output_records_with_headers \
--property schema.registry.url=https://schemaregistry.confluent.svc.cluster.local:8081 \
--property basic.auth.credentials.source=USER_INFO \
--property schema.registry.basic.auth.user.info=sr:sr-secret \
--property print.key=true \
--property parse.headers=true \
--consumer-property security.protocol=SASL_SSL \
--consumer-property sasl.mechanism=PLAIN \
--consumer-property sasl.jaas.config="org.apache.kafka.common.security.plain.PlainLoginModule required username="kafka" password="kafka-secret";" \
--from-beginning
```
