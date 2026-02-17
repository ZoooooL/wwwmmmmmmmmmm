package com.example.odooopenaichecker

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

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

            val validationError = validateInputs(values)
            if (validationError != null) {
                resultText.text = validationError
                return@setOnClickListener
            }

            resultText.text = getString(R.string.checking)
            button.isEnabled = false
            Thread {
                val result = runChecks(values)
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        resultText.text = result
                        button.isEnabled = true
                    }
                }
            }.start()
        }
    }

    private fun validateInputs(values: InputValues): String? {
        if (!values.odooUrl.startsWith("http://") && !values.odooUrl.startsWith("https://")) {
            return getString(R.string.error_invalid_odoo_url)
        }
        try {
            URL(values.odooUrl)
        } catch (_: Exception) {
            return getString(R.string.error_invalid_odoo_url)
        }
        if (values.odooDb.isEmpty() || values.odooUsername.isEmpty() || values.odooApiKey.isEmpty()) {
            return getString(R.string.error_missing_odoo_fields)
        }
        if (values.openaiApiKey.isEmpty()) {
            return getString(R.string.error_missing_openai_key)
        }
        return null
    }

    private fun runChecks(values: InputValues): String {
        val odooResult = checkOdoo(values)
        val openAiResult = checkOpenAI(values.openaiApiKey)
        return getString(R.string.result_template, odooResult, openAiResult)
    }

    private fun checkOdoo(values: InputValues): String {
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

        var conn: HttpURLConnection? = null
        return try {
            conn = URL(endpoint).openConnection() as HttpURLConnection
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
                getString(R.string.status_odoo_connected, uid)
            } else {
                val fault = Regex("<name>faultString</name>\\s*<value><string>(.*?)</string></value>", RegexOption.DOT_MATCHES_ALL)
                    .find(response)
                    ?.groupValues
                    ?.get(1)
                    ?.replace("\\n", " ")
                    ?.trim()

                if (fault.isNullOrEmpty()) {
                    getString(R.string.status_odoo_auth_failed)
                } else {
                    getString(R.string.status_failed_http_with_reason, conn.responseCode.toString(), fault.take(140))
                }
            }
        } catch (_: UnknownHostException) {
            getString(R.string.status_error_network)
        } catch (_: SocketTimeoutException) {
            getString(R.string.status_error_network)
        } catch (e: Exception) {
            getString(R.string.status_error, e.message ?: getString(R.string.unknown_error))
        } finally {
            conn?.disconnect()
        }
    }

    private fun checkOpenAI(apiKey: String): String {
        var conn: HttpURLConnection? = null
        return try {
            conn = URL("https://api.openai.com/v1/models").openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.setRequestProperty("Accept", "application/json")

            val responseBody = (if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()
                ?.use(BufferedReader::readText)
                .orEmpty()

            if (conn.responseCode == 200) {
                getString(R.string.status_openai_connected)
            } else {
                getString(
                    R.string.status_failed_http_with_reason,
                    conn.responseCode.toString(),
                    responseBody.take(140)
                )
            }
        } catch (_: UnknownHostException) {
            getString(R.string.status_error_network)
        } catch (_: SocketTimeoutException) {
            getString(R.string.status_error_network)
        } catch (e: Exception) {
            getString(R.string.status_error, e.message ?: getString(R.string.unknown_error))
        } finally {
            conn?.disconnect()
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
