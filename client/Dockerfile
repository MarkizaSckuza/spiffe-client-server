FROM openjdk:11
COPY target/client-jar-with-dependencies.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar", "10.99.130.147", "spiffe://example.org/ns/default/sa/server"]