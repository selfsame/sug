if window.DEBUG
  console.log "running arrange.js"

###

for every element in the target create a layout proxy.
float proxies are grouped into rows

then, using the position type of the element to be inserted, have the proxies sort themselves into sets of before, after, left, and right of the cursor.
The proxies should jostle for position using their dimensions, so we can easily get the last element before the cursor, or the first after.

proxies will also need to collaborate to find out if there is no gap between them and they need to sacrifice some space to allow the cursor to select between

proxies should keep rects of their dimensions (with and without margins), and their positioning css rules



###

class Frog extends Object
	constructor: ()->
		@skin = "slimy"

window.Frog = Frog


class ElementProxy extends Object
	constructor: (element)->
		@element = element
		@jelement = $(element)
		@position = @jelement.css('position')
		@float = @jelement.css('float')
		@clear = @jelement.css('clear')
		@calc_bounds()
		@init()
	init: ->
	toLiteral: ->
		literal =
			element: @element
			position: @position
			float: @float
			clear: @clear
		return literal		
	toString: ->
		return @name+':'+@element
	get_last: ()->
		return @
	calc_bounds: ->
		el = @jelement
		@css_left = parseInt(el.css('left'))
		@css_top = parseInt(el.css('top'))
		if not @css_left
			@css_left = 0
		if not @css_top
			@css_top = 0

		@margins = [parseInt(el.css('margin-left')),parseInt(el.css('margin-top')),parseInt(el.css('margin-right')),parseInt(el.css('margin-bottom'))]
		for marg, i in @margins
			if not marg
				@margins[i] = 0

		rect = @element.getBoundingClientRect()
		@rect              = { left:rect.left, top:rect.top, right:rect.right, bottom:rect.bottom, width: rect.right-rect.left, height:rect.bottom-rect.top}
		@flow_rect         = { left:@rect.left-@css_left, top:@rect.top-@css_top, right:@rect.right-@css_left, bottom:@rect.bottom-@css_top }
		@flow_rect_margins = { left:@rect.left-@css_left-@margins[0], top:@rect.top-@css_top-@margins[1], right:@rect.right-@css_left+@margins[2], bottom:@rect.bottom-@css_top+@margins[3] }
		#can be switched, for relation checks and draws
		@relation_rect = @rect

	relate_to_point: (x,y)->
		#return description of the relationship: {'inside':false, 'left': 23, 'right':false, 'before':13,'after':false} would describe the point being 23 pixels to the right
		# of the proxy and 13 pixels below it.
		rect = @relation_rect
		desc =
			'x':x
			'y':y
			'inside': false
			'left'  : false
			'right' : false
			'before': false
			'after' : false
		in_x = 0
		in_y = 0
		if x < rect.left
			desc.left = rect.left - x
		else if x > rect.right
			desc.right = x - rect.right
		else
			in_x = 1

		if y < rect.top
			desc.after = rect.top - y
		else if y > rect.bottom
			desc.before = y - rect.bottom
		else
			in_y = 1
		if in_x and in_y
			desc = @describe_inside desc
		@last_relation = desc
		return desc
	describe_inside: (desc)->
		desc.inside = true
		return desc
	sort_insert: (list, prop)->
		#smallest to largest
		if list.length is 0
			list.push @
		else
			for proxy, i in list
				if @last_relation[prop]? and proxy.last_relation[prop]?
					#console.log 'comparing ', prop, '(',@last_relation[prop], proxy.last_relation[prop], ')'
					if Math.abs(@last_relation[prop]) < Math.abs(proxy.last_relation[prop])

						list = list.slice(0,i).concat(@).concat( list.slice(i, list.length) )
						return list
			list.push @
		return list






class FloatRow extends ElementProxy
	# create a node tree of floats.
	# given an element, we should be able to recursivly find the row it belongs to, otherwise create a row and insert into node tree
	init: ->
		@float_row = true
		@float = @jelement.css('float')
		@proxies = [new ElementProxy(@element)]
		@row_rect = JSON.parse( JSON.stringify(@rect) )
		@relation_rect = @row_rect
	get_last: ()->
		return @proxies[@proxies.length-1]
	update_row_rect: (proxy)->
		rect2 = proxy.rect
		if rect2.left < @row_rect.left
			@row_rect.left = rect2.left
		if rect2.top < @row_rect.top
			@row_rect.top = rect2.top
		if rect2.right > @row_rect.right
			@row_rect.right = rect2.right
		if rect2.bottom > @row_rect.bottom
			@row_rect.bottom = rect2.bottom
		@row_rect.width = @row_rect.right - @row_rect.left
		@row_rect.height = @row_rect.bottom - @row_rect.top
		@relation_rect = @row_rect

	consider: (proxy)->
		#console.log 'considering'
		if proxy not in @proxies
			#console.log 'not in proxies'
			if proxy.float is @float
				#console.log 'floats match'
				if proxy.jelement.parent()[0] is @jelement.parent()[0]
					#console.log @float, @flow_rect_margins.top
					if @flow_rect_margins.top is proxy.flow_rect_margins.top
						@update_row_rect proxy
						@proxies.push proxy
						return true
		return false

	describe_inside: (desc)->
		desc.inside = true
		return desc



