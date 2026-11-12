package com.app.jjanchall.ui.receipt

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.app.jjanchall.databinding.ActivityReceiptBinding
import com.app.jjanchall.util.applySystemBarsPadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.text.Text
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.core.content.FileProvider
import java.io.File

class ReceiptActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReceiptBinding
    private val ocrHelper = ReceiptOcrHelper()
    private var cameraImageUri: Uri? = null

    companion object {
        // 기존 지출 추가 화면으로 전달할 데이터 이름
        const val EXTRA_AMOUNT = "receipt_amount"
        const val EXTRA_DATE = "receipt_date"
        const val EXTRA_STORE_NAME = "receipt_store_name"
    }

    // 갤러리에서 사진 선택
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onReceiptSelected(uri)
        }
    }

    // 카메라로 사진 촬영
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = cameraImageUri
        cameraImageUri = null

        if (success && uri != null) {
            onReceiptSelected(uri)
        } else if (success) {
            showMessage("사진을 불러오지 못했어요. 다시 촬영해 주세요.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraImageUri = savedInstanceState
            ?.getString("camera_image_uri")
            ?.let { Uri.parse(it) }

        enableEdgeToEdge()

        binding = ActivityReceiptBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applySystemBarsPadding()

        // 닫기 버튼
        binding.btnClose.setOnClickListener {
            finish()
        }

        // 영수증 가져오기 버튼
        binding.btnGetReceipt.setOnClickListener {
            showReceiptOptions()
        }

        // 날짜 선택
        binding.etDate.setOnClickListener {
            showDatePicker()
        }

        // 입력 완료 버튼
        binding.btnComplete.setOnClickListener {
            completeReceiptInput()
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
                    0 -> launchCamera()
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // 원본 사진을 저장할 파일을 만들고 카메라 실행
    private fun launchCamera() {
        try {
            val imageDir = File(cacheDir, "receipt_images").apply {
                mkdirs()
            }

            val imageFile = File.createTempFile(
                "receipt_",
                ".jpg",
                imageDir
            )

            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                imageFile
            )

            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            cameraImageUri = null
            showMessage("카메라를 실행하지 못했어요.", Toast.LENGTH_LONG)
        }
    }

    // 갤러리에서 선택한 영수증 인식
    private fun onReceiptSelected(uri: Uri) {
        showMessage("영수증 분석 중...")

        ocrHelper.recognizeFromUri(
            context = this,
            uri = uri,
            onSuccess = { result ->
                applyOcrResult(result)
            },
            onFailure = { exception ->
                showMessage(
                    "영수증 인식 실패: ${exception.message}",
                    Toast.LENGTH_LONG
                )
            }
        )
    }

    // OCR 결과를 입력칸에 자동 입력
    private fun applyOcrResult(result: Text) {

        val info = ReceiptParser.parse(result)

        // 금액 자동 입력
        binding.etAmount.setText(
            info.amount.orEmpty()
        )

        // 날짜 자동 입력
        binding.etDate.setText(
            info.date.orEmpty()
        )

        // 가게 이름 자동 입력
        binding.etStoreName.setText(
            info.store.orEmpty()
        )

        if (
            info.amount == null ||
            info.date == null ||
            info.store == null
        ) {
            showMessage(
                "일부 정보를 인식하지 못했어요. 직접 입력해 주세요.",
                Toast.LENGTH_LONG
            )
        } else {
            showMessage(
                "인식 완료! 금액, 날짜, 가게 이름을 확인해 주세요.",
                Toast.LENGTH_LONG
            )
        }
    }

    // 날짜 선택 달력
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

    // 입력 완료 버튼 처리
    private fun completeReceiptInput() {

        // 사용자가 수정한 최종 값 가져오기
        val amount = binding.etAmount.text
            .toString()
            .trim()

        val date = binding.etDate.text
            .toString()
            .trim()

        val storeName = binding.etStoreName.text
            .toString()
            .trim()

        // 금액 확인
        val amountNumber = amount.toLongOrNull()

        if (amountNumber == null || amountNumber <= 0L) {
            binding.etAmount.error = "올바른 금액을 입력해 주세요."
            binding.etAmount.requestFocus()
            return
        }

        // 날짜 확인
        val formatter = SimpleDateFormat(
            "yyyy.MM.dd",
            Locale.KOREA
        ).apply {
            isLenient = false
        }

        val validDate =
            date.matches(Regex("""\d{4}\.\d{2}\.\d{2}""")) &&
                    runCatching {
                        formatter.parse(date)
                    }.getOrNull() != null

        if (!validDate) {
            showMessage("날짜를 확인해 주세요.")
            showDatePicker()
            return
        }

        // 가게 이름 확인
        if (storeName.isBlank()) {
            binding.etStoreName.error = "가게 이름을 입력해 주세요."
            binding.etStoreName.requestFocus()
            return
        }

        // 기존 지출 추가 화면으로 값 전달
        val resultIntent = Intent().apply {
            putExtra(EXTRA_AMOUNT, amount)
            putExtra(EXTRA_DATE, date)
            putExtra(EXTRA_STORE_NAME, storeName)
        }

        setResult(RESULT_OK, resultIntent)

        // 영수증 입력 화면 닫기
        finish()
    }

    // 안내 메시지
    private fun showMessage(
        message: String,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        Toast.makeText(
            this,
            message,
            duration
        ).show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("camera_image_uri", cameraImageUri?.toString())
        super.onSaveInstanceState(outState)
    }

    // OCR 리소스 정리
    override fun onDestroy() {
        ocrHelper.close()
        super.onDestroy()
    }
}
