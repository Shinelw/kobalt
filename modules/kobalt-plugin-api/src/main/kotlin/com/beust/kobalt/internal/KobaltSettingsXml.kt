package com.beust.kobalt.internal

import com.beust.kobalt.BUILD_SCRIPT_CONFIG
import com.beust.kobalt.Constants
import com.beust.kobalt.ProxyConfig
import com.beust.kobalt.homeDir
import com.beust.kobalt.misc.KFiles
import com.beust.kobalt.misc.kobaltLog
import com.google.inject.Inject
import com.google.inject.Singleton
import java.io.File
import java.io.FileInputStream
import javax.xml.bind.JAXBContext
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement

/**
 * The root element of kobalt-settings.xml
 */
@XmlRootElement(name = "kobaltSettings")
class KobaltSettingsXml {
    @XmlElement(name = "localCache") @JvmField
    var localCache: String = homeDir(KFiles.KOBALT_DOT_DIR, "cache")

    @XmlElement(name = "localMavenRepo") @JvmField
    var localMavenRepo: String = homeDir(KFiles.KOBALT_DOT_DIR, "localMavenRepo")

    @XmlElement(name = "defaulRepos") @JvmField
    var defaultRepos: DefaultReposXml? = null

    @XmlElement(name = "proxies") @JvmField
    var proxies: ProxiesXml? = null

    @XmlElement(name = "kobaltCompilerVersion") @JvmField
    var kobaltCompilerVersion: String = Constants.KOTLIN_COMPILER_VERSION

    @XmlElement(name = "kobaltCompilerRepo") @JvmField
    var kobaltCompilerRepo: String? = null

    @XmlElement(name = "kobaltCompilerFlags") @JvmField
    var kobaltCompilerFlags: String? = null

    @XmlElement(name = "kobaltCompilerSeparateProcess") @JvmField
    var kobaltCompilerSeparateProcess: Boolean = false

    @XmlElement(name = "autoUpdate") @JvmField
    var autoUpdate: Boolean = false
}

class ProxiesXml {
    @XmlElement @JvmField
    var proxy: List<ProxyXml> = arrayListOf()
}

class ProxyXml {
    @XmlElement @JvmField
    var host: String = ""

    @XmlElement @JvmField
    var port: String = ""

    @XmlElement @JvmField
    var type: String = ""

    @XmlElement @JvmField
    var nonProxyHosts: String = ""
}

class DefaultReposXml {
    @XmlElement @JvmField
    var repo: List<String> = arrayListOf()
}

fun List<ProxyConfig>.getProxy(protocol:String) = find { it.type==protocol }

/**
 * The object Kobalt refers to for settings.
 */
@Singleton
class KobaltSettings @Inject constructor(val xmlFile: KobaltSettingsXml) {
    /**
     * Location of the cache repository.
     */
    var localCache = KFiles.makeDir(xmlFile.localCache) // var for testing

    /**
     * Location of the local Maven repo for the task "publishToLocalMaven".
     */
    val localMavenRepo = KFiles.makeDir(xmlFile.localMavenRepo)

    /**
     * If true, Kobalt will automatically update itself if a new version is found.
     */
    val autoUpdate = xmlFile.autoUpdate

    /**
     * If true, the Kotlin compiler will always be launched in a separate JVM, even if the requested
     * version is the same as the internal version.
     */
    val kobaltCompilerSeparateProcess = xmlFile.kobaltCompilerSeparateProcess

    val defaultRepos = xmlFile.defaultRepos?.repo

    val proxyConfigs = with(xmlFile.proxies?.proxy) {
        fun toIntOr(s: String, defaultValue: Int) = try {   //TODO can be extracted to some global Utils
            s.toInt()
        } catch(e: NumberFormatException) {
            defaultValue
        }

        if (this != null) {
            map { proxyXml->
                ProxyConfig(proxyXml.host, toIntOr(proxyXml.port, 0), proxyXml.type, proxyXml.nonProxyHosts)
            }
        } else {
            null
        }
    }

    val kobaltCompilerVersion : String?
        get() {
            return if (BUILD_SCRIPT_CONFIG != null && BUILD_SCRIPT_CONFIG?.kobaltCompilerVersion != null) {
                BUILD_SCRIPT_CONFIG?.kobaltCompilerVersion
            } else {
                xmlFile.kobaltCompilerVersion
            }
        }

    val kobaltCompilerRepo : String?
        get() {
            return if (BUILD_SCRIPT_CONFIG != null && BUILD_SCRIPT_CONFIG?.kobaltCompilerRepo != null) {
                BUILD_SCRIPT_CONFIG?.kobaltCompilerRepo
            } else {
                xmlFile.kobaltCompilerRepo
            }
        }

    val kobaltCompilerFlags : String?
        get() {
            return if (BUILD_SCRIPT_CONFIG != null && BUILD_SCRIPT_CONFIG?.kobaltCompilerFlags != null) {
                BUILD_SCRIPT_CONFIG?.kobaltCompilerFlags
            } else {
                xmlFile.kobaltCompilerFlags
            }
        }

    companion object {
        val SETTINGS_FILE_PATH = KFiles.joinDir(KFiles.HOME_KOBALT_DIR.absolutePath, "settings.xml")

        fun readSettingsXml() : KobaltSettings {
            val file = File(KobaltSettings.SETTINGS_FILE_PATH)
            if (file.exists()) {
                FileInputStream(file).use {
                    val jaxbContext = JAXBContext.newInstance(KobaltSettingsXml::class.java)
                    val xmlFile: KobaltSettingsXml = jaxbContext.createUnmarshaller().unmarshal(it)
                            as KobaltSettingsXml
                    val result = KobaltSettings(xmlFile)
                    return result
                }
            } else {
                kobaltLog(2, "Couldn't find ${KobaltSettings.SETTINGS_FILE_PATH}, using default settings")
                return KobaltSettings(KobaltSettingsXml())
            }
        }
    }

}
