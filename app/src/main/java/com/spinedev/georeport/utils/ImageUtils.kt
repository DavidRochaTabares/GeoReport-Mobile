package com.spinedev.georeport.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ImageUtils {
    
    private const val MAX_WIDTH = 1920f
    private const val MAX_HEIGHT = 1920f
    private const val JPEG_QUALITY = 85
    
    /**
     * Compress and optimize an image file
     * @param context Android context
     * @param sourceUri URI of the source image
     * @param destinationFile File where the compressed image will be saved
     * @return true if compression was successful
     */
    fun compressImage(
        context: Context,
        sourceUri: Uri,
        destinationFile: File
    ): Boolean {
        return try {
            // Read the image
            val inputStream = context.contentResolver.openInputStream(sourceUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (originalBitmap == null) {
                return false
            }
            
            // Get orientation from EXIF
            val rotation = getImageRotation(context, sourceUri)
            
            // Calculate new dimensions
            val (newWidth, newHeight) = calculateScaledDimensions(
                originalBitmap.width.toFloat(),
                originalBitmap.height.toFloat()
            )
            
            // Scale the bitmap
            val scaledBitmap = Bitmap.createScaledBitmap(
                originalBitmap,
                newWidth.toInt(),
                newHeight.toInt(),
                true
            )
            
            // Rotate if needed
            val rotatedBitmap = if (rotation != 0f) {
                val matrix = Matrix().apply { postRotate(rotation) }
                Bitmap.createBitmap(
                    scaledBitmap,
                    0,
                    0,
                    scaledBitmap.width,
                    scaledBitmap.height,
                    matrix,
                    true
                )
            } else {
                scaledBitmap
            }
            
            // Save compressed image
            FileOutputStream(destinationFile).use { out ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            
            // Clean up
            if (rotatedBitmap != scaledBitmap) {
                scaledBitmap.recycle()
            }
            if (scaledBitmap != originalBitmap) {
                originalBitmap.recycle()
            }
            rotatedBitmap.recycle()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Calculate scaled dimensions maintaining aspect ratio
     */
    private fun calculateScaledDimensions(width: Float, height: Float): Pair<Float, Float> {
        if (width <= MAX_WIDTH && height <= MAX_HEIGHT) {
            return Pair(width, height)
        }
        
        val ratio = width / height
        
        return if (width > height) {
            // Landscape
            val newWidth = MAX_WIDTH.coerceAtMost(width)
            val newHeight = newWidth / ratio
            Pair(newWidth, newHeight)
        } else {
            // Portrait
            val newHeight = MAX_HEIGHT.coerceAtMost(height)
            val newWidth = newHeight * ratio
            Pair(newWidth, newHeight)
        }
    }
    
    /**
     * Get image rotation from EXIF data
     */
    private fun getImageRotation(context: Context, uri: Uri): Float {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                when (exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (e: IOException) {
            0f
        }
    }
}
