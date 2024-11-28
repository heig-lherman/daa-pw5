package ch.heigvd.iict.daa.labo5.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.File

/**
 * Worker that periodically clears files in the provided cache directory.
 *
 * @author Emilie Bressoud
 * @author Loïc Herman
 * @author Sacha Butty
 */
class ImageCacheClearWorker(
    context: Context,
    parameters: WorkerParameters,
) : Worker(context, parameters) {

    private val cacheClear: File? = parameters.inputData.getString(CACHE_DIR_KEY)?.let { File(it) }

    companion object {
        const val CACHE_DIR_KEY = "cache_dir"
    }

    override fun doWork(): Result {
        (cacheClear ?: return Result.failure()).walkTopDown().forEach { it.delete() }
        return Result.success()
    }
}