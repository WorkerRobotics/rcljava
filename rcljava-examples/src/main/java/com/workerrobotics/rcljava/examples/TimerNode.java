package com.workerrobotics.rcljava.examples;

import com.workerrobotics.rcljava.core.Node;

public class TimerNode extends Node {

    public TimerNode() {
        super("timer_node_example");
        
        this.createTimer(1000, (timer)-> {
            System.out.println("time: "+getClock().now());
        });
    }
}
