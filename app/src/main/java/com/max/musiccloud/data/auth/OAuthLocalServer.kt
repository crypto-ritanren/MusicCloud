package com.max.musiccloud.data.auth

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket

class OAuthLocalServer(
    val port: Int = 8888,
    private val onCodeReceived: (code: String, redirectUri: String) -> Unit,
    private val onError: (String) -> Unit
) {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    companion object {
        private const val TAG = "OAuthLocalServer"
    }

    fun start(scope: CoroutineScope) {
        stop()
        serverJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"))
                Log.d(TAG, "OAuth loopback server listening on 127.0.0.1:$port")

                val socket = serverSocket?.accept() ?: return@launch
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val firstLine = reader.readLine() // e.g. "GET /callback?code=4/0Ab... HTTP/1.1"

                Log.d(TAG, "Received HTTP request: $firstLine")

                val pathWithQuery = firstLine?.split(" ")?.getOrNull(1).orEmpty()
                val parsedUri = Uri.parse("http://localhost$pathWithQuery")
                val code = parsedUri.getQueryParameter("code")
                val error = parsedUri.getQueryParameter("error")

                val deepLinkUrl = "com.max.musiccloud://oauth2callback"
                val htmlSuccess = """
                    <!DOCTYPE html>
                    <html lang="ru">
                    <head>
                        <meta charset="utf-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>MusicCloud - Успешно</title>
                        <style>
                            body {
                                background-color: #0D0F15;
                                color: #F1F5F9;
                                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                                display: flex;
                                flex-direction: column;
                                align-items: center;
                                justify-content: center;
                                height: 100vh;
                                margin: 0;
                                text-align: center;
                                padding: 20px;
                            }
                            .card {
                                background: #161922;
                                border: 1px solid rgba(255,255,255,0.08);
                                border-radius: 24px;
                                padding: 36px 28px;
                                max-width: 380px;
                                box-shadow: 0 10px 30px rgba(0,0,0,0.5);
                            }
                            h1 { color: #00E5FF; font-size: 22px; margin-bottom: 10px; }
                            p { color: #94A3B8; font-size: 14px; line-height: 1.5; margin: 0 0 20px 0; }
                            .btn {
                                display: inline-block;
                                background: linear-gradient(135deg, #7C4DFF, #00E5FF);
                                color: white;
                                font-weight: bold;
                                text-decoration: none;
                                padding: 14px 28px;
                                border-radius: 16px;
                                font-size: 15px;
                                box-shadow: 0 4px 15px rgba(124, 77, 255, 0.4);
                                cursor: pointer;
                            }
                            .subtext { margin-top: 14px; font-size: 12px; color: #64748B; }
                        </style>
                        <script>
                            function returnToApp() {
                                window.location.href = "$deepLinkUrl";
                            }
                            setTimeout(returnToApp, 300);
                        </script>
                    </head>
                    <body>
                        <div class="card">
                            <h1>✓ Авторизация успешна!</h1>
                            <p>Доступ к Google Диску предоставлен. Возвращаемся в приложение...</p>
                            <a href="$deepLinkUrl" class="btn" onclick="returnToApp()">Открыть MusicCloud</a>
                            <div class="subtext">Если окно не закрылось само, нажмите на кнопку выше</div>
                        </div>
                    </body>
                    </html>
                """.trimIndent()

                val htmlError = """
                    <!DOCTYPE html>
                    <html>
                    <body style="background:#0D0F15;color:#EF4444;font-family:sans-serif;text-align:center;padding:50px;">
                        <h2>Ошибка авторизации</h2>
                        <p style="color:#94A3B8;">$error</p>
                    </body>
                    </html>
                """.trimIndent()

                val content = if (code != null) htmlSuccess else htmlError
                val contentBytes = content.toByteArray(Charsets.UTF_8)

                val response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html; charset=utf-8\r\n" +
                        "Content-Length: ${contentBytes.size}\r\n" +
                        "Connection: close\r\n\r\n"

                val out = socket.getOutputStream()
                out.write(response.toByteArray(Charsets.UTF_8))
                out.write(contentBytes)
                out.flush()
                socket.close()

                if (!code.isNullOrBlank()) {
                    onCodeReceived(code, "http://localhost:$port/callback")
                } else if (!error.isNullOrBlank()) {
                    onError(error)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Local server socket completed or closed: ${e.message}")
            } finally {
                stop()
            }
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        serverJob?.cancel()
        serverJob = null
    }
}
