package mtmc.emulator;

import java.util.ArrayList;
import java.util.List;

public class RewindStep {

    List<Runnable> subSteps = new ArrayList<>();

    public void rewind() {
        subSteps.reversed().forEach(Runnable::run);
    }

    public void addSubStep(Runnable subStep) {
        subSteps.add(subStep);
    }
}
