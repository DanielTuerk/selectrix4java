package io.github.danieltuerk.selectrix4java.device.serial.ffm;

public class SerialConfig {

    private final int baudRate;
    private final int dataBits;
    private final int stopBits;
    private final Parity parity;

    private SerialConfig(Builder b) {
        this.baudRate = b.baudRate;
        this.dataBits = b.dataBits;
        this.stopBits = b.stopBits;
        this.parity = b.parity;
    }

    public int baudRate() { return baudRate; }
    public int dataBits() { return dataBits; }
    public int stopBits() { return stopBits; }
    public Parity parity() { return parity; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private int baudRate = 9600;
        private int dataBits = 8;
        private int stopBits = 1;
        private Parity parity = Parity.NONE;

        public Builder baudRate(int val) { this.baudRate = val; return this; }
        public Builder dataBits(int val) { this.dataBits = val; return this; }
        public Builder stopBits(int val) { this.stopBits = val; return this; }
        public Builder parity(Parity val) { this.parity = val; return this; }

        public SerialConfig build() {
            return new SerialConfig(this);
        }
    }
}