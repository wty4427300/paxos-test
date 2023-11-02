package com;


import java.util.concurrent.locks.Lock;

public class Version {
    private Lock lock;
    private Paxoskv.Acceptor acceptor;
}
