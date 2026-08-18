package io.github.danieltuerk.selectrix4java.device.serial.ffm.nativeimage;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;

import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeForeignAccess;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * Registers, ahead of time, every native downcall shape made by {@code LibC}, {@code Kernel32}
 * and {@code Advapi32}. Those classes are initialized at native-image run time (they open real
 * shared libraries, which cannot be resolved reliably inside the image builder), so the analysis
 * never observes their {@code Linker.downcallHandle} calls on its own; without this Feature,
 * native-image reports "0 downcalls ... registered for foreign access" and every call fails at
 * run time with a MissingForeignRegistrationError. Descriptor shapes shared by several native
 * functions (e.g. two same-signature Win32 calls) only need to be registered once.
 * <p>
 * Registration is split by target platform: {@code Linker.Option.captureCallState} validates the
 * requested state name against the linker of the platform native-image is currently building
 * for, and "GetLastError" (Windows) / "errno" (POSIX) are mutually exclusive - registering both
 * unconditionally makes the build fail on whichever platform doesn't recognize the other's name.
 * <p>
 * Activated for any native-image build depending on this jar via
 * {@code META-INF/native-image/io.github.danieltuerk/selectrix4java/native-image.properties}.
 */
public final class SerialFfmFeature implements Feature {

    @Override
    public void duringSetup(DuringSetupAccess access) {
        if (Platform.includedIn(Platform.LINUX.class)) {
            registerLibC();
        }
        if (Platform.includedIn(Platform.WINDOWS.class)) {
            registerKernel32();
            registerAdvapi32();
        }
    }

    // io.github.danieltuerk.selectrix4java.device.serial.ffm.LibC
    private static void registerLibC() {
        Linker.Option captureErrno = Linker.Option.captureCallState("errno");
        RuntimeForeignAccess.registerForDowncall( // open
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT), captureErrno);
        RuntimeForeignAccess.registerForDowncall( // read, write
                FunctionDescriptor.of(JAVA_LONG, JAVA_INT, ADDRESS, JAVA_LONG), captureErrno);
        RuntimeForeignAccess.registerForDowncall( // close
                FunctionDescriptor.of(JAVA_INT, JAVA_INT));
        RuntimeForeignAccess.registerForDowncall( // tcgetattr
                FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS), captureErrno);
        RuntimeForeignAccess.registerForDowncall( // tcsetattr
                FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS), captureErrno);
    }

    // io.github.danieltuerk.selectrix4java.device.serial.ffm.win.Kernel32
    private static void registerKernel32() {
        Linker.Option captureLastError = Linker.Option.captureCallState("GetLastError");
        RuntimeForeignAccess.registerForDowncall( // CreateFileW
                FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS),
                captureLastError);
        RuntimeForeignAccess.registerForDowncall( // ReadFile, WriteFile
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS), captureLastError);
        RuntimeForeignAccess.registerForDowncall( // GetCommState, SetCommState, SetCommTimeouts
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS), captureLastError);
        RuntimeForeignAccess.registerForDowncall( // CloseHandle (shape shared with Advapi32#RegCloseKey)
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
    }

    // io.github.danieltuerk.selectrix4java.device.serial.ffm.win.Advapi32
    private static void registerAdvapi32() {
        RuntimeForeignAccess.registerForDowncall( // RegOpenKeyExW
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS));
        RuntimeForeignAccess.registerForDowncall( // RegEnumValueW
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
        // RegCloseKey(ADDRESS) -> JAVA_INT, no options: already registered in registerKernel32()
    }
}
