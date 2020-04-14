FROM adoptopenjdk/openjdk12:latest
WORKDIR /
ADD target/javaclient/javaclient.jar /tmp/
ADD target/javaclient/lib /tmp/lib/
ADD configure.sh /tmp
EXPOSE 8080
ENTRYPOINT ["/tmp/configure.sh"]
