
$(window).ready ->
	window.tools['auth'] =
		google:
			listing: false
			list: (success_callback=false)->
					gapi.client.setApiKey('AIzaSyC6631lpXXc5f1geA-1EsLy-uL3qIF_cjM')
					console.log "gapi loaded"
					scopes = 'https://www.googleapis.com/auth/drive'
					clientID = '380611589386.apps.googleusercontent.com'
					handleAuthResult = (res)->
						gapi.client.load 'drive', 'v2', ()->
							console.log "google drive client loaded"
							request = gapi.client.drive.files.list( {maxResults:500, q:''} )
							request.execute (res)->
								console.log "request result"
								console.log res
								window.tools.auth.google.listing = res
								if success_callback
									success_callback(res)
					gapi.auth.authorize({client_id: clientID, scope: scopes, immediate: false}, handleAuthResult)