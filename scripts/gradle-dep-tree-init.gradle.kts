// A more flexible dependency tree generator.
// See: https://github.com/jfrog/gradle-dep-tree

initscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.jfrog:gradle-dep-tree:+")
    }
}

allprojects {
    apply<com.jfrog.GradleDepTree>()
}
