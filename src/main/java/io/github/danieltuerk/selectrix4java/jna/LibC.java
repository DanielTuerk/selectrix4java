package io.github.danieltuerk.selectrix4java.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface LibC extends Library {
    LibC INSTANCE = Native.load("c", LibC.class);

    int open(String path, int flags);
    int read(int fd, byte[] buffer, int count);
    int write(int fd, byte[] buffer, int count);
    int close(int fd);

    int tcgetattr(int fd, Termios termios);
    int tcsetattr(int fd, int optional_actions, Termios termios);
}