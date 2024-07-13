package ftp;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class Server {

    private ServerSocket ServSocket;

    /* Conrol Socket and its Data input and output streams. */
    /** Socket used for ftp commands and responds. */
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
     * Array of usernames that are stored in the server as defined in the mp
     * specifications.
     */
    private final String[] Username = { "john", "jane", "doe" };

    /**
     * Array of passwords that are stored in the server as defined in the mp
     * specifications.
     */
    private final String[] Password = { "1234", "5678", "qwerty" };

    /**
     * Enum values to described login status of user. Mostly used for error
     * checking.
     */
    private enum LoginStatus {
        NotLoggedIn, UsernameAuthenticated, IsLoggedIn
    }

    /**
     * Enum values to check the file transfer type. Can be ASCII or BINARY.
     */
    private enum Type {
        ASCII, BINARY
    }

    /**
     * Enum values to describe mode for data transfer. Can be stream, block or
     * compressed.
     */
    private enum Mode {
        Stream, Block, Compressed
    }

    /**
     * Enum values to describe file structure type. Can be file, record, or page.
     */
    private enum Stru {
        File, Record, Page
    }

    /** File transfer type. */
    private Type TransferType;

    /** Mode for data transfer. */
    private Mode TransferMode;

    /** File structure type. */
    private Stru TransferStru;

    /** The current login status of the user. */
    private LoginStatus UserLoginStatus;

    /** Use to check the password of the right user. */
    private int UsernameIndex;

    /** Root Directory */
    private final String RootDirectory = "Server Folder/";

    /** Current Server directory. */
    private String CurrentDirectory = "home/";

    /** Boolean value to keep track if server is in pasv mode or not. */
    private boolean PasvModeOn;

    /**
     * Constructor for instantiating the Server class.
     * 
     * @param ServPort Port number of the server.
     * @throws IOException
     */
    private Server(int ServPort) throws IOException {
        // Listen to incoming connections.
        Listen(ServPort);

        // Handle and Execute commands from the client.
        ExecuteCommands();

    }

    /**
     * Main function.
     * 
     * @param args Contains the port number in which the server will listen for
     *             incoming connections.
     * @throws NumberFormatException
     * @throws IOException
     */
    public static void main(String[] args) throws NumberFormatException, IOException {
        new Server(Integer.parseInt(args[0]));
    }

    /**
     * This function listens at incoming connections at the port specified in this
     * function's arguement. It also creates DataInputStream and DataOutputStream
     * which is used to communicate to the client.
     * 
     * @param ServPort Port number where the server listen for incoming connections
     * @throws IOException
     */
    private void Listen(int ServPort) throws IOException {
        System.out.println("Intializing Server. . .\n");

        // Server listen at incoming connections at ServPort.
        ServSocket = new ServerSocket(ServPort);

        System.out.println("Listening at port " + ServPort + "\n");

        // Server waits for incoming connections and then passes the connection to
        // another socket.
        ControlSocket = ServSocket.accept();

        System.out.println("Client connected: " + ControlSocket.getRemoteSocketAddress());

        // Create DataOutputStream and DataInputStream which will be used to communicate
        // to client.
        Cdos = new DataOutputStream(ControlSocket.getOutputStream());
        Cdis = new DataInputStream(ControlSocket.getInputStream());

        // Set User login status to not logged in.
        UserLoginStatus = LoginStatus.NotLoggedIn;

        // Send welcome message to client.
        SendMsg(Cdos, "220 Welcome to the NSCOM01 FTP Server.\r\n");

        // Set Pasv mode to off.
        PasvModeOn = false;
    }

    /**
     * This function is in charge of processing the commands sent by the client.
     * 
     * @throws IOException
     */
    private void ExecuteCommands() throws IOException {
        // Boolean value to monitor if client is still connected.
        boolean IsConnected = true;

        // Loop while client is connected.
        do {
            // Get and interpret client request.
            String Reply = ReceiveMsg(Cdis);

            // Check if client request contains CRLF.
            if (Reply.contains("\\r\\n")) {
                Reply = Reply.substring(0, Reply.indexOf("\\r\\n"));
            }

            // Split the string.
            String[] Command = Reply.split(" ");

            // Remove whitespaces.
            for (int i = 0; i < Command.length; i++) {
                Command[i] = Command[i].trim();
            }

            // Check for client commands.
            switch (Command[0]) {
                case "USER":
                    UserLogin(Command);
                    break;
                case "PASS":
                    UserLogin(Command);
                    break;
                case "PWD":
                    Pwd(Command);
                    break;
                case "CWD":
                    Cwd(Command);
                    break;
                case "CDUP":
                    Cdup(Command);
                    break;
                case "MKD":
                    Mkd(Command);
                    break;
                case "RMD":
                    Rmd(Command);
                    break;
                case "PASV":
                    Pasv(Command);
                    break;
                case "LIST":
                    List(Command);
                    break;
                case "RETR":
                    Retr(Command);
                    break;
                case "DELE":
                    Dele(Command);
                    break;
                case "STOR":
                    Stor(Command);
                    break;
                case "HELP":
                    Help(Command);
                    break;
                case "TYPE":
                    Type(Command);
                    break;
                case "MODE":
                    Mode(Command);
                    break;
                case "STRU":
                    Stru(Command);
                    break;
                case "QUIT":
                    IsConnected = !UserExit(Command);
                    CloseSocketConnections();
                    System.out.println("\nClient logout: " + ControlSocket.getRemoteSocketAddress());
                    break;
                default:
                    SendMsg(Cdos, "500 Invalid Command.\r\n");
            }
        } while (IsConnected);
    }

    /**
     * This function handles user login from the commands "USER" and "PASS".
     * 
     * @param Command The command used for login, can be either "USER" or "PASS".
     *                Contains another arguement that is the username or password of
     *                the user.
     * @throws IOException
     */
    private void UserLogin(String[] Command) throws IOException {
        // if else to check command if "USER" or "PASS".
        if (Command[0].equals("USER")) {

            // Check if username is already authenticated or is logged in .
            if (UserLoginStatus != LoginStatus.NotLoggedIn) {
                // Send Error message since username is already authenticated or user is already
                // logged in.
                SendMsg(Cdos, "503 Bad sequence of commands.\r\n");
            } else {
                // Check command arguements.
                if (Command.length != 2) {
                    SendMsg(Cdos, "501 Invalid Parameters.\r\n");
                } else {
                    // Check for valid usernames.
                    UsernameIndex = 0;
                    for (String s : Username) {
                        if (s.equals(Command[1])) {
                            // If valid, send username ok msg.
                            SendMsg(Cdos, "331 Username okay, need password.\r\n");
                            UserLoginStatus = LoginStatus.UsernameAuthenticated;
                            return;
                        }

                        UsernameIndex++;
                    }
                    // If invalid, send error message.
                    SendMsg(Cdos, "530 Invalid username.\r\n");
                }
            }
        } else {

            // Check if username is authenticated or user is logged in.
            if (UserLoginStatus != LoginStatus.UsernameAuthenticated) {
                // Send error message since username is not yet authenticated or user is already
                // logged in.
                SendMsg(Cdos, "503 Bad sequence of commands.\r\n");
            } else {
                // Check command arguements.
                if (Command.length != 2) {
                    SendMsg(Cdos, "501 Invalid Parameters.\r\n");
                } else {
                    // Check valid passwords
                    if (Password[UsernameIndex].equals(Command[1])) {
                        // If valid, send message login is successful.
                        SendMsg(Cdos, "230 Login Successful.\r\n");
                        UserLoginStatus = LoginStatus.IsLoggedIn;

                        // Sets the TYPE to Binary, MODE to Stream, and STRU to file.
                        SetDefaultConfig();
                    } else {
                        // Send error message since invalid password.
                        SendMsg(Cdos, "530 Invalid Password.\r\n");
                    }
                }
            }

        }
    }

    /**
     * This function handles the default configurations for stru, file, and type
     * which will be sent by the client.
     * 
     * @throws IOException
     */
    private void SetDefaultConfig() throws IOException {
        // Server is expecting 3 messages from the client.
        for (int i = 0; i < 3; i++) {
            // Get and interpret client request.
            String Reply = ReceiveMsg(Cdis);

            // Check if client request contains CRLF.
            if (Reply.contains("\\r\\n")) {
                Reply = Reply.substring(0, Reply.indexOf("\\r\\n"));
            }

            // Split the string.
            String[] Command = Reply.split(" ");

            // Remove whitespaces.
            for (int j = 0; j < Command.length; j++) {
                Command[j] = Command[j].trim();
            }

            // Set configurations.
            switch (Command[0]) {
                case "STRU":
                    Stru(Command);
                    break;
                case "MODE":
                    Mode(Command);
                    break;
                case "TYPE":
                    Type(Command);
                    break;
            }
        }
    }

    /**
     * This function shows the present working directory of the server.
     * 
     * @param Command The command used for showing the current working directory.
     *                Must have no arguements.
     * @throws IOException
     */
    private void Pwd(String[] Command) throws IOException {
        // If User is not logged in, send error message.
        if (UserLoginStatus != LoginStatus.IsLoggedIn) {
            SendMsg(Cdos, "530 User not logged in.\r\n");
            return;
        }

        // Check if command has arguments.
        if (Command.length != 1) {
            SendMsg(Cdos, "501 Invalid Parameters.\r\n");
        } else {
            SendMsg(Cdos, "257 \"/" + CurrentDirectory + "\" is the current directory.\r\n");
        }
    }

    /**
     * This function changes the working directory of the server (if it exists).
     * 
     * @param Command The command contains an arguement which is used to change the
     *                present working directory.
     * @throws IOException
     */
    private void Cwd(String[] Command) throws IOException {
        // If User is not logged in, send error message.
        if (UserLoginStatus != LoginStatus.IsLoggedIn) {
            SendMsg(Cdos, "530 User not logged in.\r\n");
            return;
        }

        // Check command arguements.
        if (Command.length != 2) {
            SendMsg(Cdos, "501 Invalid Parameters.\r\n");
        } else {
            // Temporary string to store directory.
            String Dir = CurrentDirectory + Command[1] + "/";

            // Create file object.
            File File = new File(RootDirectory + Dir);

            // Check if file exists and is a folder.
            if (File.exists() && File.isDirectory()) {
                CurrentDirectory = Dir;
                SendMsg(Cdos, "250 Current directory has been changed to /" + CurrentDirectory + " .\r\n");
            } else {
                SendMsg(Cdos, "550 No such directory exists.\r\n");
            }
        }
    }

    /**
     * This function allows the user to go one level up the current directory.
     * 
     * @param Command The command for going one level up the current directory. Must
     *                not have any arguements.
     * @throws IOException
     */
    private void Cdup(String[] Command) throws IOException {
        // If User is not logged in, send error message.
        if (UserLoginStatus != LoginStatus.IsLoggedIn) {
            SendMsg(Cdos, "530 User not logged in.\r\n");
            return;
        }

        // Check command arguments
        if (Command.length != 1) {
            SendMsg(Cdos, "501 Invalid Parameters.\r\n");
        } else {
            // Temporary string to store directory.
            String Dir = RootDirectory + CurrentDirectory;

            // Split the string with the parameter "/".
            String[] Folder = Dir.split("/");

            // Remove white spaces.
            for (int i = 0; i < Folder.length; i++) {
                Folder[i] = Folder[i].trim();
            }

            // String used to store new directory.
            String NewDir = "";

            // Store the new directory on the previously created string.
            for (int i = 0; i < Folder.length - 1; i++) {
                NewDir += Folder[i] + "/";
            }

            // Check if the user has reached the root directory. The user cannot go any
            // higher if root directory has been reached.
            if (!NewDir.equals("")) {

                // If the current working directory is the root directory, the current working
                // directory that will be shown to the user is "/" instead of "Server Folder/".
                if (NewDir.equals("Server Folder/")) {
                    NewDir = "";
                } else {
                    int Start = NewDir.indexOf("/");
                    NewDir = NewDir.substring(Start + 1);
                }

                // Update variable and send confirmation message.
                CurrentDirectory = NewDir;
                SendMsg(Cdos, "200 Current directory has been changed to /" + CurrentDirectory + " .\r\n");
            } else {
                SendMsg(Cdos, "550 No such directory exists.\r\n");
            }
        }
    }

    /**
     * This function allows the user to make another directory or folder in the
     * present working directory.
     * 
     * @param Command The command for making another folder. Contains the name of
     *                the folder to be created.
     * @throws IOException
     */
    private void Mkd(String[] Command) throws IOException {
        // If User is not logged in, send error message.
        if (UserLoginStatus != LoginStatus.IsLoggedIn) {
            SendMsg(Cdos, "530 User not logged in.\r\n");
            return;
        }

        // Check command arguements.
        if (Command.length != 2) {
            SendMsg(Cdos, "501 Invalid Parameters.\r\n");
        } else {
            // Temporary string to store directory.
            String Dir = CurrentDirectory + Command[1] + "/";

            // Create file object.
            File File = new File(RootDirectory + Dir);

            // Check if file exists and is a folder.
            if (File.exists() && File.isDirectory()) {
                SendMsg(Cdos, "550 Folder already exists.\r\n");
            } else {
                // Create folder.
                File.mkdir();
                SendMsg(Cdos, "257 Directory is succesfully created.\r\n");
            }
        }
    }

    /**
     * This function removes directories from the server, including all files and
     * subdirectories stored in that directory.
     * 
     * @param Command The command for removing a directory. Contains the name
     *                of the directory to be removed.
     * @throws IOException
     */
    private void Rmd(String[] Command) throws IOException {
        // If User is not logged in, send error message.
        if (UserLoginStatus != LoginStatus.IsLoggedIn) {
            SendMsg(Cdos, "530 User not logged in.\r\n");
            return;
        }

        // Check command arguements.
        if (Command.length != 2) {
            SendMsg(Cdos, "501 Invalid Parameters.\r\n");
        } else {
            // Create file Object.
            File File = new File(RootDirectory + CurrentDirectory + Command[1] + "/");

            // Check if directory exists and is a directory.
            if (File.exists() && File.isDirectory()) {
                // Delete the directory.
                DeleteFolderData(File);
                SendMsg(Cdos, "250 Directory has been removed.\r\n");
            } else {
                SendMsg(Cdos, "550 Directory does not exists.\r\n");
            }
        }

    }

    /**
     * This function removes a folder along with the data stored inside it.
     * 
     * @param Folder Folder to be removed.
     * @throws IOException
     */
    private void DeleteFolderData(File Folder) throws IOException {
        // Store all files in folder in a file arr.
        File[] Files = Folder.listFiles();

        // File arr equals null indicates folder is empty.
        if (Files != null) {
            // Do this operation for all files in file arr.
            for (File F : Files) {
                // Check if the file is a folder.
                if (F.isDirectory()) {
                    // Call this function recursively to empty the folder.
                    DeleteFolderData(F);
                } else {
                    // delete file.
                    F.delete();
                }
            }
        }

        // Finally, delete the folder itself.
        Folder.delete();
    }

    /**
     * This function allows the client to establish connection with the data socket
     * of the server, which will be used for sending file data.
     * 
     * @param Command The command for establishing data connection.
     * @throws IOException
     */
    private void Pasv(String[] Command) throws IOException {
        // If User is not logged in, send error message.
        if (UserLoginStatus != LoginStatus.IsLoggedIn) {
            SendMsg(Cdos, "530 User not logged in.\r\n");
            return;
        }

        // Check parameters.
        if (Command.length != 1) {
            SendMsg(Cdos, "501 Invalid Parameters.\r\n");
        } else {
            // Create Random Object.
            Random Random = new Random();

            // Get and split Server Ip address.
            String ServIpAddr = ControlSocket.getLocalAddress().getHostAddress();
            String[] IpSplit = ServIpAddr.split("\\.");

            // Use Random Object to generate random numbers to determine which port the
            // client will connect to.
            int Max = 200, Min = 50;
            int P1 = Random.nextInt((Max - Min) + 1) + Min;
            int P2 = Random.nextInt((Max - Min) + 1) + Min;

            // Send the Ip address and port to the client.
            SendMsg(Cdos, "227 Entering Passive Mode (" + IpSplit[0] + "," + IpSplit[1] + "," + IpSplit[2] + ","
                    + IpSplit[3] + "," + P1 + "," + P2 + ").\r\n");

            // Bind Socket to (P1 * 256 + P2) port.
            ServSocket = new ServerSocket(P1 * 256 + P2);

            // Listen for incoming connections in that port, then create DataSocket.
            DataSocket = ServSocket.accept();

            // Create DataInputStream and DataOutputStreams.
            Ddis = new DataInputStream(DataSocket.getInputStream());
            Ddos = new DataOutputStream(DataSocket.getOutputStream());

            // Set PASV mode to true;
            PasvModeOn = true;
        }
    }

    /**
     * This function lists all files and folders present in the current directory or
     * in the directory provided by the client.
     * 
     * @param Command The command for listing the contents of a directory. May
     *                contain an arguement that indicates the directory that the
     *                client wants all of its contents to be listed..
     * @throws IOException
     */
    private void List(String[] Command) throws IOException {
        // If User is not logged in, send error message.
        if (UserLoginStatus != LoginStatus.IsLoggedIn) {
            SendMsg(Cdos, "530 User not logged in.\r\n");
            return;
        }

        // If server is not in PASV mode, send error message.
        if (!PasvModeOn) {
            SendMsg(Cdos, "425 No data connection was established.\r\n");
            return;
        }

        if (TransferMode == Mode.Stream || TransferStru == Stru.File || TransferType == Type.BINARY) {
            // If command has no args, lists all files and folders in the present directory.
            // If there are args, list all files and folders in the specified path.
            if (Command.length == 1) {
                // Send response to control socket of client.
                SendMsg(Cdos, "125 Opening data connection for file list.\r\n");

                // Open directory and get filenames under that directory.
                String Dir = RootDirectory + CurrentDirectory;
                File[] Files = (new File(Dir)).listFiles();

                // Send the number of files under that directory.
                Ddos.writeUTF(String.valueOf(Files.length));

                // Send the filenames under that directory.
                for (int i = 0; i < Files.length; i++) {
                    Ddos.writeUTF(Files[i].getName());
                }

                // Send response to control socket of client.
                SendMsg(Cdos, "226 Transfer complete.\r\n");
            } else if (Command.length == 2) {
                // Open directory.
                String Dir = RootDirectory + CurrentDirectory + Command[1] + "/";
                File Folder = new File(Dir);

                // Check if directory is a folder and if it exists.
                if (Folder.isDirectory() && Folder.exists()) {
                    // Get filenames under that folder.
                    File[] Files = Folder.listFiles();

                    // Send response to control socket of client.
                    SendMsg(Cdos, "125 Opening data connection for file list.\r\n");

                    // Send the number of files under that directory.
                    Ddos.writeUTF(String.valueOf(Files.length));

                    // Send the filenames under that directory.
                    for (int i = 0; i < Files.length; i++) {
                        Ddos.writeUTF(Files[i].getName());
                    }

                    // Send response to control socket of client.
                    SendMsg(Cdos, "226 Transfer complete.\r\n");
                } else {
                    SendMsg(Cdos, "450 Directory does not exists\r\n");
                }
            } else {
                SendMsg(Cdos, "501 Invalid Parameters.\r\n");
            }

            // Close Data Socket and input and output streams.
            Ddis.close();
            Ddos.close();
            DataSocket.close();

            // Set Pasv mode to false.
            PasvModeOn = false;
        }
    }

    /**
     * This function allows the client to download or retrieve a file from the
     * server.
     * 
     * @param Command The command for downloading a file from the server. Contains
     *                the file to be downloaded.
     * @throws IOException
     */
    private void Retr(String[] Command) throws IOException {
        // If User is not logged in, send error message.
        if (UserLoginStatus != LoginStatus.IsLoggedIn) {
            SendMsg(Cdos, "530 User not logged in.\r\n");
            return;
        }

        // If server is not in PASV mode, send error message.
        if (!PasvModeOn) {
            SendMsg(Cdos, "425 No data connection was established.\r\n");
            return;
        }

        if (TransferMode == Mode.Stream || TransferStru == Stru.File || TransferType == Type.BINARY) {
            // Check command arguments.
            if (Command.length != 2) {
                SendMsg(Cdos, "501 Invalid Parameters.\r\n");
            } else {
                // Open file.
                String Dir = RootDirectory + CurrentDirectory + Command[1];
                File File = new File(Dir);

                // Check if file exists and if it can be read.
                if (File.exists() && File.canRead()) {
                    // Send response to control socket of client.
                    SendMsg(Cdos, "150 Initiating file retrieval.\r\n");

                    // Create FileInputSream object to read the file.
                    FileInputStream Fis = new FileInputStream(File);

                    // Create byte arr and int to monitor and store the bytes read in the file.
                    byte[] Buffer = new byte[1024];
                    int BytesRead;

                    // Read the file and store it in byte arr, and send it to the client.
                    while ((BytesRead = Fis.read(Buffer)) != -1) {
                        Ddos.write(Buffer, 0, BytesRead);
                    }

                    // Close FileInputStream.
                    Fis.close();

                    // Send response to control socket of client.
                    SendMsg(Cdos, "250 File retrival of " + File.getName() + " is completed.\r\n");
                } else {
                    SendMsg(Cdos, "550 File does not exists or cannot be read.\r\n");
                }
            }

            // Close Data Socket and input and output streams.
            Ddis.close();
            Ddos.close();
            DataSocket.close();

            // Set Pasv mode to false.
            PasvModeOn = false;
        }
    }

    /**
     * This function deletes files in the server.
     * 
     * @param Command The command for deleting files in the server. Contains an
     *                arguement which is the name of the file to be deleted.
     * @throws IOException
     */
    private void Dele(String[] Command) throws IOException {
        // If User is not logged in, send error message.
        if (UserLoginStatus != LoginStatus.IsLoggedIn) {
            SendMsg(Cdos, "530 User not logged in.\r\n");
            return;
        }

        // Check command args.
        if (Command.length != 2) {
            SendMsg(Cdos, "501 Invalid Parameters.\r\n");
        } else {
            String Dir = RootDirectory + CurrentDirectory + Command[1];

            File File = new File(Dir);

            if (File.exists() && File.isFile()) {
                File.delete();
                SendMsg(Cdos, "250 File " + File.getName() + " is deleted.\r\n");
            } else {
                SendMsg(Cdos, "550 File does not exists.\r\n");
            }
        }
    }

    /**
     * This function is in charge of storing files (file upload) in the server.
     * 
     * @param Command The command for storing files in the server. It contains an
     *                aguement which is the name of the file to be stored in the
     *                server.
     * 
     * @throws IOException
     */
    private void Stor(String[] Command) throws IOException {
        // If User is not logged in, send error message.
        if (UserLoginStatus != LoginStatus.IsLoggedIn) {
            SendMsg(Cdos, "530 User not logged in.\r\n");
            return;
        }

        // If server is not in PASV mode, send error message.
        if (!PasvModeOn) {
            SendMsg(Cdos, "425 No data connection was established.\r\n");
            return;
        }

        if (TransferMode == Mode.Stream || TransferStru == Stru.File || TransferType == Type.BINARY) {
            // Check command args.
            if (Command.length != 2) {
                SendMsg(Cdos, "501 Invalid Parameters.\r\n");
            } else {
                // Send message to control socket.
                SendMsg(Cdos, "150 Initiating file upload.\r\n");

                // Get Filename from the command sent by the client.
                String Filename = (new File(Command[1])).getName();

                // Open File and create FileOutputStream.
                File File = new File(RootDirectory + CurrentDirectory + Filename);
                FileOutputStream Fos = new FileOutputStream(File);

                // Create byte arr and int to monitor and store file data.
                byte[] Buffer = new byte[1024];
                int BytesRead;

                // Receive data from the server and write the file data.
                while ((BytesRead = Ddis.read(Buffer)) != -1) {
                    Fos.write(Buffer, 0, BytesRead);
                }

                // Close FileOutputStream.
                Fos.close();

                // Send message to control socket.
                SendMsg(Cdos, "250 File upload of " + Filename + " is completed.\r\n");

                // Set Pasv mode to false.
                PasvModeOn = false;
            }
        }
    }

    /**
     * This function send the available commands in the server and how to use them.
     * 
     * @param Command The command for displaying information about the commands in
     *                the server.
     *                May contain an argument if the client wants to know how to use
     *                a specific command.
     * @throws IOException
     */
    private void Help(String[] Command) throws IOException {
        // Check command args.
        if (Command.length == 1) {
            SendMsg(Cdos, "211 The following Commands are recognized: \n" +
                    "USER\tPASS\tPWD\tCWD\tCDUP\n" +
                    "MKD\tRMD\tPASV\tLIST\tRETR\n" +
                    "DELE\tSTOR\tHELP\tTYPE\tMODE\n" +
                    "STRU\tQUIT.\n" +
                    "For more info, run the command: HELP <SP> <command> <CRLF>.\r\n");
        } else if (Command.length == 2) {
            switch (Command[1]) {
                case "USER":
                    SendMsg(Cdos, "214 Command Usage: USER <SP> <username> <CRLF>.\r\n");
                    break;
                case "PASS":
                    SendMsg(Cdos, "214 Command Usage: PASS <SP> <password> <CRLF>.\r\n");
                    break;
                case "PWD":
                    SendMsg(Cdos, "214 Command Usage: PWD <CRLF>.\r\n");
                    break;
                case "CWD":
                    SendMsg(Cdos, "214 Command Usage: CWD <SP> <pathname> <CRLF>.\r\n");
                    break;
                case "CDUP":
                    SendMsg(Cdos, "214 Command Usage: CDUP <CRLF>.\r\n");
                    break;
                case "MKD":
                    SendMsg(Cdos, "214 Command Usage: MKD <SP> <pathname> <CRLF>.\r\n");
                    break;
                case "RMD":
                    SendMsg(Cdos, "214 Command Usage: RMD <SP> <pathname> <CRLF>.\r\n");
                    break;
                case "PASV":
                    SendMsg(Cdos, "214 Command Usage: PASV <CRLF>.\r\n");
                    break;
                case "LIST":
                    SendMsg(Cdos, "214 Command Usage: LIST [<SP> <pathname>] <CRLF>.\r\n");
                    break;
                case "RETR":
                    SendMsg(Cdos, "214 Command Usage: RETR <SP> <pathname> <CRLF>.\r\n");
                    break;
                case "DELE":
                    SendMsg(Cdos, "214 Command Usage: DELE <SP> <pathname> <CRLF>.\r\n");
                    break;
                case "STOR":
                    SendMsg(Cdos, "214 Command Usage: STOR <SP> <pathname> <CRLF>.\r\n");
                    break;
                case "HELP":
                    SendMsg(Cdos, "214 Command Usage: HELP [<SP> <string>] <CRLF>.\r\n");
                    break;
                case "TYPE":
                    SendMsg(Cdos, "214 Command Usage: TYPE <SP> <type-code> <CRLF>.\r\n");
                    break;
                case "MODE":
                    SendMsg(Cdos, "214 Command Usage: MODE <SP> <mode-code> <CRLF>.\r\n");
                    break;
                case "STRU":
                    SendMsg(Cdos, "214 Command Usage: STRU <SP> <structure-code> <CRLF>.\r\n");
                    break;
                case "QUIT":
                    SendMsg(Cdos, "214 Command Usage: QUIT <CRLF>.\r\n");
                    break;
                default:
                    SendMsg(Cdos, "501 Command not recognized or supported.\r\n");
            }
        } else {
            SendMsg(Cdos, "501 Invalid Parameters.\r\n");
        }
    }

    /**
     * This function sets the transfer type to be used by the server.
     * 
     * @param Command The command for setting the transfer type. Contains the
     *                argument on what transfer type to use.
     * @throws IOException
     */
    private void Type(String[] Command) throws IOException {
        // Check if user is logged in.
        if (UserLoginStatus != LoginStatus.IsLoggedIn) {
            SendMsg(Cdos, "530 User not logged in.\r\n");
            return;
        }

        // Check parameters.
        if (Command.length != 2) {
            SendMsg(Cdos, "501 Invalid Parameters.\r\n");
        } else {
            // Check command arguements.
            switch (Command[1]) {
                case "I":
                    SendMsg(Cdos, "200 Setting transfer type to Binary.\r\n");
                    TransferType = Type.BINARY;
                    break;
                case "A":
                default:
                    SendMsg(Cdos, "504 Command not implemented for that parameter.");
            }
        }
    }

    /**
     * This function sets the transfer mode to be used by the server.
     * 
     * @param Command The command for setting the transfer mode. Contains the
     *                arguement on what mode to be used.
     * @throws IOException
     */
    private void Mode(String[] Command) throws IOException {
        // Check if user is logged in.
        if (UserLoginStatus != LoginStatus.IsLoggedIn) {
            SendMsg(Cdos, "530 User not logged in.\r\n");
            return;
        }

        // Check parameters.
        if (Command.length != 2) {
            SendMsg(Cdos, "501 Invalid Parameters.\r\n");
        } else {
            // Check command arguements.
            switch (Command[1]) {
                case "S":
                    SendMsg(Cdos, "200 Setting transfer mode to Stream.\r\n");
                    TransferMode = Mode.Stream;
                    break;
                case "B":
                case "C":
                default:
                    SendMsg(Cdos, "504 Command not implemented for that parameter.");
            }
        }
    }

    /**
     * This function sets the file structure to be used by the server.
     * 
     * @param Command The command for setting the file structure. Contains the
     *                arguements on what file structure to be used.
     * @throws IOException
     */
    private void Stru(String[] Command) throws IOException {
        // Check if user is logged in.
        if (UserLoginStatus != LoginStatus.IsLoggedIn) {
            SendMsg(Cdos, "530 User not logged in.\r\n");
            return;
        }

        // Check parameters.
        if (Command.length != 2) {
            SendMsg(Cdos, "501 Invalid Parameters.\r\n");
        } else {
            // Check command arguements.
            switch (Command[1]) {
                case "F":
                    SendMsg(Cdos, "200 Setting file structure to File-Oriented.\r\n");
                    TransferStru = Stru.File;
                    break;
                case "R":
                case "P":
                default:
                    SendMsg(Cdos, "504 Command not implemented for that parameter.");
            }
        }
    }

    /**
     * This function is in charge of handling the command "EXIT". Checks the command
     * arguements and returns true if the command syntax is valid, else false. If
     * true, the client can successfully terminate the FTP session.
     * 
     * @param Command Command to be executed which is "EXIT".
     * @return Returns true if the command syntax is valid, else false
     * @throws IOException
     */
    private boolean UserExit(String[] Command) throws IOException {
        if (Command.length == 1) {
            SendMsg(Cdos, "221 Closing connection to FTP Server, Goodbye.\r\n");
            return true;
        } else {
            SendMsg(Cdos, "500 Invalid Parameters.\r\n");
            return false;
        }
    }

    /**
     * This function is in charge of receiving client messages.
     * 
     * @param dis DataInputStream where the message is received.
     * @return Returns a string which represents the client message.
     * @throws IOException
     */
    private String ReceiveMsg(DataInputStream Dis) throws IOException {
        byte[] Buffer = new byte[200];

        Dis.read(Buffer);

        String Reply = new String(Buffer, StandardCharsets.UTF_8);

        return Reply;
    }

    /**
     * This function sends a message to the client.
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

/*
 *
 * RFC 959: Page 37 (3-digit reply codes)
 * https://datatracker.ietf.org/doc/html/rfc959
 * https://github.com/pReya/ftpServer
 */
