package net.wbz.selectrix4java.block;

import com.google.common.collect.Maps;
import net.wbz.selectrix4java.bus.BusAddress;
import net.wbz.selectrix4java.bus.BusAddressListener;
import net.wbz.selectrix4java.train.TrainModule;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Block which use the feedback address to receive the train address on the block.
 * <p/>
 * Control the block to update the speed of each entering or exiting train on the block.
 * <p/>
 * <h2>Supported Devices:</h2>
 * <ul>
 * <li>D&H Belegtmelder 8i</li>
 * </ul>
 * <p/>
 * TODO
 * -Train still on block
 * -new train enter
 * -existing train exit
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class FeedbackBlockModule extends BlockModule {

    /**
     * TODO: need all trains + block + speed - Mapping of the train address with speed on block number.
     */
    private final Map<Integer, Integer> blockSpeedMapping = Maps.newConcurrentMap();
    private final Map<TrainModule, Integer> currentSpeedOfTrainOnBlock = new WeakHashMap<>();

    public FeedbackBlockModule(final Map<BusAddress, TrainModule> trainModules, BusAddress address,
                               BusAddress feedbackAddress, BusAddress additionalAddresses) {
        super(address, additionalAddresses);
        feedbackAddress.addListener(new BusAddressListener() {
            @Override
            public void dataChanged(byte oldValue, byte newValue) {

//                byte trainAddress = 0;
//                TrainModule trainModule = trainModules.get(trainAddress);
//
//                // existing train switch block
//
//                // train entering
//
//                currentSpeedOfTrainOnBlock.put(trainModule, speed);
//
//                // train leave
//                currentSpeedOfTrainOnBlock.remove(trainModule);
//
//                //TODO
//                int blockNumber = 0;
//
//                if (blockSpeedMapping.containsKey(blockNumber)) {
//                    TrainModule trainModule = trainModules.get(trainAddress);
//                    trainModule.setDrivingLevel(blockSpeedMapping.get(blockNumber));
//                }

            }
        });
    }

    /**
     * Set the target speed for trains which are on the block with the given number.
     *
     * @param blockNumber number of the block
     * @param targetSpeed speed for the trains on the block
     * @return {@link net.wbz.selectrix4java.block.FeedbackBlockModule}
     */
    public FeedbackBlockModule setBlockDrivingSpeed(int blockNumber, int targetSpeed) {
        return apply(blockNumber, targetSpeed);
    }

    /**
     * Set the target speed to zero for trains which are on the block with the given number.
     *
     * @param blockNumber number of the block
     * @return {@link net.wbz.selectrix4java.block.FeedbackBlockModule}
     */
    public FeedbackBlockModule setStop(int blockNumber) {
        return apply(blockNumber, 0);
    }

    /**
     * Apply data for trains which will entering block or still on the block.
     *
     * @param blockNumber number of the block
     * @param targetSpeed speed for the trains on the block
     * @return {@link net.wbz.selectrix4java.block.FeedbackBlockModule}
     */
    private FeedbackBlockModule apply(int blockNumber, int targetSpeed) {
        blockSpeedMapping.put(blockNumber, targetSpeed);

        //TODO: update train on block

        return this;
    }

}
