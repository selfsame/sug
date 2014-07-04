if window.DEBUG
  console.log "running color_picker.js"


$(window).ready ->

 

	window.tools['color_picker'] =
		window_open: 0
		user_change: 0
		refresh_hue: 0
		refresh_s_l: 0
		alpha: 1
		usage_mode: 0 #0 is css panel adjustment, 1 is content editing mode
		preview_callback: 0
		rgb: [0,0,0]
		picker_pos: [0,0]

		init: ()->
			@picker = $('<div id="jpcolor"></div>')
			@picker.data('docked', 0)
			@picker.append $('
				<div class="color-preview-background"></div>
				<div class="color-preview"></div>	
				<p class="hsl_readout">hsl(0,0,0)</p>
				<canvas id="jpc" width="200" height="200"></canvas>
				<canvas class="color_slider" id="hue_slider" width="10" height="200"></canvas>
				<canvas class="color_slider" id="alpha_slider" width="10" height="200"></canvas>
				<input id="hex-input">
				<button id="close_color_picker">cancel</button>
				<button id="confirm_color_picker">confirm</button>	
				<label>transparent <input type="checkbox"></label>
				<div class="slider_handle"></div>
				<div class="slider_handle"></div>
				<div class="color_point"></div>')

			@color_canvas = @picker.children('#jpc')
			@color_point = @picker.children('.color_point')
			@color_preview = @picker.children('.color-preview')
			@hex_input = @picker.children('#hex-input')
			@hex_input.change (e)->
				v = window.tools['color_picker'].hexToRgb $(this).val()
				#console.log v
				if v
					window.tools['color_picker'].setup_color 'rgb('+v[0]+','+v[1]+','+v[2]+')'
			@picker.children('#close_color_picker').click (e)->
				window.tools['color_picker'].cancel_pick()
			@picker.children('#confirm_color_picker').click (e)->
				window.tools['color_picker'].confirm_color()
			@color_canvas.mousedown (e)->
				window.tools['color_picker'].pick_event = 1
				window.tools['color_picker'].select_color e
			@color_canvas.mousemove (e)->
				if window.tools['color_picker'].pick_event is 1
					window.tools['color_picker'].select_color e
			@color_canvas.mouseup (e)->
				window.tools['color_picker'].pick_event = 0
			#we want to be able to drag the color point even if not in the exact bounds of the canvas
			@picker.mousemove (e)->
				if window.tools['color_picker'].pick_event is 1
					window.tools['color_picker'].select_color e
			@picker.mouseup (e)->
				window.tools['color_picker'].pick_event = 0
			@picker.mouseleave (e)->
				window.tools['color_picker'].pick_event = 0
			@w = 200
			@h = 200
			@draw_sliders()
			@setup_color 'rgb(0,0,0)'

		cancel_pick: ()->
			#console.log 'cancel pick', @window_open
			@picker.detach()
			@window_open = 0
			window.no_interaction = 0
			rule = @current_target.rule
			for element in @selection
				cache = $(element).data('color_change_cache')
				console.log "Cancel cache", cache
				if cache in ["", "none"]
					$(element).css(rule, '')
				else
					$(element).css(rule, $(element).data('color_change_cache'))
			_t.s.widget_active = false

		show_picker: (target, mode=0, color=0, offset={left:0,top:0}, callback=0)->
			@usage_mode = mode
			if callback
				@preview_callback = callback
			@window_open = 1
			window.no_interaction = 1
			#convert to a dialogue type window, only used when picking
			$('#misc').append @picker
			@current_target = target

			o = offset
			h = @picker.height()
			t = o.top
			overage =  (o.top+h)+20 - $(window).height()
			if overage > 0
				t -= overage
			l = o.left+20
			if l < 10
				l = 10
			if l + @picker.width() > $(window).width() - 20
				l = $(window).width() - 20 - @picker.width()
			@picker.offset( {left: l, top: t})

			if @usage_mode is 0

				@selection = _t.selected_elements
				rule = @current_target.rule

				for element in @selection
					console.log element.style[window.get_dom_style(rule)] 
					$(element).data('color_change_cache', element.style[window.get_dom_style(rule)] )
			if color
				@setup_color color

		draw_sliders: ()->
			@hue_slider = @picker.children('#hue_slider')
			@hue_handle = $(@picker.children('.slider_handle')[0])
			context = @hue_slider[0].getContext('2d')

			for y in [0..199]
				hue = (y+1)/200 * 360
				color = 'hsl(' + hue    + ', 100%, 50%)'
				context.fillStyle = color
				context.fillRect(0,y,10,1)

			hue_bar = @hue_slider
			@hue_slider.mousedown (e)->
				$(this).data('in_use', 1)
				handle_hue_change(e)
				window.global_mouse_callback = $(this)
			@hue_slider.data 'global_mousemove', (e)->
				if hue_bar.data('in_use') is 1
					handle_hue_change(e)
			@hue_slider.data 'global_mouseup', (e)->
				hue_bar.data('in_use', 0)
				window.global_mouse_callback = 0

			handle_hue_change = (e)->
				#try to limit recalculation to the values that changed
				window.tools['color_picker'].user_change = 1
				window.tools['color_picker'].refresh_hue = 1

				o = window.tools['color_picker'].hue_slider.offset()
				y = e.clientY - o.top
				h = window.tools['color_picker'].h
				if y < 0
					y = 0
				if y > h
					y = h
				
				window.tools['color_picker'].hue = parseInt(y/h * 360)
				window.tools['color_picker'].show_value()
				pos = window.tools['color_picker'].picker_pos
				window.tools['color_picker'].color_from_position( pos[0]*window.tools['color_picker'].w, pos[1]*window.tools['color_picker'].h )
				window.tools['color_picker'].update_color_info()

			@alpha_slider = @picker.children('#alpha_slider')
			@alpha_handle = $(@picker.children('.slider_handle')[1])
			context = @alpha_slider[0].getContext('2d')
			grd = context.createLinearGradient(0, 0, 0, 200)
			grd.addColorStop(0, 'rgba(0,0,0,0)')
			grd.addColorStop(1, 'rgba(0,0,0,1)')
			context.fillStyle = grd
			context.fillRect(0,0,10,200)

			alpha_bar = @alpha_slider
			@alpha_slider.mousedown (e)->
				$(this).data('in_use', 1)
				handle_alpha_change(e)
				window.global_mouse_callback = $(this)
			@alpha_slider.data 'global_mousemove', (e)->
				if alpha_bar.data('in_use') is 1
					handle_alpha_change(e)
			@alpha_slider.data 'global_mouseup', (e)->
				alpha_bar.data('in_use', 0)
				window.global_mouse_callback = 0

			handle_alpha_change = (e)->
				#console.log 'handle alpha change'
				#try to limit recalculation to the values that changed
				window.tools['color_picker'].user_change = 1

				o = window.tools['color_picker'].hue_slider.offset()
				y = e.clientY - o.top
				h = window.tools['color_picker'].h
				if y < 0
					y = 0
				if y > h
					y = h
				window.tools['color_picker'].alpha = (y/h).toFixed(2)

				window.tools['color_picker'].show_value()
				pos = window.tools['color_picker'].picker_pos
				window.tools['color_picker'].color_from_position( pos[0]*window.tools['color_picker'].w, pos[1]*window.tools['color_picker'].h )
				window.tools['color_picker'].update_color_info()

		update_sliders: ()->
			#console.log 'update_sliders'
			o = @hue_slider.offset()
			@hue_handle.offset( {left:o.left-2, top:o.top+ @hue/360 * @h } )
			o = @alpha_slider.offset()
			@alpha_handle.offset( {left:o.left-2, top:o.top+ @alpha * @h } )

		select_color: (e)->
			#set the color point
			o = @color_canvas.offset()
			x = e.clientX - o.left
			y = e.clientY - o.top
			if x < 0
				x = 0
			if y < 0
				y = 0
			if x > @w
				x = @w
			if y > @h
				y = @h

			s = x/@w * 100
			l = (@h-y)/@h * 100
			#lightness is 50% for a completely saturated color.. try scaling it on the x axis
			l = l / (1 + x / @w ) 
			s = parseInt(s.toFixed(0))
			l = parseInt(l.toFixed(0) )
			@saturation = s
			@lightness = l
			@user_change = 1
			

			#get the pixel data of the color
			color = @color_from_position x,y
			#console.log 'COLOR FROM CANVAS: ', color

			@rgb = color
			@hsl = @rgb_to_hsl color[0], color[1], color[2]
			@hue = @hsl[0]
			@saturation = @hsl[1]
			@lightness = @hsl[2]
			@hex = @rgb2hex @rgbstring

			@update_color_info()

			#nc = @position_from_rgb @rgb
			#if not @debug_point
			#	@debug_point = $('<div style="height:1px;width:1px;position:absolute;border:1px solid black;background-color:white;"></div>')
			#	@picker.append @debug_point
			#@debug_point.offset( {left:o.left+nc[1]*@w, top:o.top+nc[0]*@h} )

		color_from_position: (x,y)->
			x = parseInt(x)
			y = parseInt(y)
			o = window.tools.color_picker.color_canvas.offset()
			context = @color_canvas[0].getContext('2d')
			data = context.getImageData(x,y,1,1).data
			color = [data[0], data[1], data[2]]
			window.tools.color_picker.picker_pos = [x / @w, y / @h]
			window.tools.color_picker.color_point.offset( { top:o.top - 5 + y, left:o.left - 5 + x} )
			window.tools.color_picker.rgb = color
			#console.log '$$', color, x, y
			return color

		position_from_rgb: (rgb)->
			@rgb = [parseInt(rgb[0]), parseInt(rgb[1]), parseInt(rgb[2])]
			hsl = @rgb_to_hsl parseInt(rgb[0]), parseInt(rgb[1]), parseInt(rgb[2])
			hue = hsl[0]
			#console.log 'hsl: ', hsl
			rgb_hue = @hsl2rgb @hue, 100, 50
			best = 0
			bestv = 0
			i = 0
			for c in rgb_hue
				if c > bestv
					bestv = c
					best = i
				i += 1

			ratio = @rgb[best] / rgb_hue[best]
			adj_rgb = [rgb_hue[0] * ratio, rgb_hue[1] * ratio, rgb_hue[2] * ratio ]

			i = 0
			sat = 1
			for c in @rgb
				if i isnt best
					g = @rgb[i] / @rgb[best]
					if g < sat
						sat = g
				i += 1
			pos = [(1-ratio), (1-sat)]
			return pos

		set_color_point_from_color: ()->
			o = @color_canvas.offset()
			pos = @position_from_rgb @rgb
			#console.log '***', @rgb, @picker_pos, pos
			@picker_pos = pos
			@color_point.offset( { top:o.top - 5 + pos[0] * @h, left:o.left - 5 + pos[1] * @w} )

		update_color_info: ()->
			#console.log 'update_color_info'
			display = @picker.children('.hsl_readout')
			o = @color_canvas.offset()
			display.html 'hsl('+@hue+', '+@saturation+', '+@lightness+')'
			vis_lightness = @lightness * (1 + @saturation / 100)		

			if not @rgb
				@rgb = @hsl2rgb @hue, @saturation, @lightness

			@rgbstring = 'rgb('+@rgb[0]+', '+@rgb[1]+', '+@rgb[2]+')'
			if @alpha < 1
				@rgbstring = 'rgba('+@rgb[0]+', '+@rgb[1]+', '+@rgb[2]+', '+@alpha+')'

			display.append '<br>'+@rgbstring
			@hex = @rgb2hex @rgbstring
			
			@hex_input.attr('value', @hex)
			#determine if we need a rgba value
			if @alpha < 1
				@preview = @rgbstring
			else
				@preview = @hex

			@color_preview.css('background-color', @preview)
			#preview the changes
			if @usage_mode is 0
				if @current_target
					rule = @current_target.rule
					for element in _t.selected_elements
						$(element).css(rule, @preview) 

			

		confirm_color: ()->
			#console.log 'confirm color'
			color_box = @current_target.color_display
			color_input = @current_target.input
			color_input.attr('value', @preview)
			color_box.css('background-color', @hex)
			if @usage_mode is 0
				@current_target.input.change()
			else if @preview_callback
				@preview_callback @preview
			@picker.detach()
			@window_open = 0
			window.no_interaction = 0			

		setup_color: (color_css)->
			#console.log 'setup, ', color_css

			@last_color_displayed = color_css
			if color_css in ["", "none"]
				@last_color_displayed = "none"
				color_css = "rgba(0,0,0,1)"
			rgb = @parse_color color_css
			@rgb = rgb
			if rgb.length is 4
				@alpha = rgb[3]
			hsl = @rgb_to_hsl rgb[0],rgb[1], rgb[2]
			#console.log hsl
			@hue = hsl[0]
			@saturation = hsl[1]
			@lightness = hsl[2]

			@show_value()
			@set_color_point_from_color()

		show_value: ()->
			#console.log 'show_value'
			rgb_hue = @hsl2rgb @hue, 100, 50
			c = [ rgb_hue[0]/255,rgb_hue[1]/255,rgb_hue[2]/255 ]

			#get the rgba value, convert to floats
			#convert c to full intensity, and get values to place 'pointer' at correct variation
			canvas = @picker.children('#jpc')
			context = canvas[0].getContext('2d')
			imageData = context.createImageData(200, 200)
			pixels = imageData.data
			#the color ratio
			#c = [1.0,0.5,0.0]
			#find the inverse ratio and divide by the width increment
			cr = [(1-c[0]) / @w, (1-c[1]) / @w, (1-c[2]) / @w]
			for y in [0..199]
				for x in [0..199]
					i = (y*200*4) + (x*4)
					ry = @h-y #reverse y index
					rx = @w-x #reverse y index
					pixels[i] =   parseInt( (c[0]+rx*cr[0]) * 255 * (ry/200) ) 
					pixels[i+1] = parseInt( (c[1]+rx*cr[1]) * 255 * (ry/200) ) 
					pixels[i+2] = parseInt( (c[2]+rx*cr[2]) * 255 * (ry/200) ) 
					pixels[i+3] = 255
			context.putImageData(imageData, 0, 0)

			@update_sliders()
			@update_color_info()
		

		#conversion functions.  move window.rgb_to_hex here
		parse_color: (color_string)->
			# rgba(0,0,0,0)
			c = [0,0,0,1.0]
			rgba = 0
			if color_string.substr(0,4) is 'rgba'
				rgba = 1
			if color_string.substr(0,3) is 'rgb'
				s = color_string.split('(')[1]
				s = s.split(')')[0]
				s = s.split(',')
				c[0] = s[0]
				c[1] = s[1]
				c[2] = s[2]
				if rgba
					c[3] = parseFloat(s[3])
				return c

		rgb2hex: (rgb)->
		  if rgb is undefined or rgb is '' or rgb is 'none'
		    return 'transparent'
		  if typeof rgb is 'string' 
		  	rgb = rgb.split('(')[1].split(')')[0].split(',')
		  #else assume it's an array
		  return "#" +("0" + parseInt(rgb[0],10).toString(16)).slice(-2) +
		  ("0" + parseInt(rgb[1],10).toString(16)).slice(-2) +
		  ("0" + parseInt(rgb[2],10).toString(16)).slice(-2)

		hexToRgb: (hex) ->
		  #http://stackoverflow.com/questions/5623838/rgb-to-hex-and-hex-to-rgb
		  result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
		  if result
		    [parseInt(result[1], 16),
		    parseInt(result[2], 16),
		    parseInt(result[3], 16)]
		  else
		  	null

		rgb_to_hsl: (r, g, b) ->
			#http://stackoverflow.com/questions/2348597/why-doesnt-this-javascript-rgb-to-hsl-code-work
			r /=255
			g /=255
			b /=255
			max = Math.max(r, g, b)
			min = Math.min(r, g, b)
			h = undefined
			s = undefined
			l = (max + min) / 2
			if max is min
				h = s = 0 # achromatic
			else
				d = max - min
				s = (if l > 0.5 then d / (2 - max - min) else d / (max + min))
				switch max
					when r
						h = (g - b) / d + ((if g < b then 6 else 0))
					when g
						h = (b - r) / d + 2
					when b
						h = (r - g) / d + 4
				h /= 6
			[Math.floor(h * 360), Math.floor(s * 100), Math.floor(l * 100)]
		
		hsl2rgb : (h, s, l) ->
		  #jp hack
		  if s is 0
		  	s = 1
		  #http://www.codingforums.com/showthread.php?t=11156
		  m1 = undefined
		  m2 = undefined
		  hue = undefined
		  r = undefined
		  g = undefined
		  b = undefined
		  s /= 100
		  l /= 100
		  unless s is 0
		    if l <= 0.5
		      m2 = l * (s + 1)
		    else
		      m2 = l + s - l * s
		    m1 = l * 2 - m2
		    hue = h / 360
		    r = @HueToRgb(m1, m2, hue + 1 / 3)
		    g = @HueToRgb(m1, m2, hue)
		    b = @HueToRgb(m1, m2, hue - 1 / 3)
		  [r,g,b]

		HueToRgb : (m1, m2, hue) ->
		  #http://www.codingforums.com/showthread.php?t=11156
		  v = undefined
		  if hue < 0
		    hue += 1
		  else hue -= 1  if hue > 1
		  if 6 * hue < 1
		    v = m1 + (m2 - m1) * hue * 6
		  else if 2 * hue < 1
		    v = m2
		  else if 3 * hue < 2
		    v = m1 + (m2 - m1) * (2 / 3 - hue) * 6
		  else
		    v = m1
		  255 * v

	window.tools['color_picker'].init()

	$(window).mousedown (e)->
		if window.tools['color_picker'].window_open
			if not window.tools['color_picker'].picker.is(':hover')
				window.tools['color_picker'].confirm_color()
				#window.tools['color_picker'].picker.animate {backgroundColor: '#ff8400'}, 100, ()->
				#	window.tools['color_picker'].picker.animate {backgroundColor: '#ffffff'}, 100
				return false

