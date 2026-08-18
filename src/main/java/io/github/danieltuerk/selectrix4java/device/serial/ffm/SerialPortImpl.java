package io.github.danieltuerk.selectrix4java.device.serial.ffm;

import io.github.danieltuerk.selectrix4java.device.serial.SerialPort;

public class SerialPortImpl implements SerialPort {

    private final SerialPortService service;

    private SerialPortImpl(SerialPortService service) {
        this.service = service;
    }

    public static SerialPortImpl open(SerialPortService service, String port, SerialConfig config) {
        service.open(port, config);
        return new SerialPortImpl(service);
    }

    public void write(byte[] data) {
        service.write(data);
    }

    public int read(byte[] buffer, int timeoutMs) {
        return service.read(buffer, timeoutMs);
    }

    public int readNonBlocking(byte[] buffer) {
        return service.read(buffer, 0);
    }

    public int available() {
        return service.available();
    }

    public void close() {
        service.close();
    }
}