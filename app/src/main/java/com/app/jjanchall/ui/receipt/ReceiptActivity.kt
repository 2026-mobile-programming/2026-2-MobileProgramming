
package com.app.jjanchall.ui.receipt

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.app.jjanchall.databinding.ActivityReceiptBinding
import com.app.jjanchall.util.applySystemBarsPadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReceiptActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReceiptBinding

    // 갤러리에서 사진 선택
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onReceiptSelected(uri)
        }
    }

    // 카메라로 사진 촬영 (미리보기 이미지)
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            Toast.makeText(
                this,
                "영수증 촬영 완료",
                Toast.LENGTH_SHORT
            ).show()

            // TODO: 촬영한 이미지를 ML Kit으로 전달
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityReceiptBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applySystemBarsPadding()

        // 닫기 버튼
        binding.btnClose.setOnClickListener {
            finish()
        }

        // 영수증 가져오기
        binding.btnGetReceipt.setOnClickListener {
            showReceiptOptions()
        }

        // 날짜 선택
        binding.etDate.setOnClickListener {
            showDatePicker()
        }
    }

    // 카메라 / 갤러리 선택
    private fun showReceiptOptions() {
        val options = arrayOf(
            "카메라로 촬영",
            "갤러리에서 선택"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("영수증 가져오기")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> cameraLauncher.launch(null)
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // 갤러리 이미지 선택 완료
    private fun onReceiptSelected(uri: Uri) {
        Toast.makeText(
            this,
            "영수증 이미지 선택 완료",
            Toast.LENGTH_SHORT
        ).show()

        // TODO: 선택한 이미지를 ML Kit으로 전달
    }

    // 날짜 선택
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->

                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }

                val formatter = SimpleDateFormat(
                    "yyyy.MM.dd",
                    Locale.KOREA
                )

                binding.etDate.setText(
                    formatter.format(selectedDate.time)
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
