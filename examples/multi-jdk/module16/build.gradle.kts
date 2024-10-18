plugins {
    java
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(16))
    }
}

application {
    mainClass = "Main"
}