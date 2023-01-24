package hazelcast.platform.solutions.machineshop;

import com.hazelcast.map.IMap;
import hazelcast.platform.solutions.machineshop.domain.MachineStatusEvent;

public class MachineEmulator implements  Runnable{
    private final String serialNum;

    private  SignalGenerator tempSignalGenerator;
    private String targetURL;

    private final IMap<String, MachineStatusEvent> machineStatusMap;

    private int t;
    private SignalGenerator signalGenerator;

    public MachineEmulator(IMap<String,MachineStatusEvent> machineStatusMap, String sn, SignalGenerator signalGenerator){
        this.machineStatusMap = machineStatusMap;
        this.signalGenerator = signalGenerator;
        this.serialNum = sn;
        this.t = 0;
    }

    public void setSignalGenerator(SignalGenerator signalGenerator){
        this.signalGenerator = signalGenerator;
    }
    @Override
    public void run() {
        MachineStatusEvent status = new MachineStatusEvent();
        status.setSerialNum(serialNum);
        status.setEventTime(System.currentTimeMillis());
        status.setBitTemp(signalGenerator.compute(t++));
        status.setBitRPM(10000);
        status.setBitPositionX(0);
        status.setBitPositionY(0);
        status.setBitPositionZ(0);

        machineStatusMap.put(serialNum, status);
    }
}
