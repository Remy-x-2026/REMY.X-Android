package com.remyx.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ffmpegkit_maintained.ytdlp.YtDlp
import dev.ffmpegkit_maintained.ytdlp.YtDlpRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

private val RemyBackground = Color(0xFF05060A)
private val RemyAccent = Color(0xFF7668FF)
private val RemyTextMuted = Color(0xFF9AA4B5)
private val RemyPanel = Color(0xFF0B0E15)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            YtDlp.init(applicationContext)
        } catch (_: Exception) {
            // El error real se mostrará al iniciar una descarga.
        }

        setContent {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    primary = RemyAccent,
                    surface = RemyPanel,
                    background = RemyBackground
                )
            ) {
                RemyHome(applicationContext)
            }
        }
    }
}

private fun buildOutputDirectory(context: Context): File {
    val base = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        ?: File(context.filesDir, "downloads")

    return File(base, "REMY.X").apply {
        if (!exists()) {
            mkdirs()
        }
    }
}

private fun startYtDlpDownload(
    context: Context,
    url: String,
    onProgress: (Float, String) -> Unit,
    onFinish: (Result<File>) -> Unit
) {
    val scope = CoroutineScope(Dispatchers.IO)

    scope.launch {
        try {
            val outputDir = buildOutputDirectory(context)
            val template = File(outputDir, "%(title).150B.%(ext)s").absolutePath

            val request = YtDlpRequest(url)
                .setOutputTemplate(template)
                .addOption("--no-playlist")
                .addOption(
                    "-f",
                    "best[height<=720][vcodec!=none][acodec!=none]/best[vcodec!=none][acodec!=none]"
                )

            YtDlp.executeAsync(request) { progress, _, line ->
                val cleanProgress = progress.coerceIn(0, 100).toFloat() / 100f
                scope.launch(Dispatchers.Main) {
                    onProgress(cleanProgress, line.takeLast(240))
                }
            }

            val downloaded = outputDir.listFiles()
                ?.filter {
                    it.isFile &&
                        it.length() > 0L &&
                        !it.name.endsWith(".part", ignoreCase = true)
                }
                ?.maxByOrNull { it.lastModified() }
                ?: throw IllegalStateException("No se encontró el archivo descargado.")

            withContext(Dispatchers.Main) {
                onFinish(Result.success(downloaded))
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onFinish(Result.failure(e))
            }
        }
    }
}

private fun openPayPal(context: Context) {
    val intent = Intent(
        Intent.ACTION_VIEW,
        android.net.Uri.parse(
            "https://www.paypal.com/donate/?business=clipnovaingrrn%40gmail.com&currency_code=USD&item_name=Apoyo%20a%20REMY.X"
        )
    )
    context.startActivity(intent)
}

@Composable
private fun RemyHome(context: Context) {
    var url by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(0f) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Pega un enlace para comenzar.") }
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = RemyBackground
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.remyx_logo),
                        contentDescription = "REMY.X",
                        modifier = Modifier.size(122.dp)
                    )
                    Text(
                        text = "REMY.X",
                        color = Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Download. Save. Done.",
                        color = RemyTextMuted,
                        fontSize = 16.sp
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it.trim()
                        status = if (url.isBlank()) {
                            "Pega un enlace para comenzar."
                        } else {
                            "Listo para analizar el enlace."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Enlace del video") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    enabled = !busy
                )
            }

            item {
                Button(
                    onClick = {
                        if (url.isBlank()) {
                            status = "Pega primero un enlace."
                            return@Button
                        }

                        busy = true
                        progress = 0f
                        status = "Analizando y descargando..."

                        startYtDlpDownload(
                            context = context,
                            url = url,
                            onProgress = { p, line ->
                                progress = p.coerceIn(0f, 1f)
                                if (line.isNotBlank()) {
                                    status = line
                                }
                            },
                            onFinish = { result ->
                                busy = false
                                result
                                    .onSuccess { file ->
                                        progress = 1f
                                        status = "Descarga terminada: ${file.name}"
                                        Toast
                                            .makeText(
                                                context,
                                                "Guardado en la carpeta privada de REMY.X",
                                                Toast.LENGTH_LONG
                                            )
                                            .show()
                                    }
                                    .onFailure { error ->
                                        status = "No se pudo descargar: ${error.message ?: "error desconocido"}"
                                    }
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy
                ) {
                    Text(if (busy) "Descargando..." else "Descargar")
                }
            }

            if (busy) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(progress = { progress })
                            Column {
                                Text("Descarga en curso", fontWeight = FontWeight.Bold)
                                Text(
                                    "${(progress * 100).toInt()}%",
                                    color = RemyTextMuted
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Estado", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(status, color = RemyTextMuted, fontSize = 13.sp)
                    }
                }
            }

            item {
                Text(
                    "Descargas",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Los archivos se preparan en el almacenamiento privado de REMY.X.",
                    color = RemyTextMuted,
                    fontSize = 12.sp
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = {
                            status = "Ajustes se añadirán después de estabilizar el motor."
                        }
                    ) {
                        Text("⚙ Ajustes")
                    }

                    Spacer(Modifier.size(10.dp))

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                openPayPal(context)
                            }
                        }
                    ) {
                        Text("♥ Apoyar REMY.X")
                    }
                }
            }
        }
    }
}
