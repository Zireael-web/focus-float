package com.focusfloat.app.pause.shizuku

import android.annotation.SuppressLint
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.os.Process
import java.io.InputStream
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SuppressLint("BlockedPrivateApi")
class ShizukuShellUserService : Binder() {
    init {
        attachInterface(null, INTERFACE_DESCRIPTOR)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        if (!isCustomTransaction(code)) return super.onTransact(code, data, reply, flags)
        data.enforceInterface(INTERFACE_DESCRIPTOR)
        if (reply == null) return false
        val result = runCatching {
            when (code) {
                TRANSACTION_EXEC -> {
                    val args = data.createStringArray()?.toList().orEmpty()
                    exec(args)
                }
                TRANSACTION_WELLBEING_SUSPENSION -> {
                    val packageNames = data.createStringArray() ?: emptyArray()
                    val suspended = data.readInt() == 1
                    val targetUserId = data.readInt()
                    val suspendingUserId = data.readInt()
                    val dialogMessage = data.readString()
                    setWellbeingSuspension(
                        packageNames = packageNames,
                        suspended = suspended,
                        targetUserId = targetUserId,
                        suspendingUserId = suspendingUserId,
                        dialogMessage = dialogMessage,
                    )
                }
                TRANSACTION_WELLBEING_STATUS -> {
                    val targetUserId = data.readInt()
                    wellbeingStatus(targetUserId)
                }
                else -> error("Unsupported transaction: $code")
            }
        }.getOrElse { error ->
            ShellServiceResult(1, "", "${error.javaClass.name}: ${error.message ?: error.toString()}")
        }
        reply.writeInt(result.exitCode)
        reply.writeString(result.stdout)
        reply.writeString(result.stderr)
        return true
    }

    private fun isCustomTransaction(code: Int): Boolean {
        return code == TRANSACTION_EXEC ||
            code == TRANSACTION_WELLBEING_SUSPENSION ||
            code == TRANSACTION_WELLBEING_STATUS
    }

    @Suppress("unused")
    fun destroy() {
        System.exit(0)
    }

