package net.wbz.selectrix4java.device.serial;

import net.wbz.selectrix4java.block.BlockListener;
import net.wbz.selectrix4java.block.BlockModule;
import net.wbz.selectrix4java.block.FeedbackBlockListener;
import net.wbz.selectrix4java.block.FeedbackBlockModule;
import net.wbz.selectrix4java.device.DeviceAccessException;
import net.wbz.selectrix4java.device.DeviceManager;
import net.wbz.selectrix4java.train.TrainModule;
import org.junit.Test;

/**
 * TODO: how to realize changes from train by test device? Using BusDataPlayer / Recorder?
 *
 * @author Daniel Tuerk
 */
public class FeedbackBlockTest extends BaseTest {

    public FeedbackBlockTest() {
        super(new Connection(DEVICE_ID, DeviceManager.DEVICE_TYPE.TEST));
    }

//    @Test
    public void testBlock() throws DeviceAccessException, InterruptedException {

        final int blockAddress = 58;

        addConsoleBusAddressListener(1, blockAddress);
        addConsoleBusAddressListener(1, blockAddress + 1);
        addConsoleBusAddressListener(1, blockAddress + 2);

        BlockModule blockModule = getDevice().getBlockModule((byte) blockAddress);
        blockModule.addBlockListener(new BlockListener() {
            @Override
            public void blockOccupied(int blockNr) {
                print("occupied - nr: %d", blockNr);
            }

            @Override
            public void blockFreed(int blockNr) {
                print("freed - nr: %d", blockNr);
            }
        });

        FeedbackBlockModule feedbackBlockModule = getDevice().getFeedbackBlockModule((byte) blockAddress,
                (byte) (blockAddress + 2), (byte) (blockAddress + 1));
        feedbackBlockModule.addFeedbackBlockListener(new FeedbackBlockListener() {
            @Override
            public void trainEnterBlock(int blockNumber, int speed, byte trainAddress) {
                print("trainEnterBlock - nr: %d speed: %d train: %d", blockNumber,speed,trainAddress);
            }

            @Override
            public void trainLeaveBlock(int blockNumber, int speed, byte trainAddress) {
                print("trainLeaveBlock - nr: %d speed: %d train: %d", blockNumber,speed,trainAddress);
            }

            @Override
            public void blockOccupied(int blockNr) {
                print("blockOccupied - nr: %d", blockNr);
            }

            @Override
            public void blockFreed(int blockNr) {
                print("blockFreed - nr: %d", blockNr);
            }
        });

        getDevice().setRailVoltage(true);

        Thread.sleep(1000L);

        TrainModule trainModule = getDevice().getTrainModule((byte)7);
        trainModule.setDirection(TrainModule.DRIVING_DIRECTION.FORWARD);
        sleep();
        trainModule.setDrivingLevel(6);
        sleep();
        long drivingTime = 10000L;
        Thread.sleep(drivingTime);
        trainModule.setDirection(TrainModule.DRIVING_DIRECTION.BACKWARD);
        Thread.sleep(drivingTime);
        trainModule.setDrivingLevel(0);
        Thread.sleep(1000L);

        getDevice().setRailVoltage(false);

        Thread.sleep(1000L);
    }

//    @Override
//    public void tearDown() {
//        //TODO
//    }
    private void sleep(){
        try {
            Thread.sleep(100L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
