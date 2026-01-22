# Use Java 17 (safe for Render)
FROM eclipse-temurin:17-jdk

# Set working directory
WORKDIR /app

# Copy everything
COPY . .

# Compile Java source
RUN mkdir -p bin \
 && find src -name "*.java" > sources.txt \
 && javac -cp "lib/*" -d bin @sources.txt

# Render provides PORT automatically
ENV PORT=8080

# Start server (CHANGE Main if needed)
CMD ["java", "-cp", "bin:lib/*", "controller.ProductController"]

