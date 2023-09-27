FROM openjdk:11
COPY target/server-jar-with-dependencies.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar", "spiffe://example.org/ns/default/sa/client"]