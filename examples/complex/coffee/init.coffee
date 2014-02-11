
$(window).ready($("#workspace").html(window.localStorage.getItem("demo.html")))

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