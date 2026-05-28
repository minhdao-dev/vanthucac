FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x mvnw
RUN ./mvnw -B dependency:go-offline

COPY src src

RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN groupadd --system vanthucac && useradd --system --gid vanthucac vanthucac

COPY --from=build /app/target/*.jar app.jar

RUN chown -R vanthucac:vanthucac /app

USER vanthucac

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]