package com.globalr.mobile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.card.MaterialCardView
import com.google.gson.JsonObject
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.io.File
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private var currentAssetId: String? = null
    private var currentAssetNumber: String? = null
    private var pendingPhotoUri: Uri? = null

    private lateinit var tvStatusMsg: TextView
    private lateinit var cardAsset: MaterialCardView
    private lateinit var tvAssetTitle: TextView
    private lateinit var tvAssetDetails: TextView
    private lateinit var spStage: Spinner

    private val qrLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            verifyQrPayload(result.contents)
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingPhotoUri
        if (success && uri != null) {
            uploadUriPhoto(uri)
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uploadUriPhoto(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val userObj = ApiClient.getUserJson(this)
        val tvUserBanner = findViewById<TextView>(R.id.tvUserBanner)
        val tvServerBanner = findViewById<TextView>(R.id.tvServerBanner)
        val btnChangeServer = findViewById<Button>(R.id.btnChangeServer)
        val btnScanQr = findViewById<Button>(R.id.btnScanQr)
        val etManualQr = findViewById<EditText>(R.id.etManualQr)
        val btnVerifyManual = findViewById<Button>(R.id.btnVerifyManual)
        val btnTakePhoto = findViewById<Button>(R.id.btnTakePhoto)
        val btnPickPhoto = findViewById<Button>(R.id.btnPickPhoto)
        val etTicketReason = findViewById<EditText>(R.id.etTicketReason)
        val btnQuickTicket = findViewById<Button>(R.id.btnQuickTicket)

        tvStatusMsg = findViewById(R.id.tvStatusMsg)
        cardAsset = findViewById(R.id.cardAsset)
        tvAssetTitle = findViewById(R.id.tvAssetTitle)
        tvAssetDetails = findViewById(R.id.tvAssetDetails)
        spStage = findViewById(R.id.spPhotoStage)

        val stages = listOf("RECEPCION", "ASIGNACION", "ENTRADA", "SALIDA", "MANTENIMIENTO", "REGISTRO")
        spStage.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, stages)

        if (userObj != null) {
            val fullName = userObj.get("fullName")?.asString ?: ""
            val role = userObj.get("role")?.asString ?: ""
            val country = userObj.get("countryCode")?.asString ?: ""
            tvUserBanner.text = "$fullName ($role · $country)"
        }
        tvServerBanner.text = "Servidor: ${ApiClient.getServerUrl(this)}"

        btnChangeServer.setOnClickListener {
            ApiClient.clearSession(this)
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("force_config", true)
            startActivity(intent)
            finish()
        }

        btnScanQr.setOnClickListener {
            val opts = ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setPrompt("Escanear etiqueta QR cifrada GlobalR")
                setBeepEnabled(true)
            }
            qrLauncher.launch(opts)
        }

        btnVerifyManual.setOnClickListener {
            val raw = etManualQr.text.toString().trim()
            if (raw.isNotEmpty()) verifyQrPayload(raw)
        }

        btnTakePhoto.setOnClickListener {
            val dir = File(cacheDir, "photos").apply { mkdirs() }
            val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            pendingPhotoUri = uri
            cameraLauncher.launch(uri)
        }

        btnPickPhoto.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        btnQuickTicket.setOnClickListener {
            val assetId = currentAssetId ?: return@setOnClickListener
            val assetNum = currentAssetNumber ?: ""
            val reason = etTicketReason.text.toString().trim()
            if (reason.isEmpty()) {
                tvStatusMsg.text = "Ingrese el motivo del reporte."
                return@setOnClickListener
            }
            tvStatusMsg.text = "Enviando ticket..."
            thread {
                try {
                    val created = ApiClient.createQuickTicket(this, assetId, assetNum, reason)
                    val code = created.get("ticketCode")?.asString ?: "OK"
                    runOnUiThread {
                        etTicketReason.setText("")
                        tvStatusMsg.text = "Ticket $code creado y enviado a aprobación de supervisor."
                    }
                } catch (e: Exception) {
                    runOnUiThread { tvStatusMsg.text = "Error: ${e.message}" }
                }
            }
        }
    }

    private fun verifyQrPayload(payload: String) {
        tvStatusMsg.text = "Verificando firma RSA y descifrando..."
        thread {
            try {
                val res = ApiClient.scanQr(this, payload)
                val ms = res.get("processingMs")?.asLong ?: 0L
                val asset = res.getAsJsonObject("asset")
                runOnUiThread {
                    renderAsset(asset, ms)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    cardAsset.visibility = View.GONE
                    tvStatusMsg.text = "Error QR: ${e.message}"
                }
            }
        }
    }

    private fun renderAsset(asset: JsonObject, processingMs: Long) {
        currentAssetId = asset.get("id")?.asString
        currentAssetNumber = asset.get("assetNumber")?.asString

        val brand = asset.get("brand")?.asString ?: ""
        val model = asset.get("model")?.asString ?: ""
        val serial = asset.get("serialNumber")?.asString ?: ""
        val status = asset.get("status")?.asString ?: ""
        val loc = asset.get("physicalLocation")?.asString ?: ""
        val assignee = if (asset.has("currentAssigneeLabel") && !asset.get("currentAssigneeLabel").isJsonNull)
            asset.get("currentAssigneeLabel").asString else "Sin asignar"

        tvStatusMsg.text = "QR Válido · Descifrado en $processingMs ms"
        tvAssetTitle.text = "$currentAssetNumber — $brand $model"
        tvAssetDetails.text = "Estado: $status\nSerial: $serial\nUbicación: $loc\nAsignado: $assignee"
        cardAsset.visibility = View.VISIBLE
    }

    private fun uploadUriPhoto(uri: Uri) {
        val assetId = currentAssetId ?: return
        val stage = spStage.selectedItem?.toString() ?: "RECEPCION"
        tvStatusMsg.text = "Subiendo y comprimiendo evidencia ($stage)..."
        thread {
            try {
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw RuntimeException("No se pudo leer la foto")
                val ev = ApiClient.uploadPhoto(this, assetId, stage, bytes)
                val origKb = (ev.get("originalSizeBytes")?.asLong ?: 0L) / 1024
                val compKb = (ev.get("compressedSizeBytes")?.asLong ?: 0L) / 1024
                runOnUiThread {
                    tvStatusMsg.text = "Evidencia subida a MinIO: ${origKb}KB -> ${compKb}KB"
                }
            } catch (e: Exception) {
                runOnUiThread { tvStatusMsg.text = "Error subiendo foto: ${e.message}" }
            }
        }
    }
}
