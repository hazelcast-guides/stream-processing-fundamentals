package hazelcast.platform.labs.machineshop;

import com.hazelcast.map.IMap;
import hazelcast.platform.labs.machineshop.domain.MachineStatusEvent;

public class MachineEmulator implements  Runnable{
    private final String serialNum;

    private  SignalGenerator tempSignalGenerator;
    private String targetURL;

    private final IMap<String, MachineStatusEvent> machineStatusMap;

    private int t;
    private SignalGenerator signalGenerator;

    private MachineStatusEvent currStatus;

    public MachineEmulator(IMap<String,MachineStatusEvent> machineStatusMap, String sn, SignalGenerator signalGenerator){
        this.machineStatusMap = machineStatusMap;
        this.signalGenerator = signalGenerator;
        this.serialNum = sn;
        this.t = 0;
    }

    public  synchronized void setSignalGenerator(SignalGenerator signalGenerator){
        this.signalGenerator = signalGenerator;
        this.t = 0;
    }
    @Override
    public synchronized void  run() {
        currStatus = new MachineStatusEvent();
        currStatus.setSerialNum(serialNum);
        currStatus.setEventTime(System.currentTimeMillis());
        currStatus.setBitTemp(signalGenerator.compute(t++));
        currStatus.setBitRPM(10000);
        currStatus.setBitPositionX(0);
        currStatus.setBitPositionY(0);
        currStatus.setBitPositionZ(0);

        machineStatusMap.putAsync(serialNum, currStatus).whenComplete((v,x) -> {
            if (x != null) System.out.println("WARNING: put failed");
        });
    }

    public MachineStatusEvent getCurrStatus(){
        return currStatus;
    }

    public String getSerialNum() { return serialNum;}
}
