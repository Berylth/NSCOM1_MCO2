# NSCOM1_MCO2: FTP Client and Server in Java 
Implementation of the File Transport Protocol (FTP) Client and Server based on RFC 259. While the FTP server only allows up to one-connection as it is not multithreaded, it supports various commands such as those relating to directories such as PWD, MKD, DEL. 

## Pre-requisites
- Any code editor
- Java

## How to run
1) Download the repository via git:  
    ```
    git clone https://github.com/Berylth/NSCOM1_MCO2.git
    ```

2) Run the FTP server via the command:
    ```
    java Server
    ```

3) Run the FTP client via the command:
    ```
    java Client
    ```

4) Use the 'HELP' command to see the list of command supported by the application by typing:
    ```
    HELP
    ```

5) The following credentials can be used to login: (username:password)
- john:1234 
- jane:5678
- doe:qwerty
  
## Command list
Below is the list of commands supported by the application along with their description and use case.

1) USER [username]
  - Command to specify the username upon login. This command must be run first before any other commands other than 'HELP' and 'QUIT'.
2) PASS [password]
  - Command to specify the password upon login. This command must be run after the USER command.
3) PWD
  - Shows the present working directory of the FTP server.
4) CWD [directory]
  - Allows the user to change directory within the FTP server.
5) CDUP
  - Allows the user to move up the directory within the FTP server.
6) MKD [directory]
  - Makes a new directory within the FTP server.
7) RMD [directory]
  - Removes an existing directory within the FTP server.
8) PASV
  - Enables Passive mode for data transfer. Creates a new sockets specifically for data transfers. This command must be run first before 'LIST', 'RETR', and 'STOR'.
9) LIST [directory]
  - List all files within the current directory if the directory name is not specified by the user. Else, lists all files within the specified directory.
10) RETR [pathname]
  - Downloads a file from the FTP server based on the specified path.
11) DELE [pathname]
  - Deletes a file from the FTP server based on the specified path.
12) STOR [filename]
  - Uploads a file from the FTP server.
13) HELP [command]
  - Shows the list of commands. If the command name is specified, shows the specific usage of that command.
14) QUIT
  - Terminates the session between the FTP Client and Server.
  
## Members
- Loja, Kyle Flor
- Magura, Bryle Jhone R.
