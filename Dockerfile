## yacy_grid_parser dockerfile
## examples:
# docker build -t yacy_grid_parser .
# docker run -d --rm -p 8500:8500 --name yacy_grid_parser yacy_grid_parser
## Check if the service is running:
# curl http://localhost:8500/yacy/grid/mcp/info/status.json

# build app
FROM adoptopenjdk/openjdk8:alpine AS appbuilder

COPY ./ /app

WORKDIR /app

RUN ./gradlew assemble

# build dist
FROM adoptopenjdk/openjdk8:alpine

LABEL maintainer="Michael Peter Christen <mc@yacy.net>"
ENV JAVA_OPTS ""

COPY ./conf /app/conf/
COPY --from=appbuilder /app/build/libs/ ./app/build/libs/

WORKDIR /app

EXPOSE 8500

ENTRYPOINT ["/bin/sh"]
CMD ["-c", "java $JAVA_OPTS -jar /app/build/libs/yacy_grid_parser-0.0.1-SNAPSHOT-all.jar"]
