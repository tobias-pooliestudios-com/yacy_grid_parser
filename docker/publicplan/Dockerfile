FROM gradle:5.4-jdk8-alpine as builder
MAINTAINER publicplan GmbH
LABEL MAINTAINER="publicplan GmbH"

COPY --chown=gradle:gradle ./ /app/yacy_grid_parser
WORKDIR /app/yacy_grid_parser

RUN gradle build --info && \
    mkdir build/distributions/app && \
    unzip build/distributions/*.zip -d build/distributions/app && \
    mkdir data/parser-8500/conf/ -p && \
    cp -rf conf/* data/parser-8500/conf/

FROM openjdk:8-alpine as runner

COPY --from=builder /app/yacy_grid_parser/build/distributions/app/* /app
COPY --from=builder /app/yacy_grid_parser/data /app/data

WORKDIR /app
EXPOSE 8500
ENTRYPOINT ["/bin/sh"]
CMD ["bin/yacy_grid_parser"]
