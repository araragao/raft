## About The Project 

This project aims to:

* Explain what **Raft** is about
* Describe its implementation using Java
* Provide real data regarding the usage of 3, 5 and 7 servers
* Discuss its performance

The crash of server nodes is not considered, only the messages' delay.

Furthermore, the implementation's source code is provided.

#### The team

<div align="center">
  <div style="display: flex; align-items: flex-start;">
    <div class="team-member">
      <div style="margin: 10px">
        <div class="photo">
          <a href="https://www.linkedin.com/in/andreribeiroaragao/">
            <img align="left" src="pictures/aa-avatar.png" alt="aa-avatar" width="100" height="100">
          </a>
        </div>
        <div class="name">
          André
        </div>
      </div>
    </div>
    <div class="team-member">
      <div style="margin: 10px">
        <div class="photo">
          <a href="https://www.linkedin.com/in/andre-matos98/">
            <img align="left" src="pictures/am-avatar.png" alt="am-avatar" width="100" height="100">
          </a>
        </div>
        <div class="name">
          André
        </div>
      </div>
    </div>
    <div class="team-member">
      <div style="margin: 10px">
        <div class="photo">
          <a href="https://www.linkedin.com/in/margarida-marques-b65618162/">
            <img align="left" src="pictures/mm-avatar.png" alt="mm-avatar" width="100" height="100">
          </a>
        </div>
        <div class="name">
          Margarida
        </div>
      </div>
    </div>
  </div> 
</div>

## Table of Contents

<details open="open">
  <summary>Table of Contents</summary>
  <ol>
    <li><a href="#raft-overview">Raft Overview</a></li>
      <ul>
        <li><a href="#leader-election">Leader Election</a></li>
        <li><a href="#log-replication">Log Replication</a></li>
        <li><a href="#state-machine">State Machine</a></li>
      </ul>
    <li><a href="#implementation">Implementation</a></li>
      <ul>
        <li><a href="#client">Client</a></li>
        <li><a href="#servers-state-machine">Servers' State Machine</a></li>
            <ul>
              <li><a href="#leader">Leader</a></li>
              <li><a href="#candidate">Candidate</a></li>
              <li><a href="#follower">Follower</a></li>
            </ul>
        <li><a href="#interface">Interface</a></li>
      </ul>
    <li><a href="#results-and-performance-analysis">Results and Performance Analysis</a></li>
      <ul>
        <li><a href="#latency">Latency</a></li>
        <li><a href="#election-time">Election Time</a></li>
        <li><a href="#convergence-time">Convergence Time</a></li>
      </ul>
    <li><a href="#general-review">General Review</a></li>
    <li><a href="#further-work">Further work</a></li>
  </ol>
</details>

## Raft Overview

In Raft, each server can be, at a given time, in one of three states:

* Leader
* Candidate
* Follower

Raft works by having one distinguished leader.
This leader is responsible for:

* Receiving the commands from the client
* Managing the log replication

<p align="center">
  <img src="pictures/graphics/terms-evolution.png"  width="50%" alt="terms-evolution">
</p>

An important aspect is the concept of **term**.
A term is an interval of time that starts with an election.
If a leader is elected, then that server will be leader for that term.
If no server wins the election, i.e. no leader is elected, then that term will finish with the starting of a new election.

Each server saves its own current term, *CurrentTerm*, increasing its value monotonically and communicating it in each message it sends.
If a server realizes that there's a server with a current term greater than his, it updates its own to the greatest current term found.
By doing that, the server, whether a leader or a candidate, immediately becomes a follower.

### Leader Election

Raft is based on two **remote procedure calls**, refered from now on as *RPCs*:
* *AppendEntries RPC* which is initiated by the leader periodically.
It replicates log entries, if there are any to be replicated, and provides a form of heartbeat.
* *RequestVote RPC* which is iniated by a follower to start an election

If a follower does not receive any heartbeat, after a certain time, known as *election timeout*, it starts an election.
To do so, it increments its own term and becomes a candidate.
It votes for itself and request a vote from the other servers.
Each server can only vote for one candidate, at most, in a *term*.
There are three outputs from an election:

