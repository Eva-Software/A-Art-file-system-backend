import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.Resources
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.cio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.uuid.UUID
import kotlinx.uuid.generateUUID
import org.slf4j.event.*
import java.io.File
import java.io.InputStream

val WORLDS_DIRECTORY = File("./worlds")
const val PICTURES_DIRECTORY_NAME = "pictures"

fun main() {
    WORLDS_DIRECTORY.mkdirs()
    embeddedServer(
        factory =  Netty,
        host = System.getenv("HOST"),
        port = System.getenv("PORT").toInt(),
        module = Application::aart
    ).start(wait = true)
}

fun File.uniqueName(gen: () -> String): String {
    var name: String
    do {
        name = gen()
    } while (File(this, name).exists())
    return name
}

fun File.generateUniqueFileName(extension: String): String {
    var n = 0
    var name: String
    do {
        name = "${n++}.$extension"
    } while (File(this, name).exists())
    return name
}

fun File.nameOfDirectories(): List<String> =
    listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()

fun File.nameOfFiles(): List<String> =
    listFiles()?.filter { it.isFile }?.map { it.name } ?: emptyList()

fun File.takeIfExists(): File? = takeIf { it.exists() }

fun File.addFile(fileName: String, inputStream: InputStream) {
    mkdirs()
    inputStream.use {
        File(this, fileName).writeBytes(inputStream.readBytes())
    }
}

fun File(src: File, vararg names: String): File = File(src, names.joinToString("\\"))

fun ApplicationRequest.fileExtension(): String = contentType().contentSubtype

fun Application.aart() {
    install(ContentNegotiation) {
        json()
    }
    install(Resources)
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowNonSimpleContentTypes = true
        anyHost()
    }
    routing {
        static("files") {
            resource<Worlds> {
                static {
                    files(WORLDS_DIRECTORY)
                }
            }
        }

        get<Worlds> {
            call.respond(message = WORLDS_DIRECTORY.nameOfDirectories())
        }
        post<Worlds> {
            val name = WORLDS_DIRECTORY.uniqueName { UUID.generateUUID().toString() }
            withContext(Dispatchers.IO) {
                val directory = File(WORLDS_DIRECTORY, name)
                directory.addFile(
                    fileName = directory.generateUniqueFileName(call.request.fileExtension()),
                    inputStream = call.receiveStream()
                )
            }
            call.respond(message = name)
        }


        get<Worlds.World> {
            val directory = File(WORLDS_DIRECTORY, it.name).takeIfExists()
                ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(message = directory.nameOfFiles())
        }
        delete<Worlds.World> {
            File(WORLDS_DIRECTORY, it.name).takeIfExists()?.deleteRecursively()
            call.respond(HttpStatusCode.OK)
        }
        post<Worlds.World> {
            withContext(Dispatchers.IO) {
                val directory = File(WORLDS_DIRECTORY, it.name)
                directory.addFile(
                    fileName = directory.generateUniqueFileName(call.request.fileExtension()),
                    inputStream = call.receiveStream()
                )
            }
            call.respond(HttpStatusCode.OK)
        }


        post<Worlds.World.File> {
            withContext(Dispatchers.IO) {
                File(WORLDS_DIRECTORY, it.world.name).addFile(
                    fileName = it.fileName,
                    inputStream = call.receiveStream()
                )
            }
            call.respond(HttpStatusCode.OK)
        }
        delete<Worlds.World.File> {
            File(WORLDS_DIRECTORY, it.world.name, it.fileName).takeIfExists()?.delete()
            call.respond(HttpStatusCode.OK)
        }


        get<Pictures> {
            val directory = File(WORLDS_DIRECTORY, it.worldName).takeIfExists()
                ?: return@get call.respond(HttpStatusCode.NotFound)
            val picturesDirectory = File(directory, PICTURES_DIRECTORY_NAME)
            call.respond(message = picturesDirectory.nameOfFiles())
        }
        delete<Pictures> {
            File(WORLDS_DIRECTORY, it.worldName, PICTURES_DIRECTORY_NAME).takeIfExists()?.deleteRecursively()
            call.respond(HttpStatusCode.OK)
        }
        post<Pictures> {
            val pictureDirectory = File(WORLDS_DIRECTORY, it.worldName, PICTURES_DIRECTORY_NAME)
            withContext(Dispatchers.IO) {
                pictureDirectory.addFile(
                    fileName = pictureDirectory.generateUniqueFileName(call.request.fileExtension()),
                    inputStream = call.receiveStream()
                )
            }
            call.respond(HttpStatusCode.OK)
        }


        delete<Pictures.Picture> {
            File(WORLDS_DIRECTORY, it.pictures.worldName, PICTURES_DIRECTORY_NAME, it.fileName).takeIfExists()?.delete()
            call.respond(HttpStatusCode.OK)
        }
        post<Pictures.Picture> {
            withContext(Dispatchers.IO) {
                File(WORLDS_DIRECTORY, it.pictures.worldName, PICTURES_DIRECTORY_NAME).addFile(
                    fileName = it.fileName,
                    inputStream = call.receiveStream()
                )
            }
            call.respond(HttpStatusCode.OK)
        }
    }
}


@Serializable
@Resource("worlds")
class Worlds {
    @Serializable
    @Resource("{name}")
    class World(val name: String, val worlds: Worlds = Worlds()) {
        @Serializable
        @Resource("{fileName}")
        class File(val fileName: String, val world: World)
    }
}

@Serializable
@Resource("pictures/{worldName}")
class Pictures(val worldName: String) {
    @Serializable
    @Resource("{fileName}")
    class Picture(val fileName: String, val pictures: Pictures)
}