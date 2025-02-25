# NSCOM1_MCO2
Implementation of the File Transport Protocol (FTP) Client and Server based on RFC 259. While, the FTP server only allows up to one-connection as it is not multithreaded, it supports various commands relating to directories such as PWD, MKD, DEL. 

## Pre-requisites
- Any code editor
- Java

## How to run
1) Download the repository via git:  
`git clone https://github.com/Berylth/NSCOM1_MCO2.git`

2) Run the FTP server via the command:
`java Server`

3) Run the FTP client via the command:
`java Client`

4) Use the 'HELP' command to see the list of command supported by the application by typing:`HELP`

5) The following credentials can be used to login: (username:password)
- john:1234 
- jane:5678
- doe:qwerty
  
## Command list
Below is the list of commands supported by the application along with their description and use case.

1) USER [username]
- Command to specify the username upon login. This command must be run first before any other commands other than 'HELP'
2) PASS [password]
- Command to specify the password upon login. This command must be ran after the USER command.
  
## Members
- Loja, Kyle Flor
- Magura, Bryle Jhone R.
