package vn.smarthome.data.ota

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class OtaManager(private val context: Context) {

    companion object {
        const val DEFAULT_REPO = "trungpq96dev/app_smart_home"
        private const val GITHUB_API_URL = "https://api.github.com/repos/%s/releases/latest"
        private val SHA256_REGEX = Regex("\\b([a-fA-F0-9]{64})\\b")

        /**
         * Tinh ma bam SHA-256 (chu thuong 64 ky tu hex) theo tung khoi 8KB
         * de khong ton RAM tren Tab A7 Lite.
         */
        fun computeSha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

        /**
         * Trich xuat chuoi SHA-256 64 ky tu hex tu file .sha256 (`sha256sum`),
         * truong `digest` cua GitHub API (`sha256:<hex>`), hoac dong mo ta release.
         */
        fun parseSha256(raw: String?): String? {
            if (raw.isNullOrBlank()) return null
            val cleaned = raw.trim().removePrefix("sha256:").removePrefix("SHA256:")
            return SHA256_REGEX.find(cleaned)?.groupValues?.getOrNull(1)?.lowercase()
        }

        /**
         * So sanh hang thoi gian (constant-time) SHA-256 cua file APK voi ma ky vong.
         */
        fun verifyApkSha256(apkFile: File, expectedSha256: String): Boolean {
            if (!apkFile.exists() || !apkFile.isFile) return false
            val normalizedExpected = parseSha256(expectedSha256) ?: return false
            val actual = computeSha256(apkFile)
            return MessageDigest.isEqual(
                actual.toByteArray(Charsets.UTF_8),
                normalizedExpected.toByteArray(Charsets.UTF_8),
            )
        }
    }

    /**
     * Lay phien ban app hien tai dang cai tren may.
     */
    fun getCurrentVersionName(): String {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: "0.1"
        } catch (e: Exception) {
            "0.1"
        }
    }

    /**
     * Kiem tra xem ban remote tren GitHub co moi hon ban local hay khong.
     * Vi du: remote "v1.0.2" moi hon local "1.0.1" hoac "0.1-ui".
     */
    fun isNewerVersion(remoteTag: String, localVersion: String): Boolean {
        val remoteNums = extractVersionNumbers(remoteTag)
        val localNums = extractVersionNumbers(localVersion)

        val maxLen = maxOf(remoteNums.size, localNums.size)
        for (i in 0 until maxLen) {
            val r = remoteNums.getOrElse(i) { 0 }
            val l = localNums.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        // Neu so bang nhau nhung chuoi khac nhau thi coi nhu co ban moi neu tag remote co hau to
        return false
    }

    private fun extractVersionNumbers(v: String): List<Int> {
        val cleaned = v.lowercase().removePrefix("v").trim()
        val parts = cleaned.split(".", "-", "_")
        return parts.mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }
    }

    /**
     * Goi GitHub Releases API de kiem tra ban moi nhat.
     */
    suspend fun checkLatestRelease(repo: String = DEFAULT_REPO): Result<ReleaseInfo?> = withContext(Dispatchers.IO) {
        try {
            val apiUrl = String.format(GITHUB_API_URL, repo)
            val url = URL(apiUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.setRequestProperty("User-Agent", "SmartHome-OTA-Client")

            val responseCode = conn.responseCode
            if (responseCode == 404) {
                // Chua co release nao duoc tao tren GitHub
                return@withContext Result.success(null)
            }
            if (responseCode !in 200..299) {
                val errBody = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                return@withContext Result.failure(Exception("Lỗi máy chủ GitHub ($responseCode): $errBody"))
            }

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)

            val tagName = json.optString("tag_name", "")
            val name = json.optString("name", tagName)
            val notes = json.optString("body", "Không có mô tả cập nhật.")
            val publishedAt = json.optString("published_at", "")

            // Tim file APK va file checksum .sha256 trong danh sach assets
            val assets = json.optJSONArray("assets")
            var apkUrl = ""
            var apkSize = 0L
            var apkNameLower = ""
            var apkDigest: String? = null
            var sha256Url: String? = null

            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.optString("name", "").lowercase()
                    if (assetName.endsWith(".apk") && apkUrl.isBlank()) {
                        apkUrl = asset.optString("browser_download_url", "")
                        apkSize = asset.optLong("size", 0L)
                        apkNameLower = assetName
                        apkDigest = parseSha256(asset.optString("digest", ""))
                    } else if (
                        assetName.endsWith(".apk.sha256") ||
                        assetName.endsWith(".sha256") ||
                        assetName == "sha256sums.txt"
                    ) {
                        if (sha256Url.isNullOrBlank() || (apkNameLower.isNotBlank() && assetName.startsWith(apkNameLower))) {
                            sha256Url = asset.optString("browser_download_url", "").ifBlank { null }
                        }
                    }
                }
            }

            if (apkUrl.isBlank()) {
                return@withContext Result.failure(Exception("Bản phát hành $tagName không chứa file APK nào."))
            }

            val expectedSha256 = apkDigest ?: parseSha256(notes)

            Result.success(
                ReleaseInfo(
                    tagName = tagName,
                    name = name,
                    notes = notes,
                    downloadUrl = apkUrl,
                    sizeBytes = apkSize,
                    publishedAt = publishedAt,
                    sha256Url = sha256Url,
                    expectedSha256 = expectedSha256,
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Tai file APK ve bo nho dem va bao cao tien trinh theo %.
     */
    suspend fun downloadApk(
        downloadUrl: String,
        onProgress: (percent: Int, bytesRead: Long, totalBytes: Long) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        var currentUrl = downloadUrl
        var conn: HttpURLConnection
        var redirectCount = 0

        // GitHub Releases asset URL se redirect (302) sang objects.githubusercontent.com
        while (true) {
            val url = URL(currentUrl)
            conn = url.openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.setRequestProperty("User-Agent", "SmartHome-OTA-Client")

            val status = conn.responseCode
            if (status in 300..399) {
                val newUrl = conn.getHeaderField("Location")
                if (newUrl.isNullOrBlank() || redirectCount++ > 5) {
                    throw IllegalStateException("Quá nhiều lần chuyển hướng hoặc không tìm thấy liên kết tải.")
                }
                currentUrl = newUrl
                conn.disconnect()
            } else if (status in 200..299) {
                break
            } else {
                throw IllegalStateException("Không thể tải file APK (Mã lỗi $status)")
            }
        }

        val totalBytes = conn.contentLengthLong
        val targetDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val apkFile = File(targetDir, "SmartHome-update.apk")
        if (apkFile.exists()) {
            apkFile.delete()
        }

        var downloaded = 0L
        conn.inputStream.use { input: InputStream ->
            FileOutputStream(apkFile).use { output: FileOutputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var lastPercent = -1

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloaded += bytesRead

                    val percent = if (totalBytes > 0) ((downloaded * 100) / totalBytes).toInt() else 0
                    if (percent != lastPercent) {
                        lastPercent = percent
                        onProgress(percent, downloaded, totalBytes)
                    }
                }
                output.flush()
            }
        }

        apkFile
    }

    /**
     * Tai file .sha256 tu GitHub Release (neu co) hoac dung ma SHA-256 da doc tu metadata.
     */
    suspend fun fetchExpectedSha256(info: ReleaseInfo): String? = withContext(Dispatchers.IO) {
        info.expectedSha256?.let { parseSha256(it) }?.let { return@withContext it }
        val checksumUrl = info.sha256Url?.takeIf { it.isNotBlank() } ?: return@withContext null

        var currentUrl = checksumUrl
        var redirectCount = 0
        while (true) {
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("User-Agent", "SmartHome-OTA-Client")
            }
            val status = conn.responseCode
            if (status in 300..399) {
                val newUrl = conn.getHeaderField("Location")
                conn.disconnect()
                if (newUrl.isNullOrBlank() || redirectCount++ > 5) {
                    throw IllegalStateException("Không tải được file SHA-256 (chuyển hướng lỗi).")
                }
                currentUrl = newUrl
            } else if (status in 200..299) {
                val content = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                return@withContext parseSha256(content)
            } else {
                conn.disconnect()
                throw IllegalStateException("Không tải được file SHA-256 (Mã lỗi $status).")
            }
        }
        @Suppress("UNREACHABLE_CODE")
        null
    }

    /**
     * Kich hoat trinh cai dat cua Android bang Intent FileProvider.
     */
    fun installApk(apkFile: File) {
        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}