1. The candidate receives votes for the majority of servers during the *term*, wins the election and becomes the leader.
In the first heartbeat it send, it will let other servers know its role.
2. While waiting for answers, the candidate receives an *AppendEntries RPC* from another server. If the other server's current *term* is greater, then the candidate returns to follower. Otherwise, the candidate rejects the RPC.
3. There is no majority in regards to the votes resulting in no candidate winning the election.Candidates will time out and start a new election with a bigger term.

### Log Replication

As stated, the leader receives the commands from the client.
When it receives a command, it adds a new *entry* with that command into its log.
In the next *AppendEntries RPCs*, it replicates it.

Each *entry* contains the respective command, the term in which it was received, and an index that identifies its position in the log.
These are used to make a consistency check: when sending an *AppendEntries RPC*, the leader sends the index and the term of the entry in its log that precedes the new entries that it is sending. 
The follower will try to find on its log the respective entry, and if it does, it means the log of the follower is updated.
Otherwise, it refuses the new entries.
When the logs are inconsistent, the leader forces the followers’ logs to duplicate its own, as will be explained further.

<p align="center">
  <img src="pictures/graphics/log-replication.png" width="50%" alt="log-replication">
</p>

### State Machine

The leader decides when is it safe to apply a specific command to the state machines, i.e. to commit a specific entry.
It commits a certain *entry* after replicating it on the majority of the servers.
When doing this, it also commits all preceding entries in its log.
The *AppendEntries RPC* also contains the *commitIndex*, which corresponds to the last commited index.
The servers will acknowledge this, and apply the command to their own state machines.

## Implementation

The implementation was based on the suggestions from the Raft paper itself.
Variables are named according to the paper for a matter of consistency.
For further clarification, a class diagram is provided.

<p align="center">
  <img src="pictures/graphics/class-diagram-small.png" alt="class-diagram-small">
</p>

Each server makes use of a *MulticastCommunicationPackage* and a *UnicastCommunicationPackage* that implement communication services .
As a consequence, all servers support multicast and unicast communication.

All communications occur over UDP and the network was implemented using threads.
Even though all server nodes are running on the same machine, all communication happens through messages, with no use of shared memory.
Due to the lack of reliability of UDP, the application might have to deal with loss of packets, delays and different orders of arrival.

In the beginning, all nodes, including the client, exchange their IDs, which correspond to their ports.
The multicast port is known *a priori*, which is passed as an argument when a server is created.

The multicast communication is only used for the communication between the client and the leader.
It is important to emphasize that the reason why multicast needs to be supported by all server nodes is not only a result of the dynamic server role enforced by Raft but also of our own implementation.
The client does not know which server is the leader, so it sends a multicast message to the multicast group.
All servers, except for the leader, must ignore that message.
The response from the leader to the client is a unicast message.

All the other communications are based on unicast.
The *AppendEntries RPC* and the *RequestVote RPC* are sent in parallel to all other servers by sending separate messages to each server individually, thus using unicast and not multicast communication.
The function in use is called broadcastMessage(), but the name is just suggestive, as it is based on unicast.
One of the reasons to this implementation is the simulation of messaging lost in the network.

As mentioned before, Raft has to be fault tolerant.
This means that the state machines should converge in the presence of crash of nodes, delays of messages or the different arrival order of messages.
The crash of nodes was not a concern for this project.
The delay and consequent different order of arrival of messages was achieved through the use of a random function that outputs a percentual value.
Depending on its value, a message is sent, or not.

There is also one class for the Message, associated to each server.
It allows to build messages of different types.
These types are enumerated and agreed between all servers.
Every server sends the messages along with their respective type identifier.

* 0: Message from Client
* 1: *AppendEntries RPC* from Leader
* 2: Acknowledge for *AppendEntries RPC* with new Entries
* 3: Acknowledge for *AppendEntries RPC* without any Entry (heartbeat)
* 4: *RequestVote RPC*
* 5: Acknowledge for *RequestVote RPC*

The class Entry is implemented exactly as explained before.

### Client

The commands from the client have the following form: 
* add/sub + integer

