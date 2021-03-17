package com.tfg.healthwatch;

import java.util.Observable;
import java.util.Observer;

public class HeartRateObserver implements Observer {

    public void update(Observable obj, Object arg) {
        System.out.println("FirstNewsReader got The news:"+(String)arg);
    }
}
