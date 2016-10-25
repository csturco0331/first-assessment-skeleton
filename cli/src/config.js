import vorpal from 'vorpal'
import { Message } from './Message'

const cli = vorpal()

const HELPMESSAGE =
	`help :\n	returns list of available commands\
	\ndisconnect :\n	ends connection to the server\
	\necho [message]:\n	sends server a message that is then returned unchanged\
	\nbroadcast [message]:\n	sends message to all connected users\
	\nusers [message]:\n	returns a list of all connected users\
	\n@username [message]:\n	sends private message to specified user\
	\nre [message]:\n	sends private message to last user whispered with (i.e. last username to whisper you or last person you whispered)\
	\nblock :\n	blocks a specified user\
	\nunblock :\n	unblocks a specified user\
	\ntstart <password> :\n	creates or links you to a tictactoe game of the specified password\
	\ntmove <password> <int: 0-9> :\n	make a move on the tictactoe game that matches the password`

export default [
	{
		matcher: 'echo',
		options: {
			delim: cli.chalk.white.bold,
			format: (m) => `${m.timestamp} <${m.username}> (echo): ${m.contents}`
		},
		toServer: ({cliVariables, command, contents}) => {
			cliVariables.previousCommand = command
			cliVariables.server.write(new Message({ username: cliVariables.username, command: 'echo', contents }).toJSON() + '\n')
		}
	},
	{
		matcher: [ 'broadcast', 'all' ],
		options: {
			delim: cli.chalk.blue,
			format: (m) => `${m.timestamp} <${m.username}> (all): ${m.contents}`
		},
		toServer: ({cliVariables, command, contents}) => {
			cliVariables.previousCommand = command
			cliVariables.server.write(new Message({ username: cliVariables.username, command: 'broadcast', contents }).toJSON() + '\n')
		}
	},
	{
		matcher: 'users',
		options: {
			delim: cli.chalk.green,
			format: (m) => `${m.timestamp} currently connected users: ${m.contents}`
		},
		toServer: ({cliVariables, cli, command, contents}) => {
			cliVariables.previousCommand = command	
			cliVariables.server.write(new Message({ username: cliVariables.username, command: 'users', contents }).toJSON() + '\n')
		}
	},
	{
		matcher: /^@/,
		options: {
			delim: cli.chalk.magenta,
			format: (m, cliVariables) => {
				cliVariables.previousWhisper = m.username
				return `${m.timestamp} <${m.username}> (whisper): ${m.contents}`
			}
		},
		toServer: ({cliVariables, command, contents}) => {
			cliVariables.previousWhisper = command.slice(1);
			cliVariables.previousCommand = command
			cliVariables.server.write(new Message({ username: cliVariables.username, command: '@', contents: [cliVariables.previousWhisper, contents].join(' ') }).toJSON() + '\n')
		}
	},
	{
		matcher: 're',
		toServer: ({cliVariables, command, contents}) => {
			cliVariables.previousCommand = command
			cliVariables.server.write(new Message({ username: cliVariables.username, command: '@', contents: [cliVariables.previousWhisper, contents].join(' ') }).toJSON() + '\n')
		}
	},
	{
		matcher: 'connect',
		options: {
			delim: cli.chalk.yellow,
			format: (m) => `${m.timestamp}: <${m.username}> has connected`
		}
	},
	{
		matcher: 'disconnect',
		options: {
			delim: cli.chalk.red,
			format: (m) => `${m.timestamp}: <${m.username}> has disconnected`
		},
		toServer: ({cliVariables, command}) => cliVariables.server.end(new Message({ username: cliVariables.username, command: 'disconnect'}).toJSON() + '\n')
	},
	{
		matcher: 'taken',
		options: {
			delim: cli.chalk.black.bold.bgRed,
			format: (m) => 'Connection to Server failed, Username already taken'
		}
	},
	{
		matcher: 'tstart',
		options: {
			delim: cli.chalk.black.bold.bgWhite,
			format: (m) => `${m.timestamp}: <${m.username}> (tictactoe): ${m.contents}`
		},
		toServer: ({cliVariables, command, contents}) => {
			cliVariables.previousCommand = command
			cliVariables.server.write(new Message({ username: cliVariables.username, command: 'tstart', contents }).toJSON() + '\n')
		}		
	},
	{
		matcher: 'tmove',
		options: {
			delim: cli.chalk.black.bold.bgWhite,
			format: (m) => `${m.timestamp}: <${m.username}> (tictactoe): ${m.contents}`
		},
		toServer: ({cliVariables, command, contents}) => {
			cliVariables.previousCommand = command
			cliVariables.server.write(new Message({ username: cliVariables.username, command: 'tmove', contents }).toJSON() + '\n')
		}
	},
	{
		matcher: 'error',
		options: {
			delim: cli.chalk.red.bold.bgBlack,
			format: (m) => `${m.timestamp}: <${m.username}> (error): ${m.contents}`
		}
	},
	{
		matcher: 'help',
		toServer: ({cliVariables, cli, command}) => {
			cliVariables.previousCommand = command
			cli.log(HELPMESSAGE)
		}
	},
	{
		matcher: 'block',
		toServer: ({cliVariables, cli, command, contents}) => {
			cliVariables.previousCommand = command
			cliVariables.blocked = [contents.split(' ')[0], ...cliVariables.blocked]
			cli.log(`user <${contents.split(' ')[0]}> is now blocked`)
		}
	},
	{
		matcher: 'unblock',
		toServer: ({cliVariables, cli, command, contents}) => {
			cliVariables.previousCommand = command
			cliVariables.blocked = cliVariables.blocked.filter(name => name !== contents.split(' ')[0])
			cli.log(`user <${contents.split(' ')[0]}> is no longer blocked`)
		}
	},
	{
		matcher: /.*/,
		toServer: null
	}
]