window.tools['arrange'] =
	proxy_class: ElementProxy
	positioning_cursor: $('<div id="fp_arrange_cursor"><div></div></div>')
	float: 0
	float_rows: []
	init: ->

	display: (info)-> #mode is 0:before, 1:after, 2:inside
		#console.log "arrange.display( ",info," )"
		element = false
		_t.tracking.arrange_before = 0
		_t.tracking.arrange_after = 0
		_t.tracking.arrange_left = 0
		_t.tracking.arrange_right = 0
		_t.tracking.arrange_inside = 0
		_t.tracking.arrange_insert = 0
		_t.tracking.arrange_cursor = 0


		@positioning_cursor.detach()
		@positioning_cursor.css
			'position': @position
			'top': 0
			'left': 0
			'width': '100%'
			'float': @float


		if info.inside
			_t.tracking.arrange_inside = info.inside.rect
			
		else


			before = 0
			after = 0
			left = 0
			right = 0

			if info.before.length > 0
				before = info.before[0]
				_t.tracking.arrange_before = before.relation_rect
			if info.after.length > 0
				after = info.after[0]
				_t.tracking.arrange_after = after.relation_rect
			if info.left.length > 0
				left = info.left[0]
				_t.tracking.arrange_left = left.relation_rect
			if info.right.length > 0
				right = info.right[0]
				_t.tracking.arrange_right = right.relation_rect


		if info.left.length > 0 or info.right.length > 0
			@positioning_cursor.css('width', 0)

		if left or right
			@positioning_cursor.css('float', 'left')

		if info.inside
			element = info.inside.element
			mode = 2
			$(element).append @positioning_cursor
		
		else if before
			element = before.element
			mode = 1
			$(element).after @positioning_cursor
		else if after
			element = after.element
			mode = 0
			$(element).before @positioning_cursor
		else if right
			element = right.element
			mode = 1
			$(element).after @positioning_cursor
		else if left
			element = left.element
			mode = 0
			$(element).before @positioning_cursor
		
		if element?
			p_o = @positioning_cursor.offset()
			@positioning_cursor.detach()

			_t.tracking.arrange_cursor = [p_o.left, p_o.top]
			#console.log "insertion element:  ", info.inside, element, mode

			return [element, mode]
		


 
	find_insertion: (e)->
		#sort elements in target into before, after, left, right

		if not $$('html').is( $(e.target).parents()) and not $$('html').is( $(e.target))
			return

		target = e.target

		target = $(target)

		info = 
			inside: 0
			before: []
			after:  []
			left: []
			right: []

		proxies = []
		float_rows = []

		
		target_proxy = new ElementProxy(target[0])
		if target.children().length is 0
			info.inside = target_proxy
			return info

		for el in target.children()
			proxy = new ElementProxy(el)
			if $(el).css('float') in ['left','right'] and $(el).css('position') in ['relative','static','']
				
				placed = 0
				for row in float_rows
					if row.consider proxy
						placed = 1
				if not placed
					proxy = new FloatRow(el)
					float_rows.push proxy
					proxies.push proxy
			else
				proxies.push proxy
			
		#console.log 'PROXIES: ', proxies
		#console.log e.clientX, e.clientY
		sort_relations = (pset, inf, fl=false)->
			for proxy in pset

				relation = proxy.relate_to_point(e.clientX, e.clientY)
				if relation.inside
					if proxy instanceof FloatRow 
						#console.log "PROXY FLOAT", proxy
						#if inside a floatRow proxy, recurse to find the specifics
						inf = 
							inside: 0
							before: []
							after:  []
							left: []
							right: []
						return sort_relations(proxy.proxies, inf, true)
					else
						inf.inside = proxy
						return inf


				if relation.before or relation.left
					#if it's a float row, we need the last proxy in the row
					proxy.element = proxy.get_last().element

				if relation.before and not fl
					inf.before = proxy.sort_insert(inf.before, 'before')
				else if relation.after and not fl
					inf.after = proxy.sort_insert(inf.after, 'after')
				else if relation.left
					inf.left = proxy.sort_insert(inf.left, 'left')
				else if relation.right
					inf.right = proxy.sort_insert(inf.right, 'right')
			return inf
		info = sort_relations proxies, info
		#_t.debug.watch('balr: ', ''+info.before.length+ info.after.length+ info.left.length+ info.right.length)
		if info.inside
			inside = info.inside.toLiteral()
		else
			inside = false
		_t.arrange.insertion_cache = 
			after: info.after.map (a)->
				a.toLiteral()
			before: info.before.map (a)->
				a.toLiteral()
			left: info.left.map (a)->
				a.toLiteral()
			right: info.right.map (a)->
				a.toLiteral()
			inside: inside
		return info


	show_create_target: (e, position='relative', float='none', clear='none')->
		@position = position
		@float = float
		@clear = clear
		t_m = @find_insertion e
		if not t_m
			return 0
		position = @display t_m
		if position
			return position

	hide_visuals: ->

		_t.tracking.arrange_before = 0
		_t.tracking.arrange_after = 0
		_t.tracking.arrange_inside = 0
		_t.tracking.arrange_left = 0
		_t.tracking.arrange_right = 0
		_t.tracking.arrange_insert = 0
		_t.tracking.arrange_cursor = 0