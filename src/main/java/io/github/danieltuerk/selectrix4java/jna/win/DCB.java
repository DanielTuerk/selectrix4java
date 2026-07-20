package io.github.danieltuerk.selectrix4java.jna.win;

import com.sun.jna.Structure;
import java.util.List;

public class DCB extends Structure {

    public int DCBlength = size();
    public int BaudRate;
    public int flags;
    public short wReserved;
    public short XonLim;
    public short XoffLim;
    public byte ByteSize;
    public byte Parity;
    public byte StopBits;
    public byte XonChar;
    public byte XoffChar;
    public byte ErrorChar;
    public byte EofChar;
    public byte EvtChar;
    public short wReserved1;

    @Override
    protected List<String> getFieldOrder() {
        return List.of(
            "DCBlength", "BaudRate", "flags", "wReserved",
            "XonLim", "XoffLim", "ByteSize", "Parity", "StopBits",
            "XonChar", "XoffChar", "ErrorChar", "EofChar", "EvtChar", "wReserved1"
        );
    }
}