The commands are generated in a random way.
The client runs in an infinite loop. 
It send a new command either after receiving the response to the previous one or after 14 seconds.

### Servers' State Machine

Each server runs in an infinite loop that has a switch condition according to the state of the server.
This takes place on the class ServerThread, which implements the following state machine.

<p align="center">
  <img src="pictures/graphics/server-state-machine.png"  width="50%" alt="server-state-machine">
</p>

#### Leader

As mentioned before, the leader checks if any command is received from the client and if so, the leader adds it as a new entry to its log.
The socket has a timeout of 1 ms, as it is not supposed to be halted expecting a command.
If 50ms have already passed, which is the heartbeat period, then it must send another *AppendEntries RPC*.
This period was implemented using the System.nanoTime() for precision purposes.
This includes the entries from the oldest to the newest, which the leader knows to have not been received by at least one follower.
If there is none, then it will be just a heartbeat.
Afterwards, it waits for unicast messages, which will be one of the many types stated in the class Message. Again, it uses a timeout of 1 ms just so it is not halted whilst waiting for commands.

* Acknowledge to *AppendEntries RPC*
* *RequestVote RPC* from a candidate
* *AppendEntries RPC* from another Leader

The leader also checks every instance of the loop if it has some entry to commit, according to the responses received to the *AppendEntries RPCs*, as explained in the last section.
This is done by comparing its own *commitIndex* to the *matchIndex* of the followers.
The *matchIndex* of each follower is saved in the leader, and it corresponds to the last entry that the leader knows has been commited to that server.
If there’s a majority of *matchIndex* to an entry not yet commited by the leader itself, then it will commit it.
It will send, using unicast, the response to the client.
The leader will updade its *commitIndex* so it is sent in the *AppendEntries RPCs*, and other followers find out.

#### Candidate

The period for sending the *RequestVote RPC* is implemented the same way as the *AppendEntries RPC* in the leader.
The timeout for waiting for an answer is defined in a random way when creating the server.
This has a value between 150ms and 300ms, as suggested in the paper, and it is fixed for each server during the whole execution.
In the same way as the leader, the candidate waits for unicast messages, that will be one of the many types of state in the class Message:

* Acknowledge to *RequestVote RPC*
* *RequestVote RPC* from another candidate
* *AppendEntries RPC* from another Leader

#### Follower

The follower has a passive role.
It simply reads the unicast messages it receives, which are the following:

* *AppendEntries RPC* from a Leader
* *RequestVote RPC* from a candidate

If it does not receive an *AppendEntries RPC* after a certain predetermined timeout, it turns into Candidate.
As alluded to above, when the follower receives an *AppendEntries RPC* it must perform a consistency check in order to see if its log is updated or not.
The logs might be inconsistent.
There might be missing entries, extra entries or both.

To implement the log replication algorithm presented before, the leader must maintain a *nextIndex* for each follower, which corresponds to the next entry to be sent to that follower.
These are initialised when the server becomes leader to the index after the last entry in its log. This means that the leader assumes that the followers are updated.
If this turns out to be false, then the *AppendEntries RPC* consistency check will fail.
The leader will decrement the *nextIndex* for that follower, and retry the *AppendEntries RPC*. Eventually the answer will be true, meaning, they reached a point in which they are consistent.

Note that if a follower receives an *AppendEntries RPC* request that includes log entries already present in its log, it ignores those entries in the new request by answering true.
This last *AppendEntries RPC* will eliminate any subsequent entries from the follower’s log and append the new ones.

### Interface

The Graphical User Interface, GUI, was designed having in mind simplicity and ease of use since it was only developed with the objective of making it easier to evaluate the behaviour of the implemented algorithm in an empiric way.
Consequently, for each server the corresponding GUI shows the:

* Server id
* Election timeout
* Current role
* Current term
* Commited value

Besides that, each server’s GUI also has a button to check its log.

The client’s GUI only displays the:
* Client id
* Local value
* Received value (from the leader)

The received value is displayed to easily check the consistency between the server’s and the client value.
Having the same goal in mind, it also has a button to check the command list.

## Results and Performance Analysis

