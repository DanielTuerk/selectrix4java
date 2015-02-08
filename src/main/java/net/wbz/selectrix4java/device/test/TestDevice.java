package net.wbz.selectrix4java.device.test;

import net.wbz.selectrix4java.bus.BusDataDispatcher;
import net.wbz.selectrix4java.data.BusDataChannel;
import net.wbz.selectrix4java.data.recording.BusDataRecorder;
import net.wbz.selectrix4java.data.recording.IsRecordable;
import net.wbz.selectrix4java.data.recording.RecordingException;
import net.wbz.selectrix4java.device.AbstractDevice;
import net.wbz.selectrix4java.device.DeviceAccessException;

import java.nio.file.Path;

/**
 * Simple test device which mock an connection.
 * The bus is simulated by the {@link net.wbz.selectrix4java.device.test.TestBus} for read and write operations.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class TestDevice extends AbstractDevice implements IsRecordable {

    private boolean connected = false;

    private final TestBus testBus = new TestBus();

    private final BusDataRecorder busDataRecorder =new BusDataRecorder();

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    protected BusDataChannel doConnect(BusDataDispatcher busDataDispatcher) throws DeviceAccessException {
        if (isConnected()) {
            throw new DeviceAccessException("already connected");
        }
        connected = true;
        return new BusDataChannel(testBus.getInputStream(), testBus.getOutputStream(), busDataDispatcher);
    }

    @Override
    public void doDisconnect() throws DeviceAccessException {
        connected = false;
    }

    @Override
    public void startRecording(Path destinationFolder) throws DeviceAccessException {
        if(isConnected()) {
            try {
                busDataRecorder.start(destinationFolder);
            getBusDataChannel().addBusDataReceiver(busDataRecorder);
            } catch (RecordingException e) {
                throw new DeviceAccessException("no recrding possible",e);
            }
        }
    }


    public Path stopRecording() throws DeviceAccessException {
        if(isRecording()) {
            getBusDataChannel().removeBusDataReceiver(busDataRecorder);
            busDataRecorder.stop();
            return busDataRecorder.getRecordOutput();
        }
        throw new DeviceAccessException("device isn't recording");
    }

    @Override
    public boolean isRecording() {
        return busDataRecorder.isRunning();
    }
}
