FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY . .
RUN ./gradlew war :eis-adapter:rar -x test --no-daemon
RUN find ~/.gradle -name "postgresql-*.jar" | head -1 | xargs -I{} cp {} /tmp/postgresql.jar

FROM quay.io/wildfly/wildfly:33.0.0.Final-jdk17
USER root

RUN mkdir -p $JBOSS_HOME/modules/org/postgresql/main
COPY --from=builder /tmp/postgresql.jar $JBOSS_HOME/modules/org/postgresql/main/postgresql.jar
COPY wildfly/module.xml $JBOSS_HOME/modules/org/postgresql/main/module.xml

COPY wildfly/configure.cli /tmp/configure.cli
RUN $JBOSS_HOME/bin/jboss-cli.sh --file=/tmp/configure.cli \
    && rm -rf $JBOSS_HOME/standalone/configuration/standalone_xml_history

COPY --from=builder /app/eis-adapter/build/libs/eis-adapter.rar $JBOSS_HOME/standalone/deployments/eis-adapter.rar
COPY --from=builder /app/build/libs/lab_1-0.0.1-SNAPSHOT.war $JBOSS_HOME/standalone/deployments/lab_1.war

RUN chown -R jboss:jboss $JBOSS_HOME/standalone $JBOSS_HOME/modules

USER jboss
EXPOSE 8080 9990