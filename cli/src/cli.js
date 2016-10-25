import vorpal from 'vorpal'
import { split } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'
import config from './config'
import condition from './condition'

export const cli = vorpal()

//=========defaults and consts===============
const PORT = '8080'
const HOST = 'localhost'	
const CLIDELIM = cli.chalk.magenta.bold('ftd~$')
const MODEDELIM = cli.chalk.green.bold('connected>')

//============================================
//============class variables=================
let cliVariables = {	
			username: '',			//current user set on creation, used on disconnect
			server: '',				//used with connect from 'net' library
			previousCommand: '',	//keeps track of the previous command to allow quicker typing of repeat commands
			previousWhisper: '',	//keeps track of the previous whisperer to allow quicker private chatting
			blocked: []				//list of all users you don't want to hear from
}
	//============================================
//==============implementation================
cli
  .delimiter(CLIDELIM) 											//changes the command prompt

cli
  .mode('connect <username>')									//actives a mode for vorpal : username is required, host and port are optional
  .option('-h, --host <h>', 'IP address to connect to')			//host is optional
  .option('-p, --port <p>', 'Port to use for the connect')		//port is optional
  .delimiter(MODEDELIM)											//changes command prompt
  .init(function (args, callback) {								//initalizing function for the 'connect' mode
	cliVariables.username = args.username						//username is set
	let host = (args.options.host)								//host terinary check
    	? args.options.host										//host provided : use inputed host
    	: HOST													//host not provided : use default
    let port = (args.options.port)								//port terinary check
    	? args.options.port										//port provided : use inputed port
    	: PORT													//port not provided : use default
    cliVariables.server = connect({ host, port }, () => {		//create the server connect (uses net library)
    	cliVariables.server.write(new Message({ username: cliVariables.username, command: 'connect' }).toJSON() + '\n')		//writes connection message to server
      callback()												//callback to mode
    })

    cliVariables.server.on('data', (buffer) => {				//triggers every time the server sends a message
    	let message = Message.fromJSON(buffer)					//get Message from string
    	if(!cliVariables.blocked.some(name => name === message.username)) {		//check that the sending user isn't blocked
	    	//handles different output formatting depending on types of commands
    		let {options: {delim, format}} = config.find((command) => condition({command: message.command, matcher: command.matcher}))
    		this.log(delim(format(message, cliVariables)))
    	}
    })

    cliVariables.server.on('end', () => {						//called when user types disconnect
      cli.exec('exit')											//exits the connect mode
    })
  })
  .action(function (input, callback) {							//called every time a user issues a command
    const [ command, ...rest ] = split(input, ' ')				//retrieving the command and rest of the message from input
    let contents = rest.join(' ')								//creating a single string from the rest of the message
    let {toServer} = config.find((obj) => condition({command, matcher: obj.matcher}))
    if(toServer) {
    	toServer({cliVariables, cli, command, contents})
    } else {
    	let{toServer} = config.find((obj) => condition({command: cliVariables.previousCommand, matcher: obj.matcher}))
    	if(toServer) {
    		contents = [command, contents].join(' ')
    		toServer({cliVariables, cli, command: cliVariables.previousCommand, contents})
    	} else {
    		this.log(`Command <${command}> was not recognized`)
    	}
    }
    callback()
  })
  //================================================