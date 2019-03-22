FROM gradle:4.10-jdk8-alpine as builder
MAINTAINER publicplan GmbH
LABEL MAINTAINER="publicplan GmbH"

COPY --chown=gradle:gradle ./ /app/yacy_grid_parser
WORKDIR /app/yacy_grid_parser

RUN gradle shadowJar --info && \
    mkdir data/parser-8500/conf/ -p && \
    cp -rf conf/* data/parser-8500/conf/

FROM openjdk:8-alpine as runner

WORKDIR /app

COPY --from=builder /app/yacy_grid_parser/build/libs/*.jar /app/yacy-grid-parser.jar
COPY --from=builder /app/yacy_grid_parser/data /app/data

EXPOSE 8100
ENTRYPOINT ["java"]
CMD ["-jar", "yacy-grid-parser.jar"]
