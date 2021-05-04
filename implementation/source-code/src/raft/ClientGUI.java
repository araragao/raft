package raft;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;

public class ClientGUI {
  
  JLabel clientIdLabel;
  JLabel localValueLabel;
  JLabel receivedValueLabel;
  static JTable logTable;
  static String [][]logTableData = new String[10000][10000];
  
  
  ClientGUI(int receivedClientId, int receivedLocalValue, int receivedReceivedValue, List<String> receivedCommand) {
	 ClientGUI.logTableData[0][0] = "FIRST ENTRY SO IT'S NOT NULL";
	 ClientGUI.logTableData[0][1] = "FIRST ENTRY SO IT'S NOT NULL";
    JFrame frame = new JFrame("ClientGUI - " + receivedClientId);
        frame.setSize(300, 160);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();    
        frame.add(panel);
        placeComponents(panel, receivedClientId, receivedLocalValue, receivedReceivedValue, receivedCommand);
        frame.setVisible(true);
    }

    public void placeComponents(JPanel panel, int receivedClientId, int receivedLocalValue, int receivedReceivedValue, List<String> receivedCommand) {
        panel.setLayout(null);
         
        clientIdLabel = new JLabel("Client Id: " + receivedClientId);
        clientIdLabel.setBounds(10,20,150,20);
        panel.add(clientIdLabel);
        
        localValueLabel = new JLabel("Local Value: " + receivedLocalValue);
        localValueLabel.setBounds(10,40,150,20);
        panel.add(localValueLabel);
          
        receivedValueLabel = new JLabel("Received Value: " + receivedReceivedValue);
        receivedValueLabel.setBounds(10,60,150,20);
        panel.add(receivedValueLabel);

        JButton openLogButton = new JButton("Open Command List");
        openLogButton.setBounds(10, 80, 200, 20);
        panel.add(openLogButton);
        
        openLogButton.addActionListener( new ActionListener()
        {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        createFrame(receivedClientId);
      }
        });
    }
	void updateLabels(int receivedClientId, int receivedLocalValue, int receivedReceivedValue) {
      this.clientIdLabel.setText("Server Id: " + receivedClientId);
      this.localValueLabel.setText("Local Value: " + receivedLocalValue);
      this.receivedValueLabel.setText("Received Value: " + receivedReceivedValue);
    }
    
    public void updateLogTable(List<String> receivedCommand) {
      Iterator<String> iter = receivedCommand.iterator();
      int indexNumber = 0;
      while (iter.hasNext()) {
        String auxString = iter.next();
        String[] auxStringSplitted = auxString.split(" ");
          logTableData[indexNumber][0] = String.valueOf(indexNumber+1);
          logTableData[indexNumber][1] = auxStringSplitted[2] + " " + auxStringSplitted[3];
          indexNumber++;
      }
    }
    
    public static void createFrame(int receivedClientId) {
        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                JFrame logFrame = new JFrame("Log - " + receivedClientId);
                logFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                String[] columnNames = {"Order", "Command"}; 
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
