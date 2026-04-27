FROM eclipse-temurin:21-jdk as build

COPY . .

RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

RUN ./mvnw clean install -DskipTests

FROM alpine:3.19 AS geoip

ARG MMAI
ARG MMLK

RUN apk add --no-cache curl tar && \
    curl -fSL \
      "https://download.maxmind.com/geoip/databases/GeoLite2-City/download?suffix=tar.gz" \
      --user "${MMAI}:${MMLK}" \
      -o /tmp/geolite2.tar.gz && \
    tar -xzf /tmp/geolite2.tar.gz -C /tmp && \
    find /tmp -name "*.mmdb" -exec mv {} /geoip.mmdb \;

FROM eclipse-temurin:21-jdk-alpine

COPY --from=build ./target/NoteHub-2.2.jar app.jar
COPY --from=geoip /geoip.mmdb /opt/geoip/GeoLite2-City.mmdb

ENV GEOIP_DB=/opt/geoip/GeoLite2-City.mmdb

ENTRYPOINT ["java", "-Xms256m", "-Xmx768m", "-jar", "app.jar"]