module io.github.danieltuerk.selectrix4java {

    requires org.slf4j;
    requires com.google.gson;
    // compile-time only: hosted Feature/RuntimeForeignAccess API, used solely during
    // native-image builds (see ffm.nativeimage.SerialFfmFeature), never needed at run time
    requires static org.graalvm.nativeimage;

    exports io.github.danieltuerk.selectrix4java;
    exports io.github.danieltuerk.selectrix4java.block;
    exports io.github.danieltuerk.selectrix4java.bus;
    exports io.github.danieltuerk.selectrix4java.bus.consumption;
    exports io.github.danieltuerk.selectrix4java.data;
    exports io.github.danieltuerk.selectrix4java.data.recording;
    exports io.github.danieltuerk.selectrix4java.device;
    exports io.github.danieltuerk.selectrix4java.device.serial;
    exports io.github.danieltuerk.selectrix4java.device.station;
    exports io.github.danieltuerk.selectrix4java.device.test;
    exports io.github.danieltuerk.selectrix4java.train;

    // gson (de)serializes these via reflection
    opens io.github.danieltuerk.selectrix4java.data.recording to com.google.gson;
}
