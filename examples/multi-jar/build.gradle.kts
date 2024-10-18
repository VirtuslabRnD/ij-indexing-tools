plugins {
    id("java")
    kotlin("jvm")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-client-core-jvm:2.3.12")
    implementation("io.ktor:ktor-server-netty:2.3.12")
    implementation("org.springframework.boot:spring-boot-starter-web:2.7.16")
    implementation("org.hibernate:hibernate-core:5.6.15.Final")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.google.guava:guava:30.1.1-jre")
    implementation("org.apache.commons:commons-lang3:3.13.0")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.2.1")
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.mockito:mockito-core:4.11.0")
    implementation("org.junit.jupiter:junit-jupiter-api:5.8.2")

    implementation("com.amazonaws:aws-java-sdk-bom:1.12.565")
    implementation("com.amazonaws:aws-java-sdk-s3:1.12.565")
    implementation("org.apache.camel:camel-core:3.14.5")
    implementation("org.elasticsearch.client:elasticsearch-rest-high-level-client:7.17.10")
    implementation("org.apache.tika:tika-parsers:1.28.5")
    implementation("org.apache.poi:poi-ooxml:5.2.3")
    implementation("org.springframework.batch:spring-batch-core:4.3.8")
    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("org.apache.kafka:kafka-clients:2.8.2")
    implementation("org.mybatis:mybatis:3.5.10")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("org.redisson:redisson:3.17.7")
    implementation("net.sf.ehcache:ehcache:2.10.9.2")
    implementation("org.apache.zookeeper:zookeeper:3.7.1")
    implementation("org.apache.hbase:hbase-client:2.4.13")
    implementation("org.apache.hadoop:hadoop-common:3.3.4")
    implementation("org.apache.spark:spark-core_2.12:3.2.3")
    implementation("org.neo4j.driver:neo4j-java-driver:4.4.13")
    implementation("com.rabbitmq:amqp-client:5.14.2")
    implementation("org.apache.activemq:activemq-all:5.17.5")
    implementation("io.netty:netty-all:4.1.97.Final")
    implementation("org.apache.httpcomponents:httpclient:4.5.14")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("commons-io:commons-io:2.13.0")
    implementation("org.jsoup:jsoup:1.16.1")
    implementation("org.bytedeco:javacv:1.5.8")
}

tasks.register("listJars") {
    doLast {
        configurations.compileClasspath.get().forEach { file ->
            println(file.absolutePath)
        }
    }
}

kotlin {
    jvmToolchain(11)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}
