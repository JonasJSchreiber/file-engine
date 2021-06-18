FROM vimalathithen/alpine-oracle-jdk8-x86:latest

RUN mkdir -p /opt/jonasjschreiber/fileengine/jars
RUN mkdir -p /opt/jonasjschreiber/fileengine/logs
ENV TZ=America/New_York
ENV JAVA_TOOL_OPTIONS="-Xms128m -Xmx1g"
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ADD target/file-engine-0.1.0.jar /opt/jonasjschreiber/fileengine/jars/file-engine-0.1.0.jar
WORKDIR /opt/jonasjschreiber/fileengine/jars
ENTRYPOINT [ "sh", "-c", "java  -Dfile.encoding=UTF-8 -Duser.dir=/opt/jonasjschreiber/fileengine/jars -Djava.security.egd=file:/dev/./urandom -jar file-engine-0.1.0.jar --logging.path=/opt/jonasjschreiber/fileengine/logs/ --logging.file=/opt/jonasjschreiber/fileengine/logs/fileengine.log --spring.config.location=classpath:application.yml" ]