The values were taken from various amounts of servers (3, 5 and 7) and for 6 different values of percentages of error, where error means percentage of lost messages.

### Latency

The latency measures the time interval between the leader receiving a command from the client and the instant in which the client sends the response to that command.

When evaluating the results, it is clear that the latency increases with the percentage of error and number of servers.

When we focus on the simulation that has 7 servers and 50% of error, it is noticeable that after every glitch there is a linear decrease.
This linear decrease is due to the leader sending the response to other previous requests that had not been committed yet, whilst sending the response to the client.
Multiple requests can be pending since the client sends a new command after receiving the previous answer or after 15 seconds without receiving anything.

### Election Time

The election time measures the time interval between a certain follower becoming candidate and the time instant in which it stops being a candidate.
This is done either by turning into a leader or into a follower again, as they both mean another leader is recognised.

As expected, the election time grows with both the number of servers and the percentage of error.
The main factor for this behaviour is that more followers will turn into candidates with the increase of the error which results in many parallel RequestVote RPCs sent in parallel.
Adding the loss of messages that create more candidates and the loss of messages that keep them from getting the majority and winning an election, the time increases considerably.

### Convergence Time

The convergence time is the time needed for the state machines of the servers to be consistent.
It starts when the current leader commits a certain entry (with a specific index), and it ends when the last server commits that same entry.

The delay to consensus does not change much when comparing a different number of servers.
Thus, the graphic available is only for the case where there are 3 servers.
However, for different amounts of error, the results are distinct.

An observation made is that the number of glitches increases with the number of servers.
This happens when the leader receives a majority of positive acknowledges (for instance, 4 in the case of 7 servers) and it commits that entry together with those followers.
However, the other 2 followers will need log replication, which will lead to a clear delay on reaching the consensus.

The network traffic measures the number of messages exchanged during nodes since the moment in which the leader receives the command from the client until all servers have applied that command to their state machine.

It is relevant to note that, for low error percentages, the whole graphic shifts to a higher number of messages when the servers increase.
In the example with 3 servers and no error, the leader has to receive two acknowledge messages and each server receives one *AppendEntries RPC*.
This adds up to a total of 4 exchanged messages, as we can see in the graphic.
By applying the same reasoning to the others, we reach the same conclusion.
When the error increases so do the exchange messages. This happens due to many reasons:

* Every time it does not get a majority of acknowledges, it has to send again the messages to every server, even though many have already received it.
* The loss of messages makes it harder to win an election, and consequently more elections will occur.
* More followers will not have received the AppendEntries and they will turn into candidates. This creates conflicts between VoteRequest RPCs which increases the network traffic.

## General Review

The tests performed were only in the context of message loss since server crashes were not taken into consideration.

It can be acknowledged that the safety of Raft does not depend on timing: sooner or later all server nodes will converge to the same state.
This happened even when testing with 70% of message loss, which shows the robustness of the algorithm.

The results made it obvious, however, that the availability is strongly related to the message loss.
Availability is described ”the ability of a system to respond to clients in a timely manner”, as stated in Raft paper, which was measured through the latency testing.
It can be concluded that the higher the message loss the lower the availability of the system.

Nerverthless, the latency is not its biggest problem.
The normal course of events contemplates the rejection of messages from the client when the server node is a candidate or a follower.
A problem arises when a server node changes from leader to follower: when this happens, at the same time, a candidate is changing to leader, and a message loss might happen due change ocurring before the client's command is read.

Understandability is one of the strongest points offered by Raft, however, it is hard to make any strong conclusions when comparing Raft with Paxos.
This is due to the fact that Paxos was not studied as thorough as Raft.

When comparing Raft to Paxos, it is worth noting that the implementation of Raft is more straight forward according to existing literature.
The reduced number of message types and the simplicity of the state machine on which it relies are the most relevant characteristics responsible for that.

It can be understood that altough simpler, Raft still ensure safety by limiting the ways in which the logs might be inconsistent.

## Further work

Crash tests should be taken into consideration when evaluating Raft performance.
Besides, using different languages, and programming methods, should provide a stronger understanding of Raft performance.
