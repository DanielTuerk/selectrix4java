package net.wbz.selectrix4java.jna;

public interface SerialPortService {

    void open(String port, SerialConfig config);

    void write(byte[] data);

    int read(byte[] buffer, int timeoutMs);

    int available();

    void close();
}