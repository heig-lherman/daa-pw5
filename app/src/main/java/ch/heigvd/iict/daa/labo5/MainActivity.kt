package ch.heigvd.iict.daa.labo5

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ch.heigvd.iict.daa.labo5.adapter.ImageRecyclerAdapter
import ch.heigvd.iict.daa.labo5.databinding.ActivityMainBinding
import ch.heigvd.iict.daa.labo5.worker.ImageCacheClearWorker
import kotlinx.coroutines.cancelChildren
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

/**
 * Main activity for our application, it initializes the main view and binds context options
 * to the viewmodel actions.
 *
 * @author Emilie Bressoud
 * @author Loïc Herman
 * @author Sacha Butty
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private val CACHE_CLEAR_INTERVAL = 15.minutes.toJavaDuration()
    }

    private lateinit var viewBinding: ActivityMainBinding;
    private lateinit var adapter: ImageRecyclerAdapter

    private val workerData
        get() = Data.Builder()
            .putString(ImageCacheClearWorker.CACHE_DIR_KEY, cacheDir.absolutePath)
            .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Instantiate the view adapter for the recycler
        adapter = ImageRecyclerAdapter(this, lifecycleScope, cacheDir)
        viewBinding.mainRecyclerView.adapter = adapter

        // Define a periodic work task to clear the cache directory
        val periodicClear = PeriodicWorkRequestBuilder<ImageCacheClearWorker>(CACHE_CLEAR_INTERVAL)
            .setInputData(workerData)
            .build()

        // Schedule the work task
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            ImageCacheClearWorker::class.qualifiedName!!,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicClear
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_controls_action_refresh -> launchClearCache().let { true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.coroutineContext.cancelChildren()
    }

    @SuppressLint("NotifyDataSetChanged") // We explicitly want all images to reload
    private fun launchClearCache() {
        val request = OneTimeWorkRequestBuilder<ImageCacheClearWorker>()
            .setInputData(workerData)
            .build()
        WorkManager
            .getInstance(applicationContext)
            .enqueue(request)
            .state.observe(this) {
                when (it) {
                    is Operation.State.SUCCESS -> {
                        adapter.notifyDataSetChanged()
                        toast(R.string.toast_cache_cleared)
                    }

                    is Operation.State.FAILURE -> toast(R.string.toast_cache_not_cleared)
                }
            }
    }

    private fun toast(message: Int) {
        Toast.makeText(
            this,
            getString(message),
            Toast.LENGTH_SHORT
        ).show()
    }
}