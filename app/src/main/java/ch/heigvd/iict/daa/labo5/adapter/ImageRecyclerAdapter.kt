package ch.heigvd.iict.daa.labo5.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import ch.heigvd.iict.daa.labo5.R
import ch.heigvd.iict.daa.labo5.databinding.RecyclerImageViewItemBinding
import ch.heigvd.iict.daa.labo5.utils.millisecondsUntilNow
import ch.heigvd.iict.daa.labo5.utils.writeBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.URL
import kotlin.time.Duration.Companion.minutes

class ImageRecyclerAdapter(
    context: Context,
    private val scope: LifecycleCoroutineScope,
    private val cacheDir: File,
) : RecyclerView.Adapter<ImageRecyclerAdapter.ViewHolder>() {

    companion object {
        private const val IMAGE_COUNT = 10_000
        private const val IMAGE_URL_FMT = "https://daa.iict.ch/images/%d.jpg"

        private val CACHE_TTL = 5.minutes
    }

    private val failoverImage: Bitmap = Bitmap.createBitmap(300, 180, Bitmap.Config.ARGB_8888)

    init {
        val icon = AppCompatResources.getDrawable(context, R.drawable.ic_outline_broken_image)!!
        val canvas = Canvas(failoverImage)
        // draw icon in center of canvas with padding
        icon.bounds = Rect(125, 65, 175, 115)
        icon.draw(canvas)
    }

    override fun getItemCount() = IMAGE_COUNT

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): ViewHolder {
        return ViewHolder(
            RecyclerImageViewItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(
        holder: ViewHolder, position: Int
    ) {
        holder.bind(URL(IMAGE_URL_FMT.format(position)))
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.unbind()
    }

    // Fetches an image from the cache, with an orElse block for when there is a cache miss
    private suspend fun getCachedImage(
        name: String,
        orElse: suspend () -> Bitmap
    ): Bitmap = withContext(Dispatchers.IO) {
        val file = File(cacheDir, name)
        if (file.exists() && file.canRead() && !isImageExpired(file)) {
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            orElse().also { setCachedImage(name, it) }
        }
    }

    // Stores an image in the cache directory
    private suspend fun setCachedImage(name: String, bitmap: Bitmap) = withContext(Dispatchers.IO) {
        File(cacheDir, name).writeBitmap(bitmap)
    }

    // Downloads content from the provided URL
    private suspend fun downloadImage(url: URL): ByteArray? = withContext(Dispatchers.IO) {
        try {
            url.openStream().use { it.readBytes() }
        } catch (_: IOException) {
            null
        }
    }

    inner class ViewHolder(
        private val view: RecyclerImageViewItemBinding
    ) : RecyclerView.ViewHolder(view.root) {

        private var downloadJob: Job? = null

        fun bind(image: URL) {
            downloadJob = scope.launch {
                getCachedImage(image.file) {
                    downloadImage(image)?.decodeBitmap() ?: failoverImage
                }.also {
                    view.recyclerImageViewItemImage.setImageBitmap(it)
                    view.recyclerImageViewItemSpinner.visibility = View.GONE
                    view.recyclerImageViewItemImage.visibility = View.VISIBLE
                }
            }
        }

        fun unbind() {
            downloadJob?.cancel()
            view.recyclerImageViewItemImage.visibility = View.GONE
            view.recyclerImageViewItemSpinner.visibility = View.VISIBLE
        }
    }

    // Helper function to check if an image is expired from the last modification date
    private fun isImageExpired(file: File) = file.lastModified().millisecondsUntilNow > CACHE_TTL
}

// Helper function to decode a byte array into a bitmap, in a separate context
private suspend fun ByteArray.decodeBitmap(): Bitmap = withContext(Dispatchers.Default) {
    BitmapFactory.decodeByteArray(this@decodeBitmap, 0, size)
}
