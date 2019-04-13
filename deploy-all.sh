#!/usr/bin/env bash

# 注意顺序
#
# 1. common
# 2. business
# 3. 其它
# 4. eureka
# 5. openfeign
# 6. monitor
# 7. web
# 8. dts

cd summerframework-common        && mvn clean deploy -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-business      && mvn clean deploy -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-configcenter  && mvn clean deploy -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-es            && mvn clean deploy -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-jobcenter     && mvn clean deploy -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-jpa           && mvn clean deploy -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-mapping       && mvn clean deploy -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-mybatis       && mvn clean deploy -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-rabbit        && mvn clean deploy -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-redis         && mvn clean deploy -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-webapi        && mvn clean deploy -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-eureka        && mvn clean deploy -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-openfeign     && mvn clean deploy -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-monitor       && mvn clean deploy -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-web           && mvn clean deploy -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-dts           && mvn clean deploy -DskipTests -Dmaven.javadoc.skip=true && cd ..