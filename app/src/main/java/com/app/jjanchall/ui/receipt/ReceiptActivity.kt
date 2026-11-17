package com.app.jjanchall.ui.receipt

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import com.app.jjanchall.databinding.ActivityReceiptBinding
import com.app.jjanchall.util.applySystemBarsPadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.text.Text
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

class ReceiptActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReceiptBinding
    private val ocrHelper = ReceiptOcrHelper()
    private var cameraImageUri: Uri? = null
    private var pendingOcrUri: Uri? = null
    private var isRecognizing = false
    private var requestId = 0

    companion object {
        const val EXTRA_AMOUNT = "receipt_amount"
        const val EXTRA_DATE = "receipt_date"
        const val EXTRA_STORE_NAME = "receipt_store_name"

        private const val STATE_CAMERA_URI = "camera_image_uri"
        private const val STATE_OCR_URI = "pending_ocr_uri"
        // 지출 화면의 9자리 제한과 동일하게 검증
        private const val MAX_AMOUNT = 999_999_999L
        private val DATE_FORMATTER = DateTimeFormatter
            .ofPattern("uuuu.MM.dd", Locale.KOREA)
            .withResolverStyle(ResolverStyle.STRICT)
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) onReceiptSelected(uri)
    }

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
        cameraImageUri = savedInstanceState?.getString(STATE_CAMERA_URI)?.let(Uri::parse)
        pendingOcrUri = savedInstanceState?.getString(STATE_OCR_URI)?.let(Uri::parse)
        enableEdgeToEdge()
        binding = ActivityReceiptBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        binding.btnClose.setOnClickListener { finish() }
        binding.btnGetReceipt.setOnClickListener { showReceiptOptions() }
        binding.etDate.setOnClickListener { if (!isRecognizing) showDatePicker() }
        binding.btnComplete.setOnClickListener { completeReceiptInput() }
        binding.etAmount.doAfterTextChanged { binding.etAmount.error = null }
        binding.etStoreName.doAfterTextChanged { binding.etStoreName.error = null }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // 입력칸 복원이 끝난 뒤 회전 등으로 중단된 분석을 다시 시작
        val uri = pendingOcrUri
        if (uri != null) onReceiptSelected(uri) else setRecognizing(false)
    }

    private fun showReceiptOptions() {
        if (isRecognizing) return
        MaterialAlertDialogBuilder(this)
            .setTitle("영수증 가져오기")
            .setItems(arrayOf("카메라로 촬영", "갤러리에서 선택")) { _, which ->
                if (which == 0) {
                    launchCamera()
                } else {
                    try {
                        galleryLauncher.launch("image/*")
                    } catch (_: Exception) {
                        showMessage("사진 선택 화면을 열지 못했어요.")
                    }
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun launchCamera() {
        try {
            val imageDir = File(cacheDir, "receipt_images").apply { mkdirs() }
            val imageFile = File.createTempFile("receipt_", ".jpg", imageDir)
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", imageFile)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } catch (_: Exception) {
            cameraImageUri = null
            showMessage("카메라를 실행하지 못했어요.", Toast.LENGTH_LONG)
        }
    }

    private fun onReceiptSelected(uri: Uri) {
        if (isRecognizing || isFinishing || isDestroyed) return
        pendingOcrUri = uri
        val currentRequest = ++requestId
        setRecognizing(true)
        ocrHelper.recognizeFromUri(
            context = this,
            uri = uri,
            onSuccess = { result ->
                if (canApplyResult(currentRequest)) {
                    pendingOcrUri = null
                    setRecognizing(false)
                    applyOcrResult(result)
                }
            },
            onFailure = {
                if (canApplyResult(currentRequest)) {
                    pendingOcrUri = null
                    setRecognizing(false)
                    showMessage("영수증을 인식하지 못했어요. 사진을 다시 선택하거나 직접 입력해 주세요.", Toast.LENGTH_LONG)
                }
            }
        )
    }

    private fun canApplyResult(id: Int): Boolean =
        id == requestId && !isFinishing && !isDestroyed

    private fun setRecognizing(recognizing: Boolean) {
        isRecognizing = recognizing
        binding.btnGetReceipt.isEnabled = !recognizing
        binding.btnComplete.isEnabled = !recognizing
        binding.etAmount.isEnabled = !recognizing
        binding.etDate.isEnabled = !recognizing
        binding.etStoreName.isEnabled = !recognizing
        binding.btnGetReceipt.text = if (recognizing) "영수증 분석 중…" else "＋  영수증 가져오기"
    }

    private fun applyOcrResult(result: Text) {
        val info = ReceiptParser.parse(result)
        // 새 영수증에서 못 읽은 값은 비워 이전 영수증 정보와 섞이지 않게 함
        binding.etAmount.setText(info.amount.orEmpty())
        binding.etDate.setText(info.date.orEmpty())
        binding.etStoreName.setText(info.store.orEmpty())
        val message = if (info.amount == null || info.date == null || info.store == null) {
            "일부 정보를 인식하지 못했어요. 내용을 확인하고 빈 항목을 직접 입력해 주세요."
        } else {
            "인식 완료! 금액, 날짜, 가게 이름을 확인해 주세요."
        }
        showMessage(message, Toast.LENGTH_LONG)
    }

    private fun parseDate(value: String): LocalDate? = runCatching {
        LocalDate.parse(value, DATE_FORMATTER)
    }.getOrNull()

    private fun showDatePicker() {
        val date = parseDate(binding.etDate.text.toString().trim()) ?: LocalDate.now()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                binding.etDate.setText(LocalDate.of(year, month + 1, day).format(DATE_FORMATTER))
            },
            date.year, date.monthValue - 1, date.dayOfMonth
        ).show()
    }

    private fun completeReceiptInput() {
        if (isRecognizing) return
        val amount = binding.etAmount.text.toString().trim()
        val date = binding.etDate.text.toString().trim()
        val storeName = binding.etStoreName.text.toString().trim()
        val amountNumber = amount.toLongOrNull()
        if (amountNumber == null || amountNumber !in 1L..MAX_AMOUNT) {
            binding.etAmount.error = "금액은 1원부터 999,999,999원까지 입력해 주세요."
            binding.etAmount.requestFocus()
            return
        }
        if (parseDate(date) == null) {
            showMessage("날짜를 확인해 주세요.")
            showDatePicker()
            return
        }
        if (storeName.isBlank()) {
            binding.etStoreName.error = "가게 이름을 입력해 주세요."
            binding.etStoreName.requestFocus()
            return
        }
        setResult(RESULT_OK, Intent().apply {
            // 선행 0을 없앤 값을 전달해 지출 화면에서 잘리는 것을 방지
            putExtra(EXTRA_AMOUNT, amountNumber.toString())
            putExtra(EXTRA_DATE, date)
            putExtra(EXTRA_STORE_NAME, storeName)
        })
        finish()
    }

    private fun showMessage(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_CAMERA_URI, cameraImageUri?.toString())
        outState.putString(STATE_OCR_URI, pendingOcrUri?.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        requestId++
        ocrHelper.close()
        super.onDestroy()
    }
}
