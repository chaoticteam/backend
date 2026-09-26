# Etapa de compilación con Maven y JDK 17 en Alpine
FROM eclipse-temurin:17-jdk-alpine AS build
# Instalar Maven en Alpine
RUN apk add --no-cache maven
# Establecer directorio de trabajo
WORKDIR /app
# Copiar archivos del proyecto
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
# Compilar la aplicación
RUN mvn package -DskipTests
# Etapa final: solo JDK + JAR para ejecución
FROM eclipse-temurin:17-jdk-alpine
# Versión de la imagen (la pipeline pasa la de GitVersion); GET /api/version responde "v<VERSION>"
ARG VERSION=1.0
ENV VERSION=$VERSION
WORKDIR /app
# Copiar el JAR desde la etapa de compilación
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
# Exponer el puerto de la aplicación
EXPOSE 8080
# Ejecutar la aplicación
CMD ["java", "-jar", "app.jar"]
