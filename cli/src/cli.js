import vorpal from 'vorpal'
import { split } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()

//=========defaults and consts===============
const PORT = '8080'
const HOST = 'localhost'	
const CONNECT = 'connect'
const DISCONNECT = 'disconnect'
const ECHO = 'echo'
const BROADCAST = 'broadcast'
const ALL = 'all'
const USERS = 'users'
const AT = '@'
const CONNECTION = 'connection'
const DISCONNECTION = 'disconnection'
const TAKEN = 'taken'
const BLOCK = 'block'
const UNBLOCK = 'unblock' 
const HELP = 'help'
const RE = 're'
const CLIDELIM = cli.chalk.magenta.bold('ftd~$')
const MODEDELIM = cli.chalk.green.bold('connected>')
const ECHODELIM = cli.chalk.white.bold
const BROADCASTDELIM = cli.chalk.blue
const USERSDELIM = cli.chalk.green
const ATDELIM = cli.chalk.magenta
const CONNDELIM = cli.chalk.yellow
const DISCONDELIM = cli.chalk.red
const TAKENDELIM = cli.chalk.black.bold.bgRed
const TAKENMESSAGE = 'Connection to Server failed, Username already taken'
const UNBLOCKMESSAGE = 'user <${temp}> is no longer blocked'
const HELPMESSAGE =
	'help : returns list of available commands\
	\ndisconnect : ends connection to the server\
	\necho : sends server a message that is then returned unchanged\
	\nbroadcast : sends message to all connected users\
	\nusers : returns a list of all connected users\
	\n@username : sends private message to specified user\
	\nre : sends private message to last user whispered with (i.e. last username to whisper you or last person you whispered)\
	\nblock : blocks a specified user\
	\nunblock : unblocks a specified user'
//============================================
//============class variables=================
let username				//current user set on creation, used on disconnect
let server					//used with connect from 'net' library
let previousCommand = '';	//keeps track of the previous command to allow quicker typing of repeat commands
let previousWhisper = '';	//keeps track of the previous whisperer to allow quicker private chatting
let blocked = [];			//list of all users you don't want to hear from
//============================================
//==============implementation================
cli
  .delimiter(CLIDELIM) 								//changes the command prompt

