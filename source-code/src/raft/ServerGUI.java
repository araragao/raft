package raft;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;

public class ServerGUI {
  
  JLabel serverIdLabel;
  JLabel electionTimeoutLabel;
  JLabel roleLabel;
  JLabel termLabel;
  JLabel commitedValueLabel;
  static JTable logTable;
  static String [][]logTableData = new String[10000][10000];
  
  
  ServerGUI(int receivedServerId, int receivedElectionTimeOut, int receivedCurrentRole, int receivedCurrentTerm, int receivedStateMachineValue, List<Entry> receivedLog) {
    ServerGUI.logTableData[0][0] = "FIRST ENTRY SO IT'S NOT NULL";
        ServerGUI.logTableData[0][1] = "FIRST ENTRY SO IT'S NOT NULL";
    JFrame frame = new JFrame("ServerGUI - " + receivedServerId);
        frame.setSize(300, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();    
        frame.add(panel);
        placeComponents(panel,receivedServerId,receivedElectionTimeOut,receivedCurrentRole,receivedCurrentTerm,receivedStateMachineValue);
        frame.setVisible(true);
    }

    public void placeComponents(JPanel panel, int receivedServerId, int receivedElectionTimeOut, int receivedCurrentRole, int receivedCurrentTerm, int receivedStateMachineValue) {
        panel.setLayout(null);
         
        serverIdLabel = new JLabel("Server Id: " + receivedServerId);
        serverIdLabel.setBounds(10,20,150,20);
        panel.add(serverIdLabel);
        
        electionTimeoutLabel = new JLabel("Election Timeout: " + receivedElectionTimeOut);
        electionTimeoutLabel.setBounds(10,40,150,20);
        panel.add(electionTimeoutLabel);
        
        roleLabel = new JLabel();
        switch(receivedCurrentRole) {
          case 1:
          roleLabel.setText("Current Role: Leader");
          break;
        case 2:
          roleLabel.setText("Current Role: Candidate");
        break;
        case 3:
          roleLabel.setText("Current Role: Follower");
        break;
        }
        roleLabel.setBounds(10,60,150,20);
        panel.add(roleLabel);
          
        termLabel = new JLabel("Current Term: " + receivedCurrentTerm);
        termLabel.setBounds(10,80,150,20);
        panel.add(termLabel);
        
        commitedValueLabel = new JLabel("Commited Value: " + receivedStateMachineValue);
        commitedValueLabel.setBounds(10,100,150,20);
        panel.add(commitedValueLabel);

        JButton openLogButton = new JButton("Open Log");
        openLogButton.setBounds(10, 120, 150, 20);
        panel.add(openLogButton);
        
        openLogButton.addActionListener( new ActionListener()
        {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        createFrame(receivedServerId);
      }
        });
    }
	void updateLabels(int receivedServerId, int receivedElectionTimeOut, int receivedCurrentRole, int receivedCurrentTerm, int receivedStateMachineValue) {
      this.serverIdLabel.setText("Server Id: " + receivedServerId);
      this.electionTimeoutLabel.setText("Election Timeout: " + receivedElectionTimeOut);
        switch(receivedCurrentRole) {
          case 1:
          this.roleLabel.setText("Current Role: Leader");
          break;
        case 2:
          this.roleLabel.setText("Current Role: Candidate");
        break;
        case 3:
          this.roleLabel.setText("Current Role: Follower");
        break;
        }
        this.termLabel.setText("Current Term: " + receivedCurrentTerm);
        this.commitedValueLabel.setText("Commited Value: " + receivedStateMachineValue);
    }
    
    public void updateLogTable(List<Entry> receivedLog) {
      Iterator<Entry> iter = receivedLog.iterator();
      int indexNumber = 0;
      while (iter.hasNext()) {
        Entry auxEntry = iter.next();
        if(auxEntry.getTerm() == -1) {
          continue;
        }
          logTableData[indexNumber][0] = String.valueOf(auxEntry.getTerm());
          logTableData[indexNumber][1] = auxEntry.getCommand();
          indexNumber++;
      }
    }
    
    public static void createFrame(int receivedServerId) {
        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                JFrame logFrame = new JFrame("Log - " + receivedServerId);
                logFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                String[] columnNames = {"Term", "Command"}; 
                logTable = new JTable(logTableData, columnNames); 
                logTable.setBounds(30, 40, 200, 300); 
                JScrollPane sp = new JScrollPane(logTable); 
                logFrame.add(sp); 
                logFrame.setSize(500, 200); 
                logFrame.setVisible(true);
            }
        });
    }
}