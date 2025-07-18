package mtmc.emulator;

import static mtmc.emulator.MonTanaMiniComputer.ComputerStatus.*;

/**
 *
 * @author jbanes
 */
public class MTMCClock
{
    private MonTanaMiniComputer computer;

    public MTMCClock(MonTanaMiniComputer computer) {
        this.computer = computer;
    }

    public void run() {
        
        long instructions = 0;
        long ips = 0;
        long expected = 0;
        long virtual = 0;
        
        long startTime = System.currentTimeMillis();
        long deltaStart;
        long delta;
        
        long speed = 0;
        long pulse;
        long ms = 10;
        
        while(computer.getStatus() == EXECUTING) {
            speed = Math.max(computer.getSpeed(), 0);
            pulse = (speed <= 0 ? 1000000 : Math.max(speed / 100, 1));
            ms = (pulse < 10 ? 1000 / speed : 10);
            
            deltaStart = System.currentTimeMillis();
            delta = ms - (System.currentTimeMillis() - deltaStart);
            
            /* We've lost more than a second. Recalibrate. */
            if ((expected - virtual) > pulse * 100) {
                startTime = deltaStart;
                virtual = 0;
            }
            
            /* Throttles to every 10ms, but "catches up" if we're behind */
            if(delta > 0 && (expected - virtual) < pulse && speed != 0) {
                try { Thread.sleep(delta); } catch(InterruptedException e) {}
            }
            
            instructions += computer.pulse(pulse);
            
            virtual += pulse;
            ips = (virtual * 1000) / Math.max(1, System.currentTimeMillis() - startTime);
            expected = (System.currentTimeMillis() - startTime) * speed / 1000;
        }
        
        System.err.println("Executed " + instructions + " instructions at a rate of " + ips + " ips (speed = " + speed + ")");
    }
    
    public void step() {
        computer.fetchAndExecute();
        computer.fetchCurrentInstruction();
        computer.notifyOfStepExecution();
    }

    public void back() {
        computer.rewind();
        computer.fetchCurrentInstruction();
        computer.notifyOfStepExecution();
    }
}
