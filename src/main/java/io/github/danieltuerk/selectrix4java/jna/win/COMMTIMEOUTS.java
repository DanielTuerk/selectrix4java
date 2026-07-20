package io.github.danieltuerk.selectrix4java.jna.win;

import com.sun.jna.Structure;

import java.util.List;

public class COMMTIMEOUTS extends Structure {

    public int ReadIntervalTimeout;
    public int ReadTotalTimeoutMultiplier;
    public int ReadTotalTimeoutConstant;
    public int WriteTotalTimeoutMultiplier;
    public int WriteTotalTimeoutConstant;

    @Override
    protected List<String> getFieldOrder() {
        return List.of(
            "ReadIntervalTimeout",
            "ReadTotalTimeoutMultiplier",
            "ReadTotalTimeoutConstant",
            "WriteTotalTimeoutMultiplier",
            "WriteTotalTimeoutConstant"
        );
    }
}
