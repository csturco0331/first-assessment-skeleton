const condition = ({ command, matcher }) =>
	matcher instanceof RegExp
		? matcher.test(command)
		: typeof matcher === 'string'
			? command === matcher
			: Array.isArray(matcher)
				? matcher.some(m => condition({ command, matcher: m }))
				: matcher()
			
export default condition