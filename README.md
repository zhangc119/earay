Earay for micro service based on dropwizard.

1) Working with Eclipse
------------------------------------------------------

1.1) ./gradlew cleanEclipse eclipse

1.2) Download latest eclipse and create project set "earay"

1.3) import sub projects into project set "earay" , such as "base", "bigdata/bigtop"...

2) Working with one sub project (except framework "base")
------------------------------------------------------

2.1) edit bigdata/bigtop/bigtop.yml by setting included projects, comment out lines "  - class: earay.serengeti.cli.SerengetiCLIProject" and "- class: earay.serengeti.install.SerengetiInstallProject" for sub project bigdata/bigtop

2.2) ./gradlew -p bigdata/bigtop run -Pargs="server bigtop.yml"

3) Working with combination of multiple projects
------------------------------------------------------

3.1) edit bigdata/bigtop/bigtop.yml by setting combined projects

3.2) for all sub projects under "bigdata" : ./gradlew -p base -Papps=bigdata run -Pargs="server ../bigdata/bigtop/bigtop.yml"

3.2) edit bigdata/bigtop/bigtop.yml, run application with one sub project under "bigdata" and all sub projects under "serengeti" : ./gradlew -p base -Papps=bigdata:bigtop,serengeti run -Pargs="server ../bigdata/bigtop/bigtop.yml"