    private fun exec(args: List<String>): ShellServiceResult {
        if (args.isEmpty()) return ShellServiceResult(2, "", "Shell command args cannot be empty")
        validateAllowedExec(args)?.let { reason ->
            return ShellServiceResult(2, "", reason)
        }
        val process = try {
            ProcessBuilder(args).start()
        } catch (error: Throwable) {
            return ShellServiceResult(1, "", "Could not start command: ${error.message ?: error.toString()}")
        }
        val executor = Executors.newFixedThreadPool(2)
        val stdout = executor.submit<String> { process.inputStream.readLimited() }
        val stderr = executor.submit<String> { process.errorStream.readLimited() }
        return try {
            val finished = process.waitFor(SHELL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                ShellServiceResult(
                    exitCode = 124,
                    stdout = stdout.get(2, TimeUnit.SECONDS),
                    stderr = (stderr.get(2, TimeUnit.SECONDS) + "\nShell command timed out after ${SHELL_TIMEOUT_MS}ms").trim(),
                )
            } else {
                ShellServiceResult(
                    exitCode = process.exitValue(),
                    stdout = stdout.get(2, TimeUnit.SECONDS),
                    stderr = stderr.get(2, TimeUnit.SECONDS),
                )
            }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun validateAllowedExec(args: List<String>): String? {
        if (args.size < 6 || args[0] != "cmd" || args[1] != "package") {
            return "Command is not allowed"
        }
        return when (args[2]) {
            "suspend" -> validateSuspendCommand(args)
            "unsuspend" -> validateUnsuspendCommand(args)
            else -> "Package command is not allowed"
        }
    }

    private fun validateSuspendCommand(args: List<String>): String? {
        if (Process.myUid() == ROOT_UID) return "Root shell suspend is disabled; use Wellbeing pause"
        if (args.size != 6 && args.size != 8) return "Unexpected suspend command shape"
        if (args[3] != "--user") return "Suspend command must specify --user"
        validateUserId(args[4])?.let { return it }
        val packageName = if (args.size == 8) {
            if (args[5] != "--dialogMessage") return "Unexpected suspend command option"
            if (args[6].length > MAX_DIALOG_MESSAGE_LENGTH) return "Dialog message is too long"
            args[7]
        } else {
            args[5]
        }
        validatePackageName(packageName)?.let { return it }
        return if (packageName in SERVICE_PROTECTED_PACKAGES) "$packageName is protected" else null
    }

    private fun validateUnsuspendCommand(args: List<String>): String? {
        if (args.size != 6) return "Unexpected unsuspend command shape"
        if (args[3] != "--user") return "Unsuspend command must specify --user"
        validateUserId(args[4])?.let { return it }
        val packageName = args[5]
        validatePackageName(packageName)?.let { return it }
        return if (packageName in SERVICE_PROTECTED_PACKAGES) "$packageName is protected" else null
    }

    private fun wellbeingStatus(targetUserId: Int): ShellServiceResult {
        if (Process.myUid() != ROOT_UID) {
            return ShellServiceResult(
                exitCode = 126,
                stdout = "uid=${Process.myUid()}",
                stderr = "Root Shizuku required for Wellbeing-owned package suspension",
            )
        }
        validateUserId(targetUserId)?.let { return ShellServiceResult(2, "uid=${Process.myUid()}", it) }
        return runCatching {
            allowHiddenApiReflection()
            val packageManager = packageManagerBinder()
            val suspendingPackage = resolveWellbeingPackage(packageManager, targetUserId)
                ?: error("SYSTEM_WELLBEING role holder was not found for user $targetUserId")
            val suspendMethod = packageManager.javaClass.methods.firstOrNull {
                it.name == "setPackagesSuspendedAsUser" && it.parameterTypes.size == 9
            } ?: error("IPackageManager.setPackagesSuspendedAsUser signature not found")
            val stateMethod = packageManager.javaClass.methods.firstOrNull {
                it.name == "isPackageSuspendedForUser" && it.parameterTypes.size == 2
            } ?: error("IPackageManager.isPackageSuspendedForUser signature not found")
            ShellServiceResult(
                exitCode = 0,
                stdout = "uid=${Process.myUid()}; suspendingPackage=$suspendingPackage; suspendMethod=${suspendMethod.name}; stateMethod=${stateMethod.name}",
                stderr = "",
            )
        }.getOrElse { error ->
            val cause = (error as? InvocationTargetException)?.targetException ?: error
            ShellServiceResult(
                exitCode = 1,
                stdout = "uid=${Process.myUid()}",
                stderr = "${cause.javaClass.name}: ${cause.message ?: cause.toString()}",
            )
        }
    }

    private fun setWellbeingSuspension(
        packageNames: Array<String>,
        suspended: Boolean,
        targetUserId: Int,
        suspendingUserId: Int,
        dialogMessage: String?,
    ): ShellServiceResult {
        if (packageNames.isEmpty()) return ShellServiceResult(2, "", "No packages provided")
        if (packageNames.size > MAX_PACKAGE_BATCH_SIZE) return ShellServiceResult(2, "", "Too many packages")
        validateUserId(targetUserId)?.let { return ShellServiceResult(2, "", it) }
        if (suspendingUserId != targetUserId) {
            return ShellServiceResult(2, "", "Suspending user must match target user")
        }
        val distinctPackages = packageNames.distinct()
        if (distinctPackages.size != packageNames.size) return ShellServiceResult(2, "", "Duplicate packages are not allowed")
        distinctPackages.forEach { packageName ->
            validatePackageName(packageName)?.let { return ShellServiceResult(2, "", it) }
            if (packageName in SERVICE_PROTECTED_PACKAGES) {
                return ShellServiceResult(2, "", "$packageName is protected")
            }
        }
        if ((dialogMessage?.length ?: 0) > MAX_DIALOG_MESSAGE_LENGTH) {
            return ShellServiceResult(2, "", "Dialog message is too long")
        }
        if (Process.myUid() != ROOT_UID) {
            return ShellServiceResult(
                exitCode = 126,
                stdout = "uid=${Process.myUid()}",
                stderr = "Root Shizuku required for Wellbeing-owned package suspension",
            )
        }

        return runCatching {
            allowHiddenApiReflection()
            val packageManager = packageManagerBinder()
            val suspendingPackage = resolveWellbeingPackage(packageManager, targetUserId)
                ?: error("SYSTEM_WELLBEING role holder was not found for user $targetUserId")
            val dialogInfo = if (suspended && !dialogMessage.isNullOrBlank()) {
                buildSuspendDialogInfo(dialogMessage)
            } else {
                null
            }
            val method = packageManager.javaClass.methods.firstOrNull {
                it.name == "setPackagesSuspendedAsUser" && it.parameterTypes.size == 9
            } ?: error("IPackageManager.setPackagesSuspendedAsUser signature not found")
            method.isAccessible = true
            val failed = method.invoke(
                packageManager,
                distinctPackages.toTypedArray(),
                suspended,
                null,
                null,
                dialogInfo,
                0,
                suspendingPackage,
                suspendingUserId,
                targetUserId,
            ) as? Array<*> ?: emptyArray<String>()

            val failedPackages = failed.filterIsInstance<String>()
            val states = distinctPackages.joinToString(", ") { packageName ->
                "$packageName=${packageSuspensionStateForUser(packageManager, packageName, targetUserId)}"
            }
            val stdout = buildString {
                append("uid=${Process.myUid()}; suspendingPackage=$suspendingPackage; suspended=$suspended; ")
                append("targetUserId=$targetUserId; states=[$states]")
                if (failedPackages.isNotEmpty()) {
                    append("; rejectedPackages=${failedPackages.joinToString(",")}")
                }
            }
            val stderr = if (failedPackages.isEmpty()) "" else "PackageManager rejected: ${failedPackages.joinToString()}"
            ShellServiceResult(0, stdout, stderr)
        }.getOrElse { error ->
            val cause = (error as? InvocationTargetException)?.targetException ?: error
            ShellServiceResult(
                exitCode = 1,
                stdout = "uid=${Process.myUid()}; suspended=$suspended",
                stderr = "${cause.javaClass.name}: ${cause.message ?: cause.toString()}",
            )
        }
    }

    private fun packageManagerBinder(): Any {
        val serviceManager = Class.forName("android.os.ServiceManager")
        val getService = serviceManager.getDeclaredMethod("getService", String::class.java)
        val binder = getService.invoke(null, "package") as IBinder
        val stub = Class.forName("android.content.pm.IPackageManager\$Stub")
        val asInterface = stub.getDeclaredMethod("asInterface", IBinder::class.java)
        return asInterface.invoke(null, binder)
            ?: error("Could not obtain IPackageManager binder")
    }

    private fun roleManagerBinder(): Any {
        val serviceManager = Class.forName("android.os.ServiceManager")
        val getService = serviceManager.getDeclaredMethod("getService", String::class.java)
        val binder = getService.invoke(null, "role") as IBinder
        val stub = Class.forName("android.app.role.IRoleManager\$Stub")
        val asInterface = stub.getDeclaredMethod("asInterface", IBinder::class.java)
        return asInterface.invoke(null, binder)
            ?: error("Could not obtain IRoleManager binder")
    }

    private fun resolveWellbeingPackage(packageManager: Any, userId: Int): String? {
        val roleManager = roleManagerBinder()
        val holdersMethod = roleManager.javaClass.methods.firstOrNull {
            it.name == "getRoleHoldersAsUser" && it.parameterTypes.size == 2
        } ?: error("IRoleManager.getRoleHoldersAsUser signature not found")
        val holders = holdersMethod.invoke(roleManager, SYSTEM_WELLBEING_ROLE, userId) as? List<*>
            ?: return null
        val installed = holders
            .filterIsInstance<String>()
            .filter { validatePackageName(it) == null }
            .filter { isPackageInstalledForUser(packageManager, it, userId) }
        return installed.singleOrNull()
    }

    private fun isPackageInstalledForUser(packageManager: Any, packageName: String, userId: Int): Boolean {
        return runCatching {
            val method = packageManager.javaClass.methods.firstOrNull {
                it.name == "getPackageUid" && it.parameterTypes.size == 3
            } ?: return@runCatching false
            val uid = method.invoke(packageManager, packageName, 0L, userId) as? Int ?: return@runCatching false
            uid >= 0
        }.getOrDefault(false)
    }

    private fun buildSuspendDialogInfo(message: String): Any? {
        return runCatching {
            val builderClass = Class.forName("android.content.pm.SuspendDialogInfo\$Builder")
            val builder = builderClass.getDeclaredConstructor().newInstance()
            builderClass.methods.firstOrNull {
                it.name == "setMessage" &&
                    it.parameterTypes.size == 1 &&
                    it.parameterTypes[0] == String::class.java
            }?.invoke(builder, message)
            builderClass.methods.first { it.name == "build" && it.parameterTypes.isEmpty() }.invoke(builder)
        }.getOrNull()
    }

    private fun isPackageSuspendedForUser(packageManager: Any, packageName: String, userId: Int): Boolean {
        val method = packageManager.javaClass.methods.firstOrNull {
            it.name == "isPackageSuspendedForUser" && it.parameterTypes.size == 2
        } ?: return false
        method.isAccessible = true
        return method.invoke(packageManager, packageName, userId) as? Boolean ?: false
    }

    private fun packageSuspensionStateForUser(packageManager: Any, packageName: String, userId: Int): String {
        return runCatching { isPackageSuspendedForUser(packageManager, packageName, userId).toString() }
            .getOrElse { error -> "unknown:${error.javaClass.simpleName}" }
    }

    private fun allowHiddenApiReflection() {
        runCatching {
            val vmRuntimeClass = Class.forName("dalvik.system.VMRuntime")
            val runtime = vmRuntimeClass.getDeclaredMethod("getRuntime").invoke(null)
            val setHiddenApiExemptions = vmRuntimeClass.getDeclaredMethod(
                "setHiddenApiExemptions",
                Array<String>::class.java,
            )
            setHiddenApiExemptions.invoke(runtime, arrayOf("L"))
        }
    }

    private fun validateUserId(rawUserId: String): String? {
        val userId = rawUserId.toIntOrNull() ?: return "Invalid user id"
        return validateUserId(userId)
    }

    private fun validateUserId(userId: Int): String? {
        return if (userId in 0..MAX_USER_ID) null else "Invalid user id"
    }

    private fun validatePackageName(packageName: String): String? {
        if (packageName.length !in 3..MAX_PACKAGE_NAME_LENGTH) return "Invalid package name"
        return if (PACKAGE_NAME_REGEX.matches(packageName)) null else "Invalid package name"
    }

    private fun InputStream.readLimited(): String {
        return bufferedReader().use { reader ->
            val buffer = CharArray(512)
            val out = StringBuilder()
            while (out.length < MAX_STREAM_CHARS) {
                val toRead = minOf(buffer.size, MAX_STREAM_CHARS - out.length)
                val read = reader.read(buffer, 0, toRead)
                if (read <= 0) break
                out.append(buffer, 0, read)
            }
            out.toString()
        }
    }

    data class ShellServiceResult(
        val exitCode: Int,
        val stdout: String,
        val stderr: String,
    )

    companion object {
        internal const val INTERFACE_DESCRIPTOR = "com.focusfloat.app.pause.shizuku.ShizukuShellUserService"
        const val TRANSACTION_EXEC = IBinder.FIRST_CALL_TRANSACTION + 10
        const val TRANSACTION_WELLBEING_SUSPENSION = IBinder.FIRST_CALL_TRANSACTION + 11
        const val TRANSACTION_WELLBEING_STATUS = IBinder.FIRST_CALL_TRANSACTION + 12
        private const val ROOT_UID = 0
        private const val SHELL_TIMEOUT_MS = 30_000L
        private const val MAX_STREAM_CHARS = 4_000
        private const val MAX_DIALOG_MESSAGE_LENGTH = 240
        private const val MAX_PACKAGE_NAME_LENGTH = 255
        private const val MAX_PACKAGE_BATCH_SIZE = 100
        private const val MAX_USER_ID = 999
        private const val SYSTEM_WELLBEING_ROLE = "android.app.role.SYSTEM_WELLBEING"
        private val PACKAGE_NAME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
        private val SERVICE_PROTECTED_PACKAGES = setOf(
            "android",
            "com.android.shell",
            "com.android.settings",
            "com.android.systemui",
            "com.focusfloat.app",
            "com.android.permissioncontroller",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.google.android.apps.nbu.files",
            "com.android.phone",
            "com.google.android.dialer",
            "com.google.android.gms",
            "com.android.vending",
            "com.google.android.apps.wellbeing",
            "com.google.android.apps.nexuslauncher",
            "com.android.launcher3",
            "com.google.android.permissioncontroller",
            "moe.shizuku.privileged.api",
        )
    }
}
