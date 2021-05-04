# Raft: Understandable Distributed Consensus

André Aragão, André Matos, Margarida Marques

- [Objective](#objective)
- [Raft Overview](#raft)
- [Executables](#exe)
- [Code Overview](#code)
- [GitHub Structure](#stru)

## Objective <a name="objective"></a>
The objective of this work is to implement a consensus algorithm called Raft. This algorithm is implemented entirely in java. Furthermore, a final goal is to compare the suggested raft implementation with other consensus algorithms such as the Paxos algorithm. 

The following work was based on the article written by Diego Ongaro and John Ousterhout which is accessible in [Raft](https://github.com/araragao/raft/blob/main/01-paper/raft-paper.pdf). In addition to this, the results and comments to the understanding of the algorithm are described in the report  [raft-understandable-distributed-consensus.pdf](https://github.com/araragao/raft/blob/main/01-paper/raft-understandable-distributed-consensus.pdf).

In the context of this implementation, a [github website](https://araragao.github.io/raft/) was developed. This aims to support the written paper by revewing topics such as raf overview, implementation, results and performance anaysis, and so on.

## Raft Overview <a name="raft"></a>

Raft implements consensus by a leader approach. Moreover, one of the n servers is designated the leader. All operations for the state machine are sent to the leader. The leader appends the operation to their log and asks the other servers to do the same. Once the leader has received acknowledgements from a majority of servers that this has taken place, it applies the operation to its state machine. This process repeats until the leader fails. When the leader fails, another server takes over as leader. This process of electing a new leader involves at least a majority of servers, ensuring that the new leader will not overwrite any previously applied operations.

## Executables <a name="exe"></a>
The user can run the executable file for 3, 5 or 7 servers, as well as the fast or slow execution. Furthermore, in the fast executation the client sends a new message when receives an answer from the leader. These files can be found in the [exec](https://github.com/araragao/raft/tree/main/03-executable-files). In the other hand, in the slow execution the client waits 2 seconds since receives an answer from the leader to send a new request.

When running, it is possible to see the GUIs for all the servers, as well as the client. Each server presents its own ID, election timeout, current role, current term and committed value. There is also the option of opening the log with all the entries since the beginning of the execution. The GUI of the client shows both the local value and the received one, as well as the command list. 

## Code Overview <a name="code"></a>
There are some variables that can be changed by changing the values passed to the functions in the main file. These are the communication ports for the servers and the port for multicast communication. Other values that can be changed are the maximum and minimum values for the operands in the commands sent by the client. 
Finally, it is also possible to change the timeout for the client to wait for the response from the system.
With regards the code files, these can be found in [src](https://github.com/araragao/raft/tree/main/03-executable-files).

## GitHub Structure <a name="stru"></a>

This GitHub repository is divided in 4 different folders and it gives support to a developed website.

* [01-paper](https://github.com/araragao/raft/tree/main/01-paper) - contains the original report which this work is based on. A personal algorithm overview and performance analysis report can be found in this folder.
* [02-source-code](https://github.com/araragao/raft/tree/main/02-source-code) - contains the source code of this project as well as every developed class.
* [03-executable-file](https://github.com/araragao/raft/tree/main/03-executable-files) - contains a set of executable files of the source code with different settings. These settings differ in number of servers, client response, and percentage of message lost.
* [04-downloads](https://github.com/araragao/raft/tree/main/04-downloads) - this folder has zip files to the source code and to the executable files.
