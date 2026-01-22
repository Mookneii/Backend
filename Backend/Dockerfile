FROM eclipse-temurin:17-jdk

WORKDIR /app
COPY . .

# go into your real project folder (case-sensitive)
WORKDIR /app/Backend

RUN mkdir -p bin \
 && find src -name "*.java" > sources.txt \
 && javac -cp "lib/*" -d bin @sources.txt

ENV PORT=8080
CMD ["java", "-cp", "bin:lib/*", "controller.ProductController"]
