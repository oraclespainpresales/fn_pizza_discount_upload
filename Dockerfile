FROM fnproject/fn-java-fdk-build:jdk11-1.0.104 as build-stage
WORKDIR /function
ENV MAVEN_OPTS -Dhttp.proxyHost= -Dhttp.proxyPort= -Dhttps.proxyHost= -Dhttps.proxyPort= -Dhttp.nonProxyHosts= -Dmaven.repo.local=/usr/share/maven/ref/repository

ADD pom.xml /function/pom.xml
RUN ["mvn", "package", "dependency:copy-dependencies", "-DincludeScope=runtime", "-DskipTests=true", "-Dmdep.prependGroupId=true", "-DoutputDirectory=target", "--fail-never"]

ADD src /function/src
RUN ["mvn", "package", "-DskipTests=true"]

FROM fnproject/fn-java-fdk:jre11-1.0.104
WORKDIR /function
COPY --from=build-stage /function/target/*.jar /function/app/

CMD ["com.example.fn.UploadDiscountCampaigns::handleRequest"]