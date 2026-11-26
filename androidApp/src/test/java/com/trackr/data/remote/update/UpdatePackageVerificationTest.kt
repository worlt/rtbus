package com.trackr.data.remote.update

import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.content.pm.SigningInfo
import com.trackr.core.model.AppUpdateInfo
import com.trackr.core.model.AppUpdateInstallState
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import java.io.File

/** 用 PackageManager 本地夹具走真实安装器校验；不替代真实签名 APK 升级验收。 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class UpdatePackageVerificationTest {
    private val context = RuntimeEnvironment.getApplication()
    private val packageManager = shadowOf(context.packageManager)
    private val target = File(context.cacheDir, "updates/trackr-6.apk")

    @Test
    fun wrongPackage_isRejectedAndDeleted() = runBlocking {
        assertRejected("other.app", 6, arrayOf(Signature("abcd")), "不是实时公交")
    }

    @Test
    fun sameOrOlderVersion_isRejectedAndDeleted() = runBlocking {
        for (version in listOf(4L, 5L)) {
            assertRejected(context.packageName, version, arrayOf(Signature("abcd")), "版本不正确")
        }
    }

    @Test
    fun differentSigner_isRejectedAndDeleted() = runBlocking {
        assertRejected(context.packageName, 6, arrayOf(Signature("dcba")), "签名与当前版本不一致")
    }

    @Test
    fun missingSigner_isRejectedAndDeleted() = runBlocking {
        assertRejected(context.packageName, 6, emptyArray(), "签名与当前版本不一致")
    }

    @Test
    fun sameSignerNewerVersion_reachesPermissionStepAndCanBeDiscarded() = runBlocking {
        configureArchive(context.packageName, 6, arrayOf(Signature("abcd")))
        val installer = installer()
        try {
            assertEquals(AppUpdateInstallState.AwaitingPermission, installer.downloadAndInstall(updateInfo))
            assertTrue(target.isFile)
            assertEquals(AppUpdateInstallState.AwaitingPermission, installer.retryPendingInstall())
            installer.discardPending()
            assertFalse(target.exists())
            assertTrue(installer.retryPendingInstall() is AppUpdateInstallState.Failed)
        } finally {
            installer.close()
        }
    }

    private suspend fun assertRejected(name: String, version: Long, signatures: Array<Signature>, reason: String) {
        configureArchive(name, version, signatures)
        val installer = installer()
        try {
            val result = installer.downloadAndInstall(updateInfo)
            assertTrue(result is AppUpdateInstallState.Failed)
            assertTrue((result as AppUpdateInstallState.Failed).message.contains(reason))
            assertFalse(target.exists())
            assertTrue(installer.retryPendingInstall() is AppUpdateInstallState.Failed)
        } finally {
            installer.close()
        }
    }

    private fun configureArchive(name: String, version: Long, signatures: Array<Signature>) {
        packageManager.getInternalMutablePackageInfo(context.packageName).apply {
            longVersionCode = 5
            signingInfo = signingInfo(arrayOf(Signature("abcd")))
        }
        packageManager.setPackageArchiveInfo(target.absolutePath, PackageInfo().apply {
            packageName = name
            longVersionCode = version
            signingInfo = signingInfo(signatures)
        })
        packageManager.setCanRequestPackageInstalls(false)
    }

    private fun signingInfo(signatures: Array<Signature>): SigningInfo =
        ReflectionHelpers.newInstance(SigningInfo::class.java).also {
            shadowOf(it).setSignatures(signatures)
        }

    private fun installer() = AndroidAppUpdateInstaller(context, HttpClient(MockEngine {
        respond(ByteArray(1024) { 7 })
    }))

    private val updateInfo = AppUpdateInfo(
        versionCode = 6,
        versionName = "0.1.2",
        changelog = "Local verification fixture",
        downloadUrl = "https://github.com/example/update-fixture/releases/download/v6/app.apk"
    )
}
