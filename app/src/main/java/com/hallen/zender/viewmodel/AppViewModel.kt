package com.hallen.zender.viewmodel

import android.content.ContentUris
import android.content.Context
import android.content.pm.ApplicationInfo
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.storage.StorageManager
import android.provider.MediaStore
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hallen.zender.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val appsUseCase: AppsUseCase,
    private val imageUseCase: ImageUseCase,
    private val fileUseCase: FileUseCase
) : ViewModel() {
    val appModel = MutableLiveData<List<ApplicationInfo>>()
    val imagesLiveData: MutableLiveData<List<String>> = MutableLiveData()
    val albumesLiveData: MutableLiveData<List<Image>> = MutableLiveData()
    val videosLiveData: MutableLiveData<List<Video>> = MutableLiveData()
    val audiosLiveData: MutableLiveData<List<Audio>> = MutableLiveData()
    val storageLiveData: MutableLiveData<List<Storage>> = MutableLiveData()
    val fileLiveData: MutableLiveData<List<File>> = MutableLiveData()
    val actualPath: MutableLiveData<String> = MutableLiveData()

    private fun getAllVideosFromDevice(context: Context): List<Video> {
        val uri: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val cursor: Cursor?
        val videos = ArrayList<Video>()
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.VideoColumns.DATA,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )
        cursor = context.contentResolver.query(
            uri, projection, null, null, sortOrder
        ) ?: return arrayListOf()
        val columnIndexData: Int = cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DATA)
        val columnId: Int = cursor.getColumnIndex(MediaStore.Video.Media._ID)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(columnId)
            val videoUri = ContentUris.withAppendedId(uri, id)
            val videoPath = cursor.getString(columnIndexData)
            val video = Video(videoPath, videoUri)
            videos.add(video)
        }
        cursor.close()
        return videos
    }

    private fun getAllAudioFromDevide(context: Context): ArrayList<Audio> {
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val cursor: Cursor?
        val audios: ArrayList<Audio> = arrayListOf()
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
        )
        cursor = context.contentResolver.query(
            uri, projection, null, null, sortOrder
        ) ?: return arrayListOf()
        val columnIndexData: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
        val artistColumns = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        while (cursor.moveToNext()) {

            val id: Long = cursor.getLong(idColumn)
            val size: Int = cursor.getInt(sizeColumn)
            val title: String = cursor.getString(titleColumn)
            val duration: Int = cursor.getInt(durationColumn)
            val albumId: Long = cursor.getLong(albumIdColumn)
            val path: String = cursor.getString(columnIndexData)
            val artists: String = cursor.getString(artistColumns).takeIf {
                it != MediaStore.UNKNOWN_STRING
            } ?: "Artista Desconocido"
            // Album folder uri
            val albumUri = Uri.parse("content://media//external/audio/albumart")
            // Song uri
            val audioUri: Uri = ContentUris.withAppendedId(uri, id)
            // Album artwork uri
            val albumArtWorkUri = ContentUris.withAppendedId(albumUri, albumId)
            // remove .mp3 extension from the song's name


            // The audio item
            val audio = Audio(
                name = title,
                path = path,
                artist = artists,
                uri = audioUri,
                icon = albumArtWorkUri,
                size = size,
                duration = duration
            )

            // Add audio item to audios arraylist
            audios.add(audio)
        }
        cursor.close()
        return audios
    }

    fun getAllAudios(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val audios: List<Audio> = getAllAudioFromDevide(context)
            audiosLiveData.postValue(audios)
        }
    }

    fun getAllVideos(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val videos: List<Video> = getAllVideosFromDevice(context)
            videosLiveData.postValue(videos)
        }
    }

    fun getAllImages(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val albumes: HashMap<Long, Album> = imageUseCase.loadImagesByDate(context)
            val arrayAlbum: ArrayList<Image> = arrayListOf()
            for (has in albumes) {
                val imageArray: ArrayList<Image> = arrayListOf()
                imageArray.add(Image("", Uri.EMPTY, has.value.date, has.key, true))
                imageArray.addAll(has.value.images)
                arrayAlbum.addAll(imageArray)
            }
            albumesLiveData.postValue(arrayAlbum)
        }
    }

    private fun getStorages(context: Context): List<Storage> {
        val storages: ArrayList<Storage> = ArrayList()
        val storageManager: StorageManager =
            context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val externalDirs: Array<File> = context.applicationContext.getExternalFilesDirs(null)
        for (file in externalDirs) {
            val path: String = file.path.split("/Android")[0]
            val name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                storageManager.getStorageVolume(file)?.getDescription(context)
            } else {
                File(path).name
            }
            val storage = Storage(
                name = name ?: "Unknown",
                path = path,
                uri = Uri.EMPTY,
                total = file.totalSpace,
                available = file.freeSpace
            )
            storages.add(storage)
        }
        return storages
    }

    fun loadFiles(path: String) {
        fileUseCase.getFilesFromFolder(path)?.let {
            fileLiveData.postValue(it)
            actualPath.value = path
        }
    }

    fun back() {
        actualPath.value?.let {
            val file = File(it)
            file.parent?.let { parent -> loadFiles(parent) }
        }
    }

    fun reloadFiles() {
        actualPath.value?.let {
            loadFiles(it)
        }
    }


    fun getAllData(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val storages = getStorages(context)
            storageLiveData.postValue(storages)
            CoroutineScope(Dispatchers.Main).launch {
                loadFiles(storages.firstOrNull()?.path ?: "")
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            val apps = appsUseCase.getAllApps(context)
            appModel.postValue(apps)
        }.invokeOnCompletion {
            getAllImages(context)
            getAllVideos(context)
            getAllAudios(context)
        }
    }
}