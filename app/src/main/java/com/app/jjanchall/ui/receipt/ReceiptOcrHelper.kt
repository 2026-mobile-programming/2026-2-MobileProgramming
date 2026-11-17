package com.app.jjanchall.ui.receipt

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions

class ReceiptOcrHelper {

    private val recognizer = TextRecognition.getClient(
        KoreanTextRecognizerOptions.Builder().build()
    )

    fun recognizeFromUri(
        context: Context,
        uri: Uri,
        onSuccess: (Text) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            recognizeImage(image, onSuccess, onFailure)
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    fun recognizeFromBitmap(
        bitmap: Bitmap,
        onSuccess: (Text) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizeImage(image, onSuccess, onFailure)
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    private fun recognizeImage(
        image: InputImage,
        onSuccess: (Text) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        recognizer.process(image)
            .addOnSuccessListener { result ->
                onSuccess(result)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun close() {
        recognizer.close()
    }
}
