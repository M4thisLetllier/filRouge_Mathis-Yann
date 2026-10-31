package ca.uqac.c8INF865.test

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object AlarmSoundCache {
    private const val DIRECTORY = "alarm_sounds"

    fun localFile(context: Context, sonId: String): File =
        File(File(context.filesDir, DIRECTORY), "$sonId.3gp")

    fun cache(context: Context, sonId: String, url: String): File? {
        val destination = localFile(context, sonId)
        if (destination.exists() && destination.length() > 0) return destination

        val directory = destination.parentFile ?: return null
        if (!directory.exists() && !directory.mkdirs()) return null

        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 20_000
            connection.requestMethod = "GET"
            connection.connect()
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
            if (destination.length() > 0) destination else null
        } catch (exception: Exception) {
            destination.delete()
            android.util.Log.e("AlarmSoundCache", "Impossible de mettre le son en cache", exception)
            null
        }
    }
}
