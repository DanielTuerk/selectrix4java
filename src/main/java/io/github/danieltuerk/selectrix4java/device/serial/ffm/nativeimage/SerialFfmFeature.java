package io.github.danieltuerk.selectrix4java.device.serial.ffm.nativeimage;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;

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
 * Activated for any native-image build depending on this jar via
 * {@code META-INF/native-image/io.github.danieltuerk/selectrix4java/native-image.properties}.
 */
public final class SerialFfmFeature implements Feature {

    @Override
    public void duringSetup(DuringSetupAccess access) {
        Linker.Option captureErrno = Linker.Option.captureCallState("errno");
        Linker.Option captureLastError = Linker.Option.captureCallState("GetLastError");

        // LibC (Linux/POSIX) - io.github.danieltuerk.selectrix4java.device.serial.ffm.LibC
        RuntimeForeignAccess.registerForDowncall( // open
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT), captureErrno);
        RuntimeForeignAccess.registerForDowncall( // read, write
                FunctionDescriptor.of(JAVA_LONG, JAVA_INT, ADDRESS, JAVA_LONG), captureErrno);
        RuntimeForeignAccess.registerForDowncall( // tcgetattr
                FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS), captureErrno);
        RuntimeForeignAccess.registerForDowncall( // tcsetattr
                FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS), captureErrno);

        // Kernel32 (Windows) - io.github.danieltuerk.selectrix4java.device.serial.ffm.win.Kernel32
        RuntimeForeignAccess.registerForDowncall( // CreateFileW
                FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS),
                captureLastError);
        RuntimeForeignAccess.registerForDowncall( // ReadFile, WriteFile
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS), captureLastError);
        RuntimeForeignAccess.registerForDowncall( // GetCommState, SetCommState, SetCommTimeouts
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS), captureLastError);

        // Advapi32 (Windows) - io.github.danieltuerk.selectrix4java.device.serial.ffm.win.Advapi32
        RuntimeForeignAccess.registerForDowncall( // RegOpenKeyExW
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS));
        RuntimeForeignAccess.registerForDowncall( // RegEnumValueW
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

        // shared by both: CloseHandle(ADDRESS) and RegCloseKey(ADDRESS), no capture option
        RuntimeForeignAccess.registerForDowncall(FunctionDescriptor.of(JAVA_INT, ADDRESS));
    }
}
