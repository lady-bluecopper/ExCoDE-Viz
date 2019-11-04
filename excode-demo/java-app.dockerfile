# Alpine Linux with OpenJDK JRE
FROM openjdk:8-jre-alpine

RUN mkdir -p /javapp
WORKDIR /javapp
COPY ./javapp /javapp

# run application with this command line 
CMD ["/usr/bin/java", "-jar", "-Xmx5g", "-XX:-UseGCOverheadLimit", "-XX:+UseConcMarkSweepGC", "-XX:+UseParNewGC", "-XX:NewSize=6g", "-XX:+CMSParallelRemarkEnabled", "-XX:+ParallelRefProcEnabled", "-XX:+CMSClassUnloadingEnabled", "/javapp/target/DenCE-1.0-jar-with-dependencies.jar", "eu.unitn.disi.db.dence.webserver.Laucher"]
