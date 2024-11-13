package com;


import java.util.concurrent.locks.Lock;
import proto.Paxoskv;

public class Version {
    private Lock mu;
    private Paxoskv.Acceptor acceptor;

    public Version() {
        this.mu = new Mutex();
    }

    public Lock getMu() {
        return mu;
    }

    public void setMu(Lock mu) {
        this.mu = mu;
    }

    public Paxoskv.Acceptor getAcceptor() {
        return acceptor;
    }

    public void setAcceptor(Paxoskv.Acceptor acceptor) {
        this.acceptor = acceptor;
    }
}
