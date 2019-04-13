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

cd summerframework-common        && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-business      && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-configcenter  && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-es            && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-jobcenter     && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-jpa           && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-mapping       && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-mybatis       && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-rabbit        && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-redis         && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-webapi        && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-eureka        && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-openfeign     && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-monitor       && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-web           && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-dts           && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..