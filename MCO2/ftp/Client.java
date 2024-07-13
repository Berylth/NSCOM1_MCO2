package ftp;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Client {

    /** Scanner object used to get user inputs. */
    private Scanner Scan;

    /* Conrol Socket and its Data input and output streams. */
    /** Socket used for ftp commands and responds . */
    private Socket ControlSocket;

    /** Data input stream used for receiving data in the control socket. */
    private DataInputStream Cdis;

    /** Data output stream used for sending data in the control socket. */
    private DataOutputStream Cdos;

    /* Data Socket and its Data input and output streams. */
    /** Socket used for data transfer */
    private Socket DataSocket;

    /** Data input stream used for receiving data in the data socket. */
    private DataInputStream Ddis;

    /** Data output stream used for sending data in the data socket. */
    private DataOutputStream Ddos;

    /**
     * Constructor for instantiating the Client class.
     * 
     * @param ServAddr IP address of the server.
     * @param ServPort Port number of the server.
     * @throws UnknownHostException
     * @throws IOException
     */
    private Client(String ServAddr, int ServPort) throws UnknownHostException, IOException {

        ConnectToServer(ServAddr, ServPort);

        SendCommands();
    }

    /**
     * Main function.
     * 
     * @param args Contains the address and port number of the server.
     * @throws NumberFormatException
     * @throws UnknownHostException
     * @throws IOException
     */
    public static void main(String[] args) throws NumberFormatException, UnknownHostException, IOException {
        new Client(args[0], Integer.parseInt(args[1]));
    }

    /**
     * This function connects the client to the server using the arguements server
     * address and server port number.
     * 
     * @param ServAddr Address of the server.
     * @param ServPort Port number of the server.
     * @throws UnknownHostException
     * @throws IOException
     */
    private void ConnectToServer(String ServAddr, int ServPort) throws UnknownHostException, IOException {
        System.out.println("Intializing Client. . . .\n");
        System.out.println("Connecting to Server . . .\n");

        // Create Control Socket that connects to server.
        ControlSocket = new Socket(ServAddr, ServPort);

        System.out.println("Connected to Server at " + ControlSocket.getRemoteSocketAddress() + "\n");

        // Create DataOutputStream and DataInputStream which will be used to communicate
        // to client.
        Cdis = new DataInputStream(ControlSocket.getInputStream());
        Cdos = new DataOutputStream(ControlSocket.getOutputStream());

        // Create Scanner object.
        Scan = new Scanner(System.in);

        // Receive welcome message from server.
        System.out.println(ReceiveMsg(Cdis));
    }

    /**
     * This function sends commands written by the client to the server.
     * 
     * @throws IOException
     */
    private void SendCommands() throws IOException {
        // Boolean value to monitor whether client is still active.
        boolean IsActive = true;

        // Loop while client is active.
        do {
            System.out.print("Command: ");
            String Command = Scan.nextLine();

            // Send command to server.
            SendMsg(Cdos, Command);

            // Wait for server reply.
            String Reply = ReceiveMsg(Cdis);
            System.out.println("\n" + Reply);
            String ReplyCode = Reply.substring(0, 3);

            // Server reply code 1xx usually indicates that commands LIST, RETR, and STOR is
            // being proccessed by the server.
            if (ReplyCode.charAt(0) == '1') {
                String Args = Command.substring(4);
                Command = Command.substring(0, 4);
                switch (Command) {
                    case "LIST":
                        HandleList();
                        break;
                    case "RETR":
                        HandleRetr(Args);
                        break;
                    case "STOR":
                        HandleStor(Args);
                        break;
                }

                System.out.println("\n" + ReceiveMsg(Cdis));
            }

            // 221 reply from the server indicates of that a successful logout.
            if (ReplyCode.equals("221")) {
                System.out.println("Exiting . . .");
                CloseSocketConnections();
                IsActive = false;
            }

            // 227 reply from the server indicates that the server has accepted PASV.
            if (ReplyCode.equals("227")) {
                // Create Data Socket
                CreateDataSocket(Reply);
            }

            // 230 reply from the server means client is logged in.
            if (ReplyCode.equals("230")) {
                System.out.println("Sending default configurations for stru, type, and mode. . .\n");
                // Send default configurations for stru, type, and mode as defined in mp specs.
                SendDefaultConfig();
            }

        } while (IsActive);
    }

    /**
     * This function sends default configurations for stru, type, and mode as
     * defined in mp specs.
     * 
     * @throws IOException
     */
    private void SendDefaultConfig() throws IOException {
        // Set type to BINARY.
        SendMsg(Cdos, "TYPE I\r\n");
        System.out.println(ReceiveMsg(Cdis));

        // Set mode to STREAM.
        SendMsg(Cdos, "MODE S\r\n");
        System.out.println(ReceiveMsg(Cdis));

        // Set mode to FILE.
        SendMsg(Cdos, "STRU F\r\n");
        System.out.println(ReceiveMsg(Cdis));
    }

    /**
     * This function create a data socket once the server has accepted PASV.
     * 
     * @param Reply Reply of the server.
     * @throws IOException
     */
    private void CreateDataSocket(String Reply) throws IOException {
        // Get index of "(" and ")"
        int Start = Reply.indexOf("(");
        int End = Reply.indexOf(")");

        // Get the substring of the reply to get address and port number.
        String Addr = Reply.substring(Start + 1, End);

        // Split the substring based on regex ",".
        String[] Addr_Split = Addr.split(",");

        // Create arrays to store address and port.
        String[] ServIp = new String[4];
        int[] Port = new int[2];

        // Get address and port from the string arr "Addr_Split".
        for (int i = 0; i < Addr_Split.length; i++) {
            if (i < 4) {
                ServIp[i] = Addr_Split[i];
            } else {
                Port[i - 4] = Integer.parseInt(Addr_Split[i]);
            }
        }

        // Connect to DataSocket of the Server.
        DataSocket = new Socket(ServIp[0] + "." + ServIp[1] + "." + ServIp[2] + "." + ServIp[3],
                Port[0] * 256 + Port[1]);

        // Create DataInputStream and DataOutputStreams.
        Ddis = new DataInputStream(DataSocket.getInputStream());
        Ddos = new DataOutputStream(DataSocket.getOutputStream());
    }

    /**
     * This function handles file listing response from the server.
     * 
     * @throws IOException
     */
    private void HandleList() throws IOException {
        // Receive server reply for the number of files in that directory.
        int NFiles = Integer.parseInt(Ddis.readUTF());

        // Create String array to store the filenames.
        String[] Filenames = new String[NFiles];

        // Receive server reply for the filenames in that directory.
        for (int i = 0; i < NFiles; i++) {
            Filenames[i] = Ddis.readUTF();
        }

        // Display the filenames to the client.
        System.out.println("Files in that directory are: ");
        for (String S : Filenames) {
            System.out.println(S);
        }

        // If directory is empty, display this message.
        if (NFiles == 0) {
            System.out.println("[EMPTY]");
        }
    }

    /**
     * This function handles file retrival from the server.
     * 
     * @param Args String that contains the arguments used from the command used by
     *             the client.
     * @throws IOException
     */
    private void HandleRetr(String Args) throws IOException {
        // Get Filename from the command used by the client.
        String Filename = ConvertPathToFilename(Args);

        // Open File and create FileOutputStream.
        File File = new File("Client Folder/" + Filename);
        FileOutputStream Fos = new FileOutputStream(File);

        // Create byte arr and int to monitor and store file data.
        byte[] Buffer = new byte[1024];
        int BytesRead;

        // Receive data from the server and write the file data.
        while ((BytesRead = Ddis.read(Buffer)) != -1) {
            Fos.write(Buffer, 0, BytesRead);
        }
        /*
         * Original condition of while loop: (FileSize [long] is sent by the server)
         * 
         * (FileSize > 0 && (BytesRead = Ddis.read(Buffer, 0, (int)
         * Math.min(Buffer.length, FileSize))) != -1)
         */

        // Close FileOutputStream.
        Fos.close();
    }

    /**
     * This function handles file storage from the server.
     * 
     * @param Args String that contains the arguments used from the command used by
     *             the client.
     * @throws IOException
     */
    private void HandleStor(String Args) throws IOException {
        // Get filename from the command sent by the client.
        String Filename = ConvertPathToFilename(Args);

        // Create FileInputSream object to read the file.
        File File = new File("Client Folder/" + Filename);
        FileInputStream Fis = new FileInputStream(File);

        // Create byte arr and int to monitor and store the bytes read in the file.
        byte[] Buffer = new byte[1024];
        int BytesRead;

        // Read the file and store it in byte arr, and send it to the server.
        while ((BytesRead = Fis.read(Buffer)) != -1) {
            Ddos.write(Buffer, 0, BytesRead);
        }

        // Close FileInputStream.
        Fis.close();

        // Close DataSocket and its DataInput and Output Stream.
        Ddis.close();
        Ddos.close();
        DataSocket.close();
    }

    /**
     * This function converts path into filename. For example:
     * Path: Document/Folder/FileA.txt
     * Filename: FileA.txt
     * 
     * @param Path Path of the file.
     * @return Returns the filename that is extracted from the path.
     */
    private String ConvertPathToFilename(String Path) {
        // Remove white spaces.
        Path = Path.trim();

        // Check for CRLF.
        if (Path.contains("\\r\\n")) {
            Path = Path.substring(0, Path.indexOf("\\r\\n"));
        }

        // Split the string by "/" to get the filename.
        String[] Folders = Path.split("/");

        // Remove white spaces.
        for (int i = 0; i < Folders.length; i++) {
            Folders[i] = Folders[i].trim();
        }

        // If string arr length is equal to 1, then the filename is in arr 0. Else, then
        // the filename is in arr length - 1.
        if (Folders.length == 1) {
            return Folders[0];
        } else {
            return Folders[Folders.length - 1];
        }
    }

    /**
     * This function is in charge of receiving server messages.
     * 
     * @param dis DataInputStream where the message is received.
     * @return Returns a string which represents the server message.
     * @throws IOException
     */
    private String ReceiveMsg(DataInputStream Dis) throws IOException {
        byte[] Buffer = new byte[200];

        Dis.read(Buffer);

        String Reply = new String(Buffer, StandardCharsets.UTF_8);

        return Reply;
    }

    /**
     * This function sends a message to the server.
     * 
     * @param Dos DataOutput stream where message is sent.
     * @param Msg Message to be sent.
     * @throws IOException
     */
    private void SendMsg(DataOutputStream Dos, String Msg) throws IOException {
        Dos.write(Msg.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * This function closes all socket connections when the client is exiting.
     * 
     * @throws IOException
     */
    private void CloseSocketConnections() throws IOException {
        // Check if DataSocket is null.
        if (DataSocket != null) {
            Ddis.close();
            Ddos.close();
            DataSocket.close();
        }

        // No need to check for control socket if null.
        Cdis.close();
        Cdos.close();
        ControlSocket.close();
    }
}