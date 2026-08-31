package com.jianshen.fitness

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.jianshen.fitness.data.ExerciseRepository
import com.jianshen.fitness.data.FitnessDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FitnessApplication : Application(), ImageLoaderFactory {

    val database: FitnessDatabase by lazy { FitnessDatabase.get(this) }

    val exercises: List<com.jianshen.fitness.data.Exercise> by lazy {
        ExerciseRepository.load(this)
    }

    override fun onCreate() {
        super.onCreate()
        com.jianshen.fitness.data.seedTemplatesIfNeeded(this)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                com.jianshen.fitness.data.BackupManager.autoBackupIfNeeded(this@FitnessApplication)
            } catch (_: Exception) {
            }
        }
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
}
