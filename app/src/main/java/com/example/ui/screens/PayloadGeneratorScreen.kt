package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionMode
import com.example.ui.VpnViewModel
import com.example.ui.theme.VpnRedPrimary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PayloadGeneratorScreen(
    viewModel: VpnViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val config by viewModel.config.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var urlHost by remember { mutableStateOf(config.payloadUrl) }
    var selectedMethod by remember { mutableStateOf(config.payloadMethod) }
    var injectionMethod by remember { mutableStateOf(config.payloadInjection) }
    var payloadOutput by remember { mutableStateOf(config.payloadString) }

    // Checkbox extra headers
    var keepAlive by remember { mutableStateOf(true) }
    var onlineHost by remember { mutableStateOf(true) }
    var userAgent by remember { mutableStateOf(false) }
    var reverseProxy by remember { mutableStateOf(false) }
    var forwardHost by remember { mutableStateOf(false) }

    fun generatePayload() {
        val hostTarget = urlHost.ifBlank { "[host]" }
        val sb = StringBuilder()
        when (injectionMethod) {
            "Front Inject" -> {
                sb.append("GET http://$hostTarget/ HTTP/1.1[crlf]")
                sb.append("Host: $hostTarget[crlf]")
                if (onlineHost) sb.append("X-Online-Host: $hostTarget[crlf]")
                if (forwardHost) sb.append("X-Forward-Host: $hostTarget[crlf]")
                if (reverseProxy) sb.append("X-Forwarded-For: 127.0.0.1[crlf]")
                if (userAgent) sb.append("User-Agent: Mozilla/5.0 (Android; Mobile)[crlf]")
                if (keepAlive) sb.append("Connection: Keep-Alive[crlf][crlf]")
                sb.append("$selectedMethod [host_port] [protocol][crlf][crlf]")
            }
            "Back Inject" -> {
                sb.append("$selectedMethod [host_port] [protocol][crlf]")
                sb.append("Host: $hostTarget[crlf]")
                if (keepAlive) sb.append("Connection: Keep-Alive[crlf]")
                sb.append("[crlf]GET http://$hostTarget/ HTTP/1.1[crlf]")
                if (onlineHost) sb.append("X-Online-Host: $hostTarget[crlf][crlf]")
            }
            else -> { // Normal
                sb.append("$selectedMethod [host_port] [protocol][crlf]")
                sb.append("Host: $hostTarget[crlf]")
                if (onlineHost) sb.append("X-Online-Host: $hostTarget[crlf]")
                if (forwardHost) sb.append("X-Forward-Host: $hostTarget[crlf]")
                if (reverseProxy) sb.append("X-Forwarded-For: 127.0.0.1[crlf]")
                if (userAgent) sb.append("User-Agent: MutigandaVpn/1.0[crlf]")
                if (keepAlive) sb.append("Connection: Keep-Alive[crlf]")
                sb.append("[crlf]")
            }
        }
        payloadOutput = sb.toString()
        Toast.makeText(context, "Payload generated!", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Payload Generator",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("payload_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VpnRedPrimary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "URL / Bug Host",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = VpnRedPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = urlHost,
                onValueChange = { urlHost = it },
                placeholder = { Text("e.g. bug.host.com or [host]") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payload_url_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Request Method",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = VpnRedPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("CONNECT", "GET", "POST", "HEAD", "PUT", "OPTIONS", "TRACE", "PATCH").forEach { m ->
                    FilterChip(
                        selected = selectedMethod == m,
                        onClick = { selectedMethod = m },
                        label = { Text(m, fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VpnRedPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Injection Method",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = VpnRedPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Normal", "Front Inject", "Back Inject").forEach { inj ->
                    FilterChip(
                        selected = injectionMethod == inj,
                        onClick = { injectionMethod = inj },
                        label = { Text(inj, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VpnRedPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Extra Headers",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = VpnRedPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = keepAlive,
                    onCheckedChange = { keepAlive = it },
                    colors = CheckboxDefaults.colors(checkedColor = VpnRedPrimary)
                )
                Text("Keep-Alive", fontSize = 13.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Checkbox(
                    checked = onlineHost,
                    onCheckedChange = { onlineHost = it },
                    colors = CheckboxDefaults.colors(checkedColor = VpnRedPrimary)
                )
                Text("Online Host", fontSize = 13.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = userAgent,
                    onCheckedChange = { userAgent = it },
                    colors = CheckboxDefaults.colors(checkedColor = VpnRedPrimary)
                )
                Text("User-Agent", fontSize = 13.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Checkbox(
                    checked = forwardHost,
                    onCheckedChange = { forwardHost = it },
                    colors = CheckboxDefaults.colors(checkedColor = VpnRedPrimary)
                )
                Text("Forward Host", fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { generatePayload() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("generate_payload_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = VpnRedPrimary)
            ) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Payload", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Payload Configuration String",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = VpnRedPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = payloadOutput,
                onValueChange = { payloadOutput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .testTag("payload_output_box"),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(payloadOutput))
                        Toast.makeText(context, "Payload copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy")
                }
                Button(
                    onClick = {
                        val updated = config.copy(
                            payloadString = payloadOutput,
                            payloadUrl = urlHost,
                            payloadMethod = selectedMethod,
                            payloadInjection = injectionMethod,
                            connectionMode = ConnectionMode.CUSTOM_PAYLOAD,
                            isCustomSetupEnabled = true
                        )
                        viewModel.updateConfig(updated)
                        Toast.makeText(context, "Applied to Custom Payload profile!", Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = VpnRedPrimary)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Apply & Save")
                }
            }
        }
    }
}
