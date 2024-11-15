package com;

import proto.Paxoskv;

public class Phase1Response {

    Paxoskv.Value value;
    Paxoskv.BallotNum ballotNum;

    public Phase1Response() {
    }

    public Phase1Response(Paxoskv.Value value) {
        this.value = value;
    }

    public Phase1Response(Paxoskv.BallotNum ballotNum) {
        this.ballotNum = ballotNum;
    }

    public Phase1Response(Paxoskv.Value value, Paxoskv.BallotNum ballotNum) {
        this.value = value;
        this.ballotNum = ballotNum;
    }

    public Paxoskv.Value getValue() {
        return value;
    }

    public void setValue(Paxoskv.Value value) {
        this.value = value;
    }

    public Paxoskv.BallotNum getBallotNum() {
        return ballotNum;
    }

    public void setBallotNum(Paxoskv.BallotNum ballotNum) {
        this.ballotNum = ballotNum;
    }
}
