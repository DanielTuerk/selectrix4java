package io.github.danieltuerk.selectrix4java.device.serial.ffm;

public interface SerialPort {
    void write(byte[] data);
   int read(byte[] buffer, int timeoutMs);
   int readNonBlocking(byte[] buffer);
   int available();
   void close();
}
