FROM eclipse-temurin:21-jdk as build

COPY . .

RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

RUN ./mvnw clean install

FROM eclipse-temurin:21-jdk-alpine

COPY --from=build ./target/NoteHub-1.5.0.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]