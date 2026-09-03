# Stage 1: Build & Compile
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy dependencies and sources
COPY lib/ ./lib/
COPY src/ ./src/

# Compile application using bundled JARs
RUN mkdir -p bin && \
    javac -encoding UTF-8 \
    -cp "lib/*" \
    -d bin \
    src/com/todo/model/*.java \
    src/com/todo/util/*.java \
    src/com/todo/dao/*.java \
    src/com/todo/handler/*.java \
    src/com/todo/*.java

# Stage 2: Runtime Container
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy compiled classes, libraries, and static assets
COPY --from=builder /app/bin ./bin
COPY lib/ ./lib/
COPY public/ ./public/

# Port detected from Main.java
EXPOSE 8080

# Run entrypoint using Linux classpath separator (:)
CMD ["java", "-cp", "bin:lib/*", "com.todo.Main"]