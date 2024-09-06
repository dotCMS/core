package com.dotcms.observability.service;

import com.dotcms.observability.state.State;
import com.dotcms.observability.state.StateChangeEvent;
import com.dotcms.observability.subsystem.Subsystem;
import java.util.List;



public interface SystemStateService {

    State getState(String subsystemName);

    Subsystem getSubsystem(String subsystemName);

    List<Subsystem> getSubsystems();

    void submitStateChange(StateChangeEvent event);
}
