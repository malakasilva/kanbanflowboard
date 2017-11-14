FROM tomcat:8.0.20-jre8

COPY mvn/web-dashboard/kabana-dashboard/target/*.war /usr/local/tomcat/webapps/

COPY mvn/java-sync/target/dependency-jars/mysql-connector-java-5.1.6.jar /usr/local/tomcat/lib/

RUN mkdir -p /usr/local/gitsync

COPY mvn/java-sync/target/git-sync-1.0.0-SNAPSHOT.jar /usr/local/gitsync

COPY mvn/java-sync/target/config.properties /usr/local/gitsync

WORKDIR /usr/local/gitsync

CMD ["java","-jar","git-sync-1.0.0-SNAPSHOT.jar"]

WORKDIR /usr/local/tomcat/bin

EXPOSE 8080
CMD ["catalina.sh", "run"]
