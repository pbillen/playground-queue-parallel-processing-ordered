FROM openjdk:8-jre-alpine
RUN wget http://archive.apache.org/dist/activemq/5.15.8/apache-activemq-5.15.8-bin.tar.gz -O - | tar -xz
CMD ["apache-activemq-5.15.8/bin/activemq", "console"]
