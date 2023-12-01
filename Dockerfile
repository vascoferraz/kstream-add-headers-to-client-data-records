FROM adoptopenjdk/openjdk11:latest

ENV DEBIAN_FRONTEND=noninteractive

# Update the package index, install the specified packages and remove the package lists
RUN apt-get update \
  && apt-get install -y krb5-user libpam-krb5 libpam-ccreds \
  && rm -rf /var/lib/apt/lists/*

# Add Kafka certificate to cacerts (Java truststore)
COPY certificates/server.pem /opt/java/openjdk/lib/security/server.pem
RUN keytool -import -trustcacerts -keystore /opt/java/openjdk/lib/security/cacerts \
       -storepass changeit -noprompt -alias kafka -file /opt/java/openjdk/lib/security/server.pem
RUN rm /opt/java/openjdk/lib/security/server.pem

# Update SSL CA certificates
RUN update-ca-certificates

# Copy all JAR files into the pod and adds executable permissions
COPY /target/*.jar /kstream/
RUN chmod +x /kstream/*.jar

# Create a new group named 'appuser'
RUN addgroup --gid 1000 appuser

# Create a new user named 'appuser'
RUN useradd --uid 1000 --gid 1000 --comment "appuser" -ms /bin/bash appuser

# Change ownership of the /kstream directory recursively
RUN chown -R appuser:appuser /kstream

# Set the user's home directory
#USER appuser
#WORKDIR /home/appuser