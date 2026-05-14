@echo off
call mvn --quiet -DskipTests=true clean package
call mvn --quiet exec:java -Dexec.mainClass="safbuilder.cli.BatchProcess" -Dexec.args="%*"
