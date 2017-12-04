FROM ubuntu:latest
MAINTAINER publicplan GmbH

# Update
RUN apt-get update
RUN apt-get upgrade -y

# add packages
RUN apt-get install -y git openjdk-8-jdk

#install gradle required for build
RUN apt-get update && apt-get install -y software-properties-common
RUN add-apt-repository ppa:cwchien/gradle
RUN apt-get update
RUN apt-get install -y wget
RUN wget https://services.gradle.org/distributions/gradle-3.4.1-bin.zip
RUN mkdir /opt/gradle
RUN apt-get install -y unzip
RUN unzip -d /opt/gradle gradle-3.4.1-bin.zip
RUN PATH=$PATH:/opt/gradle/gradle-3.4.1/bin
ENV GRADLE_HOME=/opt/gradle/gradle-3.4.1
ENV PATH=$PATH:$GRADLE_HOME/bin
RUN gradle -v

#RUN git clone https://github.com/publicplan/yacy_grid_parser.git --recursive
ADD ./ /yacy_grid_parser
WORKDIR /yacy_grid_parser


# compile
RUN gradle build
RUN mkdir data/parser-8500/conf/ -p
RUN cp docker/config-parser.properties data/parser-8500/conf/config.properties
RUN chmod +x ./docker/start.sh
# Expose web interface ports
EXPOSE 8500

# Define default command.
ENTRYPOINT ["/bin/bash", "./docker/start.sh"]
