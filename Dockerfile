
FROM eclipse-temurin:22.0.2_9-jdk-alpine
RUN apk add bash
WORKDIR /app

COPY install-sbt.sh /app/

RUN chmod +x /app/install-sbt.sh && /app/install-sbt.sh

COPY build.sbt build.sbt

COPY . .

RUN /app/sbt/bin/sbt compile

EXPOSE 9000

CMD ["/app/sbt/bin/sbt", "run"]