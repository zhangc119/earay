Earay for micro service based on dropwizard.

1) Working with Eclipse
------------------------------------------------------

1.1) ./gradlew cleanEclipse eclipse

1.2) Download latest eclipse and create project set "earay"

1.3) import sub projects into project set "earay" , such as "base", "bigdata/bigtop"...

2) Working with one sub project (except framework "base")
------------------------------------------------------

2.1) ./gradlew -p bigdata/bigtop clean hadoopFatCapsule

2.2) java -jar bigdata/bigtop/build/libs/earay-bigtop-0.1-SNAPSHOT-capsule.jar server bigdata/bigtop/bigtop.yml

3) Working with combination of multiple projects
------------------------------------------------------

3.1) for all sub projects under "bigdata" : ./gradlew -p base -Papps=bigdata clean earayThinCapsule

3.2) for one sub project under "bigdata" : ./gradlew -p base -Papps=bigdata:bigtop clean earayThinCapsule

3.3) combine more by format "./gradlew -p base -Papps=<project1>,<project2>,...,<projectN> clean earayThinCapsule", here 'project?' represents either parent one or child one in 3.1/3.2

3.4) java -jar base/build/libs/earay-base-0.1-SNAPSHOT-capsule.jar server bigdata/bigtop/bigtop.yml
