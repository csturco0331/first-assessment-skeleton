import vorpal from 'vorpal'
import { split } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()

let username
let server
let previousCommand = '';

cli
  .delimiter(cli.chalk['magenta']('ftd~$'))

cli
  .mode('connect <host> <port> <username>')
  .delimiter(cli.chalk['green']('connected>'))
  .init(function (args, callback) {
    username = args.username
    server = connect({ host: args.host, port: args.port }, () => {
      server.write(new Message({ username, command: 'connect' }).toJSON() + '\n')
      callback()
    })

    server.on('data', (buffer) => {
    	let message = Message.fromJSON(buffer)  	
    	switch(message.command) {
    	  case 'echo' :
    		  //${timestamp} <${username}> (echo): ${contents}
    	      this.log(cli.chalk['white'](`${Date.now()}: <${message.username}> (echo): ${message.contents}`))
    	      break;
    	  case 'broadcast' :
    		  //${timestamp} <${username}> (all): ${contents}
    	      this.log(cli.chalk.blue(`${Date.now()}: <${message.username}> (all): ${message.contents}`))
    	      break;
    	  case 'users' :
    		  // ${timestamp}: currently connected users: (repeated) <${username}> 
    	      this.log(cli.chalk.green(`${Date.now()}: currently connected users: ${message.contents}`))
    	      break;
    	  case '@' :
    		  // ${timestamp} <${username}> (whisper): ${contents}
    	      this.log(cli.chalk.magenta(`${Date.now()}: <${message.username}> (whisper): ${message.contents}`))
    	      break;
    	  case 'connection' :
    		  // ${timestamp} <${username}> has connected
    		  this.log(cli.chalk.yellow(`${Date.now()}: <${message.username}> has connected`))
    		  break;
    	  case 'disconnection' :
    		  // ${timestamp} <${username}> has disconnected
    		  this.log(cli.chalk.black.bgRed(`${Date.now()}: <${message.username}> has disconnected`))
    		  break;
    	  default :
    		  this.log(cli.chalk.red('Connection to Server failed, Username already taken'))
    		  break;
    	}
    })

    server.on('end', () => {
      cli.exec('exit')
    })
  })
  .action(function (input, callback) {
    const [ command, ...rest ] = split(input, ' ')
    const contents = rest.join(' ')
    if (command === 'disconnect') {
      previousCommand = command;
      server.end(new Message({ username, command }).toJSON() + '\n')
    } else if (command === 'help'){
      previousCommand = command;
      this.log('help : returns list of available commands')
      this.log('disconnect : ends connection to the server')
      this.log('echo : sends server a message that is then returned unchanged')
      this.log('broadcast : sends message to all connected users')
      this.log('users : returns a list of all connected users')
      this.log('@username : sends private message to specified user')
    } else if (command === 'echo') {
      previousCommand = command;
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else if (command === 'broadcast') {
      previousCommand = command;
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else if (command === 'users') {
      previousCommand = command;
      server.write(new Message({ username, command }).toJSON() + '\n')
    } else if (command[0] === '@') {
      previousCommand = command;
      server.write(new Message({ username, command: command[0], contents: [command.slice(1), contents].join(' ') }).toJSON() + '\n')
    } else if (previousCommand !== ''){
        if (previousCommand === 'disconnect') {
          server.end(new Message({ username, command: previousCommand }).toJSON() + '\n')
        } else if (command === 'help'){
          this.log('help : returns list of available commands')
          this.log('disconnect : ends connection to the server')
          this.log('echo : sends server a message that is then returned unchanged')
          this.log('broadcast : sends message to all connected users')
          this.log('users : returns a list of all connected users')
          this.log('@username : sends private message to specified user')
        } else if (previousCommand === 'echo') {
          server.write(new Message({ username, command: previousCommand, contents: [command, contents].join(' ') }).toJSON() + '\n')
        } else if (previousCommand === 'broadcast') {
          server.write(new Message({ username, command: previousCommand, contents: [command, contents].join(' ') }).toJSON() + '\n')
        } else if (previousCommand === 'users') {
          server.write(new Message({ username, command: previousCommand }).toJSON() + '\n')
        } else if (previousCommand[0] === '@') {
          server.write(new Message({ username, command: previousCommand[0], contents: [command.slice(1), contents].join(' ') }).toJSON() + '\n')
        } else {
          this.log(`Command <${command}> was not recognized`)
        }
    } else {
      this.log(`Command <${command}> was not recognized`)
    }
    callback()
  })

