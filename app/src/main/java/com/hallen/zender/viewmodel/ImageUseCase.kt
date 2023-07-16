package com.hallen.zender.viewmodel

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.hallen.zender.model.Album
import com.hallen.zender.model.Image
import java.util.*
import javax.inject.Inject

class ImageUseCase @Inject constructor() {

    fun getAllImagesFromDevice(context: Context): ArrayList<String> {
        val cursor: Cursor?
        val listOfAllImages: ArrayList<String> = arrayListOf()
        var absolutePathOfImage: String
        val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection =
            arrayOf(MediaStore.MediaColumns.DATA, MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        cursor =
            context.contentResolver.query(uri, projection, null, null, null) ?: return arrayListOf()
        val columnIndexData: Int = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
        val columnIndexFolderName: Int =
            cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        while (cursor.moveToNext()) {
            absolutePathOfImage = cursor.getString(columnIndexData)
            listOfAllImages.add(absolutePathOfImage)
        }
        cursor.close()
        return listOfAllImages

    }

    fun getAllImagesFromDevice(context: Context, a: Any): ArrayList<String> {
        val alumes: ArrayList<Album> = arrayListOf()
        val cursor: Cursor?
        val listOfAllImages: ArrayList<String> = arrayListOf()
        var absolutePathOfImage: String
        val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection =
            arrayOf(MediaStore.MediaColumns.DATA, MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        cursor =
            context.contentResolver.query(uri, projection, null, null, null) ?: return arrayListOf()
        val columnIndexData: Int = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
        val columnIndexFolderName: Int =
            cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        while (cursor.moveToNext()) {
            absolutePathOfImage = cursor.getString(columnIndexData)
            listOfAllImages.add(absolutePathOfImage)
        }
        cursor.close()
        return listOfAllImages

    }

    /*
    fun loadAlbumesfromSdCard(context: Context): List<Album> {
        val cursor: Cursor?
        val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val projection = arrayOf(
            MediaStore.Images.ImageColumns._ID,
            MediaStore.Images.ImageColumns.BUCKET_ID,
            MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Images.ImageColumns.DATA
        )

        val findAlbums = HashMap<String, Album>()

        cursor = context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            sortOrder
        )
            ?: return emptyList()
        val bucketIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.BUCKET_ID)
        val bucketNameIndex =
            cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME)
        val columnIndexData: Int =
            cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
        while (cursor.moveToNext()) {

            val bucketId = cursor.getString(bucketIdIndex)
            val imagePath = cursor.getString(columnIndexData)
            findAlbums[bucketId] ?: let {
                val bucketName = cursor.getString(bucketNameIndex)
                Album(
                    id = bucketId,
                    name = bucketName ?: ""
                )
            }.images.add(imagePath)

        }
        cursor.close()
        return findAlbums.values.toList()
    }
    */

    fun loadImagesByDate(context: Context): HashMap<Long, Album> {
        val cursor: Cursor?
        val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.ImageColumns.DATE_ADDED,
            MediaStore.Images.ImageColumns.DATA
        )

        val findAlbums = HashMap<Long, Album>()

        cursor = context.contentResolver.query(
            uri, projection, null, null, sortOrder
        ) ?: return hashMapOf()

        val columnIndexData: Int = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
        val columnDateAdded: Int = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
        val columnId: Int = cursor.getColumnIndex(MediaStore.Images.Media._ID)
        val calendar = Calendar.getInstance();
        val dayInMillis = 86400
        while (cursor.moveToNext()) {
            val id = cursor.getLong(columnId)
            val contentUri = ContentUris.withAppendedId(uri, id)
            // Utiliza la URI `contentUri` para acceder a la imagen
            val imagePath = cursor.getString(columnIndexData)
            //val dateAdded = cursor.getString(columnDateAdded)
            val dateAdded = cursor.getLong(columnDateAdded)
            calendar.timeInMillis = dateAdded * 1000L // convertir a milisegundos
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val midnightInMillis = calendar.timeInMillis / 1000L // convertir a segundos
            val dayGroup = midnightInMillis / dayInMillis
            val image = Image(imagePath, contentUri, dateAdded, dayGroup)
            if (findAlbums[dayGroup] == null) {
                val album = Album(date = dateAdded)
                album.images.add(image)
                findAlbums[dayGroup] = album
            } else findAlbums[dayGroup]?.images?.add(image)
        }
        cursor.close()
        return findAlbums
    }


}
