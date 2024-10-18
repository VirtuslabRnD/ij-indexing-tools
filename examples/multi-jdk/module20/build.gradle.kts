plugins {
    java
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(20))
    }
}

application {
    mainClass = "Main"
}