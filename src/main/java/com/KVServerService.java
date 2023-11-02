package com;


public interface KVServerService {
    Paxoskv.Acceptor Prepare(Paxoskv.Proposer proposer);
}
