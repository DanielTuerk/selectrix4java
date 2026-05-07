package net.wbz.selectrix4java.jna;

import com.sun.jna.Structure;
import java.util.List;

public class Termios extends Structure {

    public int c_iflag;   // input modes
    public int c_oflag;   // output modes
    public int c_cflag;   // control modes
    public int c_lflag;   // local modes
    public byte[] c_cc = new byte[32]; // control characters

    public int c_ispeed;  // input speed
    public int c_ospeed;  // output speed

    @Override
    protected List<String> getFieldOrder() {
        return List.of(
            "c_iflag", "c_oflag", "c_cflag", "c_lflag",
            "c_cc", "c_ispeed", "c_ospeed"
        );
    }
}
