package no.nav.aap.ktor.config

import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.yaml.YamlParser


//inline fun <reified T : Any> Application.loadConfig(vararg resources: String = arrayOf("/application.yml")): T =
//    ConfigLoader.builder()
//        .addFileExtensionMapping("yml", YamlParser())
//        .addKtorConfig(environment.config)
//        .build()
//        .loadConfigOrThrow(*resources)

inline fun <reified T : Any> loadConfig(resource: String = "/application.yml"): T =
    ConfigLoader.builder()
        .addFileExtensionMapping("yml", YamlParser())
        .build()
        .loadConfigOrThrow(resource)

/**
 * Add Ktors MapApplicationConfig as PropertySource,
 * this allows the MapApplicationConfig to override config values in tests
 */
//fun ConfigLoaderBuilder.addKtorConfig(config: ApplicationConfig) = apply {
//    if (config is MapApplicationConfig) {
//        // get access to the protected property 'map'
//        @Suppress("UNCHECKED_CAST")
//        val map = config.javaClass.getDeclaredField("map").let {
//            it.isAccessible = true
//            it.get(config) as Map<String, String>
//        }
//
//        addPropertySource(MapPropertySource(map))
//    }
//}