cli
  .mode('connect <username> [host] [port]')			//actives a mode for vorpal : username is required, host and port are optional
  .delimiter(MODEDELIM)								//changes command prompt
  .init(function (args, callback) {					//initalizing function for the 'connect' mode
    username = args.username						//username is set
    let host = (args.host)							//host terinary check
    	? args.host									//host provided : use inputed host
    	: HOST										//host not provided : use default
    let port = (args.port)							//port terinary check
    	? args.port									//port provided : use inputed port
    	: PORT										//port not provided : use default
    server = connect({ host, port }, () => {		//create the server connect (uses net library)
      server.write(new Message({ username, command: CONNECT, }).toJSON() + '\n')		//writes connection message to server
      callback()									//callback to mode
    })

    server.on('data', (buffer) => {					//triggers every time the server sends a message
    	let message = Message.fromJSON(buffer)		//get Message from string
    	if(!blocked.some(name => name === message.username)) {		//check that the sending user isn't blocked
	    	//handles different output formatting depending on types of commands
    		switch(message.command) {
	    	  case ECHO :
	    		  // ${timestamp} <${username}> (echo): ${contents}
	    	      this.log(ECHODELIM(`${message.timestamp}: <${message.username}> (echo): ${message.contents}`))
	    	      break;
	    	  case BROADCAST :
	    		  // ${timestamp} <${username}> (all): ${contents}
	    	      this.log(BROADCASTDELIM(`${message.timestamp}: <${message.username}> (all): ${message.contents}`))
	    	      break;
	    	  case USERS :
	    		  // ${timestamp}: currently connected users: (repeated)
					// <${username}>
	    	      this.log(USERSDELIM(`${message.timestamp}: currently connected users: ${message.contents}`))
	    	      break;
	    	  case AT :
	    		  // ${timestamp} <${username}> (whisper): ${contents}
	    	      this.log(ATDELIM(`${message.timestamp}: <${message.username}> (whisper): ${message.contents}`))
	    	      previousWhisper = message.username;
	    	      break;
	    	  case CONNECTION :
	    		  // ${timestamp} <${username}> has connected
	    		  this.log(CONNDELIM(`${message.timestamp}: <${message.username}> has connected`))
	    		  break;
	    	  case DISCONNECTION :
	    		  // ${timestamp} <${username}> has disconnected
	    		  this.log(DISCONDELIM(`${message.timestamp}: <${message.username}> has disconnected`))
	    		  break;
	    	  case TAKEN :
	    		  this.log(TAKENDELIM(TAKENMESSAGE))
	    		  break;
	    	}
    	}
    })

    server.on('end', () => {		//called when user types disconnect
      cli.exec('exit')				//exits the connect mode
    })
  })
  .action(function (input, callback) {					//called every time a user issues a command
    const [ command, ...rest ] = split(input, ' ')		//retrieving the command and rest of the message from input
    const contents = rest.join(' ')						//creating a single string from the rest of the message
    
    //====================================================
    if (command === DISCONNECT) {
      previousCommand = command
      server.end(new Message({ username, command }).toJSON() + '\n')
      
    //====================================================
    } else if (command === HELP){
      previousCommand = command
      this.log(HELPMESSAGE)
      
    //====================================================
    } else if (command === ECHO) {
      previousCommand = command
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
          
    //====================================================
    } else if (command === BROADCAST || command === ALL) {
      previousCommand = BROADCAST
      server.write(new Message({ username, command: BROADCAST, contents }).toJSON() + '\n')
          
    //====================================================
    } else if (command === USERS) {
      previousCommand = command
      server.write(new Message({ username, command }).toJSON() + '\n')
          
    //====================================================
    } else if (command[0] === AT) {
      previousCommand = command
      previousWhisper = command.slice(1)
      server.write(new Message({ username, command: AT, contents: [command.slice(1), contents].join(' ') }).toJSON() + '\n')
          
    //====================================================
    } else if (command === RE) {
      previousCommand = command
      server.write(new Message({ username, command: AT, contents: [previousWhisper, contents].join(' ') }).toJSON() + '\n')
          
    //====================================================
    } else if (command === BLOCK) {
    	previousCommand = command
    	blocked = [rest[0],...blocked]
    	this.log(`user <${rest[0]}> now blocked`)
    	    
    //====================================================
    } else if (command === UNBLOCK) {
    	previousCommand = command
    	blocked = blocked.filter(name => name !== rest[0])
    	this.log(`user <${rest[0]}> is no longer blocked`)
    	    
    //====================================================
    } else if (previousCommand !== ''){
        
    //====================================================
        if (previousCommand === DISCONNECT) {
          server.end(new Message({ username, command: previousCommand }).toJSON() + '\n')
        
    //====================================================
        } else if (command === HELP){
          this.log(HELPMESSAGE)
        
    //====================================================
        } else if (previousCommand === ECHO) {
          server.write(new Message({ username, command: previousCommand, contents: [command, contents].join(' ') }).toJSON() + '\n')
        
    //====================================================    
        } else if (previousCommand === BROADCAST) {
          server.write(new Message({ username, command: previousCommand, contents: [command, contents].join(' ') }).toJSON() + '\n')
        
    //====================================================    
        } else if (previousCommand === USERS) {
          server.write(new Message({ username, command: previousCommand }).toJSON() + '\n')
        
    //====================================================    
        } else if (previousCommand[0] === AT) {
          server.write(new Message({ username, command: previousCommand[0], contents: [previousWhisper, command, contents].join(' ') }).toJSON() + '\n')
        
    //====================================================    
        } else if (previousCommand === RE) {
            server.write(new Message({ username, command: AT, contents: [previousWhisper, command, contents].join(' ') }).toJSON() + '\n')
        
    //====================================================    
        } else if (previousCommand === BLOCK) {
        	blocked = [command,...blocked]
        	this.log(`user <${command}> now blocked`)
        
    //====================================================    
        } else if (previousCommand === UNBLOCK) {
        	blocked.filter(name => name !== command)
        	this.log(`user <${command}> is no longer blocked`)
        
    //====================================================    
        } else {
          this.log(`Command <${command}> was not recognized`)
        }
        
    //====================================================
    } else {
      this.log(`Command <${command}> was not recognized`)
    }
    callback()
  })
  //================================================