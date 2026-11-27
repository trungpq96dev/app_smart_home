package vn.smarthome.data.ota

import android.net.Uri

/**
 * Thong tin ban phat hanh lay tu GitHub Release.
 */
data class ReleaseInfo(
    val tagName: String,
    val name: String,
    val notes: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val publishedAt: String,
    val sha256Url: String? = null,
    val expectedSha256: String? = null,
) {
    val coMaSha256: Boolean
        get() = !sha256Url.isNullOrBlank() || !expectedSha256.isNullOrBlank()
}

/**
 * Trang thai cua qua trinh OTA Update.
 */
sealed interface OtaStatus {
    /** Khong lam gi / Mac dinh */
    data object Idling : OtaStatus

    /** Dang kiem tra ban moi tren GitHub */
    data object Checking : OtaStatus

    /** Da phat hien ban moi */
    data class UpdateAvailable(val info: ReleaseInfo) : OtaStatus

    /** Dang tai file APK ve may */
    data class Downloading(
        val progressPercent: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long,
    ) : OtaStatus

    /** Dang kiem tra toan ven SHA-256 truoc khi cho cai dat */
    data object Verifying : OtaStatus

    /** Da tai xong file APK, san sang cai dat */
    data class ReadyToInstall(
        val apkUri: Uri,
        val info: ReleaseInfo,
    ) : OtaStatus

    /** Ung dung da la ban moi nhat */
    data object UpToDate : OtaStatus

    /** Co loi xay ra (loi mang, khong tim thay file APK,...) */
    data class Error(val message: String) : OtaStatus
}
