# file-engine
Run `mvn clean install` to build

Run `docker build . -t fileengine:latest` to build the docker image

Run `docker run -d --name fileengine -p 8081:8680  -v /home/ubuntu/images:/opt/jonasjschreiber/fileengine/images fileengine:latest` to run the docker image