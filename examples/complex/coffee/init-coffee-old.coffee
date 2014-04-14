window.tools = {}


#$(window).ready($("#workspace").html(window.localStorage.getItem("demo.html")))

window.requestAnimFrame = ((callback) ->
    window.requestAnimationFrame or window.webkitRequestAnimationFrame or window.mozRequestAnimationFrame or window.oRequestAnimationFrame or window.msRequestAnimationFrame or (callback) ->
      window.setTimeout callback, 1000 / 60
  )()

Array::remove = (item)->
  indx = @indexOf(item)
  if indx != -1
    return @.splice(indx,1)

Array::get_last = ()->
  return this[this.length-1]

Array::set_last = (value)->
  return this[this.length-1] = value



ifrm = document.createElement("IFRAME")
ifrm.setAttribute("src", "./iframe.html")
$('#iframe_proxy').append $(ifrm)
window.ifrm = ifrm

window.doc_zoom = 1.0

$(ifrm).ready ->
	$('#iframe_proxy iframe').contents().on 'mousedown mouseup mousemove keydown keyup keypress', (e)->

	    e.origin = "iframe"
	    e.clientX += _m.outer_x + _m.ruler_w
	    e.clientY += _m.outer_y + _m.ruler_w

	    $(window).trigger e