package io.github.danieltuerk.selectrix4java.jna;

public interface SerialPort {
    void write(byte[] data);
   int read(byte[] buffer, int timeoutMs);
   int readNonBlocking(byte[] buffer);
   int available();
   void close();
}
