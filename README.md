# Raft: Understandable Distributed Consensus

André Aragão, André Matos, Margarida Marques

- [Objective](#objective)
- [Raft Overview](#raft)
- [Executables](#exe)
- [Code Overview](#code)

## Objective <a name="objective"></a>

## Raft Overview <a name="raft"></a>

## Executables <a name="exe"></a>
The user can run the executable file for 3, 5 or 7 servers, as well as the fast or slow execution. Furthermore, in the fast executation the client sends a new message when receives an answer from the leader. In the other hand, in the slow execution the client waits 2 seconds since receives an answer from the leader to send a new request.

When running, it is possible to see the GUIs for all the servers, as well as the client. Each server presents its own ID, election timeout, current role, current term and committed value. There is also the option of opening the log with all the entries since the beginning of the execution. The GUI of the client shows both the local value and the received one, as well as the command list. 

## Code Overview <a name="code"></a>
There are some variables that can be changed by changing the values passed to the functions in the main file. These are the communication ports for the servers and the port for multicast communication. Other values that can be changed are the maximum and minimum values for the operands in the commands sent by the client. 
Finally, it is also possible to change the timeout for the client to wait for the response from the system.
