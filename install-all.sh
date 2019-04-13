#!/usr/bin/env bash





cd summerframework-redis 
mvn install:install-file -Dfile=hadoop-hdfs-2.2.0-tests.jar -DgroupId=org.apache.hadoop -DartifactId=hadoop-hdfs -Dversion=2.2.0 -Dclassifier=tests -Dpackaging=jar
cd ..
cd summerframework-common        && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-configcenter  && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-mybatis       && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-rabbit        && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-redis         && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-webapi        && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-eureka        && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-openfeign     && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-monitor       && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..