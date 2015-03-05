Earay for micro service based on dropwizard.

1) Working with Eclipse
------------------------------------------------------

1.1) ./gradlew cleanEclipse eclipse

1.2) Download latest eclipse and create project set "earay"

1.3) import sub projects into project set "earay" , such as "base", "bigdata/bigtop"...

2) Working with one sub project (except framework "base")
------------------------------------------------------

2.1) edit bigdata/bigtop/bigtop.yml by setting included projects, comment out lines "  - class: earay.serengeti.cli.SerengetiCLIProject" and "- class: earay.serengeti.install.SerengetiInstallProject" for sub project bigdata/bigtop

2.2) ./gradlew -p bigdata/bigtop run -PrunSpec="bigtop.yml"

3) Working with combination of multiple projects
------------------------------------------------------

3.1) edit bigdata/bigtop/bigtop.yml by setting combined projects

3.2) for all sub projects under "bigdata" : ./gradlew -p base -Papps=bigdata run -Pargs="server ../bigdata/bigtop/bigtop.yml"

3.2) edit bigdata/bigtop/bigtop.yml, run application with one sub project under "bigdata" and all sub projects under "serengeti" : ./gradlew -p base -Papps=bigdata:bigtop,serengeti run -PrunSpec="../bigdata/bigtop/bigtop.yml"

4) Working with docker
------------------------------------------------------

4.1) https://github.com/bmuschko/gradle-docker-plugin is used in Earay for docker image/container. Please set docker instance in build.gradle first , "docker.url" and "docker.certPath"

4.2) create docker image : ./gradlew -p base -Papps=bigdata:bigtop,serengeti -PrunSpec="../bigdata/bigtop/bigtop.yml" clean inspectImage  

4.3) on docker instance, run "docker run -p 8083:8083 <image_id returned in #4.2>", then you can access the application from http://<docker_instance_ip>:8083
