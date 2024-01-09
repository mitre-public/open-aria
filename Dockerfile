# ----------------------------------------
FROM eclipse-temurin:17-jre-ubi9-minimal

RUN mkdir /app
RUN mkdir /config

COPY open-aria-deploy/target/OpenARIA.jar /app
COPY config/* /config

EXPOSE 9092

# Hard code the command docker runs on launch
#   Command = "java -cp OpenARIA.jar org.mitre.openaria.RunAirborneOnKafkaNop /config/demoConfig.yaml"
#   Upside = Fewer moving parts, exactly 1 main method and exactly 1 configuration approach
#   Downside = MUST rebuild the image for ANY change in configuration

CMD ["java", "-cp", "/app/OpenARIA.jar", "org.mitre.openaria.RunAirborneOnKafkaNop", "/config/demoConfig.yaml"]


