# --- Aşama 1: derleme (JDK + Maven wrapper var) ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline
COPY src src
RUN ./mvnw -B package -DskipTests

# --- Aşama 2: çalıştırma (sadece JRE, daha küçük imaj) ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/wallet-api-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]