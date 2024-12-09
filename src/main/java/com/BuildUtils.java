package com;

import proto.Paxoskv;

public class BuildUtils {
    public static Paxoskv.Proposer buildProposer(String key, long ver, long n, long proposerId){
        Paxoskv.PaxosInstanceId id = Paxoskv.PaxosInstanceId.newBuilder().setKey(key).setVer(ver).build();
        Paxoskv.BallotNum bal = Paxoskv.BallotNum.newBuilder().setN(n).setProposerId(proposerId).build();
        return Paxoskv.Proposer.newBuilder().setId(id).setBal(bal).build();
    }

    public static Paxoskv.Value buildValue(long vi64){
        return Paxoskv.Value.newBuilder().setVi64(vi64).build();
    }
}
