plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation("ai.koog:koog-agents:0.1.0")
    implementation("ai.koog:code-prompt-executor-grazie-for-koog:1.0.0-beta.63+0.4.62")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    testImplementation(kotlin("test"))
}
