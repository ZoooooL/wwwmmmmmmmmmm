package com.example.odooopenaichecker

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val odooUrl = findViewById<EditText>(R.id.odooUrl)
        val odooDb = findViewById<EditText>(R.id.odooDb)
        val odooUsername = findViewById<EditText>(R.id.odooUsername)
        val odooApiKey = findViewById<EditText>(R.id.odooApiKey)
        val openaiApiKey = findViewById<EditText>(R.id.openaiApiKey)
        val resultText = findViewById<TextView>(R.id.resultText)
        val button = findViewById<Button>(R.id.checkButton)

        button.setOnClickListener {
            val values = InputValues(
                odooUrl = odooUrl.text.toString().trim(),
                odooDb = odooDb.text.toString().trim(),
                odooUsername = odooUsername.text.toString().trim(),
                odooApiKey = odooApiKey.text.toString().trim(),
                openaiApiKey = openaiApiKey.text.toString().trim()
            )

            resultText.text = "Checking..."
            Thread {
                val result = runChecks(values)
                runOnUiThread { resultText.text = result }
            }.start()
        }
    }

    private fun runChecks(values: InputValues): String {
        val odooResult = checkOdoo(values)
        val openAiResult = checkOpenAI(values.openaiApiKey)
        return "Odoo: $odooResult\n\nOpenAI: $openAiResult"
    }

    private fun checkOdoo(values: InputValues): String {
        if (!values.odooUrl.startsWith("http://") && !values.odooUrl.startsWith("https://")) {
            return "Invalid Odoo URL. Must start with http:// or https://"
        }
        if (values.odooDb.isEmpty() || values.odooUsername.isEmpty() || values.odooApiKey.isEmpty()) {
            return "Missing Odoo DB/Username/API Key"
        }

        return try {
            val endpoint = values.odooUrl.trimEnd('/') + "/xmlrpc/2/common"
            val payload = """
                <?xml version="1.0"?>
                <methodCall>
                  <methodName>authenticate</methodName>
                  <params>
                    <param><value><string>${xmlEscape(values.odooDb)}</string></value></param>
                    <param><value><string>${xmlEscape(values.odooUsername)}</string></value></param>
                    <param><value><string>${xmlEscape(values.odooApiKey)}</string></value></param>
                    <param><value><struct></struct></value></param>
                  </params>
                </methodCall>
            """.trimIndent()

            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "text/xml")

            OutputStreamWriter(conn.outputStream).use { it.write(payload) }

            val response = (if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()
                ?.use(BufferedReader::readText)
                .orEmpty()

            val uid = Regex("<int>(\\d+)</int>").find(response)?.groupValues?.get(1)
            if (uid != null && uid != "0") {
                "Connected (uid=$uid)"
            } else {
                "Authentication failed"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private fun checkOpenAI(apiKey: String): String {
        if (apiKey.isEmpty()) return "Missing OpenAI API Key"

        return try {
            val conn = URL("https://api.openai.com/v1/models").openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.setRequestProperty("Authorization", "Bearer $apiKey")

            val responseBody = (if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()
                ?.use(BufferedReader::readText)
                .orEmpty()

            if (conn.responseCode == 200) {
                "Connected (HTTP 200)"
            } else {
                "Failed (HTTP ${conn.responseCode}): ${responseBody.take(120)}"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private fun xmlEscape(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}

data class InputValues(
    val odooUrl: String,
    val odooDb: String,
    val odooUsername: String,
    val odooApiKey: String,
    val openaiApiKey: String
)
