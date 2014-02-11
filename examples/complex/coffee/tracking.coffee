if window.DEBUG
  console.log "running tracking.js"

# draw various effects for the app
# should be able to use either dom or canvas


$(window).ready ->

  class Selection_Box
    constructor: ->
      @w = 0
      @h = 0
      @left = 0
      @top = 0
      @element = 0
      @margins = [0,0,0,0]
      @paddings = [0,0,0,0]
      @borders = [0,0,0,0]
      @represents = 0
      if _t.tracking.draw_mode is 0
        @create_html_box()

    associate: (element)->
      @represents = $(element)
      @represents.data('selection_box', @)
      if _t.tracking.draw_mode is 0
        @element.data('selecting', @represents)
        $('#selections').append @element
      @update_dimensions()

    update_dimensions: (partial=0)->
      if @represents
        e_o = @represents.offset()
        if not partial or partial is 'x'
          @w = @represents.outerWidth()
          @left = e_o.left 
          if _t.tracking.draw_mode is 0
            @element.css
              'left': @left
              'width': @w
        if not partial or partial is 'y'
          @h = @represents.outerHeight()
          @top = e_o.top
          if _t.tracking.draw_mode is 0
            @element.css
              'top': @top
              'height': @h
        @margins[0] = parseInt(@represents.css('margin-top'))
        @margins[1] = parseInt(@represents.css('margin-right'))
        @margins[2] = parseInt(@represents.css('margin-bottom'))
        @margins[3] = parseInt(@represents.css('margin-left'))

        @paddings[0] = -parseInt(@represents.css('padding-top'))
        @paddings[1] = -parseInt(@represents.css('padding-right'))
        @paddings[2] = -parseInt(@represents.css('padding-bottom'))
        @paddings[3] = -parseInt(@represents.css('padding-left'))

        @borders[0] = parseInt(@represents.css('border-top-width'))
        @borders[1] = parseInt(@represents.css('border-right-width'))
        @borders[2] = parseInt(@represents.css('border-bottom-width'))
        @borders[3] = parseInt(@represents.css('border-left-width'))




    draw_to_canvas: ->
      if _t.tracking.draw_mode is 1
        opo = @get_op_offset()
        xx = opo[0]
        yy = opo[1]

        options =
          fillStyle: 'rgba(113, 183, 248, .5)'
          strokeStyle: 'transparent'
          lineWidth: 0
          use_scroll: true
          rulerw: true
        _t.tracking.draw_box @left+xx, @top+yy, @w, @h, options

    clear_canvas_area: ->
      if _t.tracking.draw_mode is 1
        opo = @get_op_offset()
        xx = opo[0]
        yy = opo[1]
        r0 = @get_rect(0)
        r = @get_rect(2)
        r1 = @get_rect(1)
        b = @borders

        options =
            fillStyle: 'orange'
            strokeStyle: 'rgb(113, 183, 248)'
            lineWidth: 0
            use_scroll: true
            rulerw: true

        if _t.tracking.display_margins
	        _t.tracking.clear_box(r[0]+xx, r[1]+yy, r[2], r[3]  )
	        _t.tracking.context.globalAlpha = .5
	        
	        _t.tracking.draw_box r[0]+xx, r[1]+yy, r[2], r[3], options
        _t.tracking.context.globalAlpha = 1.0
        _t.tracking.clear_box(r0[0]+xx, r0[1]+yy, r0[2], r0[3]  )
        if _t.tracking.display_padding
	        options.fillStyle = 'green'
	        _t.tracking.context.globalAlpha = .5
	        _t.tracking.draw_box r0[0]+xx+b[3], r0[1]+yy+b[0], r0[2]-b[1]-b[3], r0[3]-b[2]-b[0], options
	        _t.tracking.context.globalAlpha = 1.0
	        _t.tracking.clear_box(r1[0]+xx+b[3], r1[1]+yy+b[0], r1[2]-b[1]-b[3], r1[3]-b[2]-b[0]  )

    get_op_offset: ()->
      if @represents.data('_cache_sb_update')
        return [0,0]
      return _t.tracking.total_offset

    get_rect: (mode=0)-> #0 for inner, 1 for padding, 2 for margins
      #returns [x,y,w,h]
      x = @left
      y = @top
      w = @w
      h = @h 
      if mode is 3
        using = @borders
      if mode is 2
        using = @margins
      if mode is 1
        using = @paddings
      if mode isnt 0
        x -= using[3]
        y -= using[0]
        w += (using[3] + using[1])
        h += (using[0] + using[2])
      return [x,y,w,h]


    create_html_box: ()->
      sb = $('<div id="sb" class="selection_box"><div class="selection_margin"></div></div>')
      sb.mousemove (e) ->
        if window.selected_tool
          window.selected_tool.mousemove(e)
      sb.mousedown (e) ->
        if e.which is 3
          $(this).css('pointer-events', 'none')
        else if window.selected_tool
          window.selected_tool.mousedown(e)
      sb.mouseup (e) ->
        if window.selected_tool
          window.selected_tool.mouseup(e)
      @element = sb
      @element.data('selection_box', @)

    detach: ->
      @represents.data('selection_box', 0)
      @represents = 0
      if _t.tracking.draw_mode is 0
        @element.data('selecting', 0)
        @element.detach()

    display_margin: ->




  _t.tracking =
    draw_mode: 1 #0 is html/css, 1 is canvas
    sb_pool: [] #selection boxes that are available for display
    sb_active: [] #selection boxes that are being used
    total_offset: [0,0]
    box_select: 0
    show_select_mouseover: 0
    arrange_before: 0
    arrange_after: 0
    arrange_inside: 0
    arrange_left: 0
    arrange_right: 0
    arrange_insert: 0
    arrange_cursor: 0
    debug_move: {}
    display_margins: true
    display_padding: true

    
    init: ()->
      @canvas = $('#effects_overlay')
      @w = @canvas.attr('width')
      @h = @canvas.attr('height')
      @canvas.css('pointer-events','none')
      @context = @canvas[0].getContext("2d")
      for i in [0..10]
        @sb_pool.push new Selection_Box

      @context.webkitImageSmoothingEnabled = false
      @context.mozImageSmoothingEnabled = false
      @context.imageSmoothingEnabled = false

      @set_mode @draw_mode

      if not @context.setLineDash
        @context.setLineDash = ([])->

      img = new Image() # Create new img element
      img.onload = ->
        _t.tracking.rulerimg = this
      # execute drawImage statements here
      img.src = "./img/ruler2.png" # Set source path

      $(window).on 'selectionchange', (e)->
        _t.tracking.update_selections()
        _t.tracking.resync_selections()

      $(window).on 'contenteditstart', (e)->
        _t.tracking.show_select_mouseover = false
        _t.tracking.show_outliner_mouseover = false

      

    set_mode: (mode)->
      if mode is 1
        @draw_mode = 1
        $('#selections').css('display', 'none')
      else if mode is 0
        @draw_mode = 0
        $('#selections').css('display', 'block')

    update_selections: ->
      for sb in @sb_active
        @free_selection_box sb
      for element in _t.select.selected_elements
        @assign_selection_box element



    resync_selections: ->
      for sb in @sb_active
        sb.update_dimensions()

    assign_selection_box: (element)->
      if @sb_pool.length <= 0
        @sb_pool.push new Selection_Box
      new_sb = @sb_pool.pop(0)
      @sb_active.push new_sb
      new_sb.associate element

    free_selection_box: (sb)->
      @sb_active.remove sb
      @sb_pool.push sb
      sb.detach()

    free_selection_boxes: ()->
      for sb in $('#selections').children()
        @free_selection_box $(sb).data('selection_box')

    update_fluid_element_selections: ()->
      for element in _t.select.selected_elements
        @recalc_selection_box_size element
      _t.select.refresh_selection_handles()

    recalc_selection_box_sizes: ()->
      for element in _t.select.selected_elements
        @recalc_selection_box_size element

    recalc_selection_box_size: (element) ->
      sb = $(element).data('selection_box')
      if sb
        sb.update_dimensions()

    

    
      
    resize: ()->
      @w = (_m.outer_w+16)
      @h = (_m.outer_h+16)
      #@canvas.attr('width', _m.outer_w)
      #@canvas.attr('height', _m.outer_h)
      #$('#canvas_clip').css
      #  width: _m.outer_w 
      #  height: _m.outer_h
      #@draw_box()

    draw_box: (x=0,y=0,w=100,h=100, options={fillStyle:"transparent", strokeStyle:"rgb(113, 183, 248)", lineWidth:0, dash:0, scale:true, rulerw:16})->
      if options.scale isnt false
        x = parseInt(x*window.doc_zoom)
        y = parseInt(y*window.doc_zoom)
        w = parseInt(w*window.doc_zoom)
        h = parseInt(h*window.doc_zoom)
      rulerw = 16
      if options.rulerw
        x += -_m.scroll_x + _m.ruler_w
        y += -_m.scroll_y + _m.ruler_w
      ###
      x -= x % .5
      y -= y % .5
      w -= w % .5
      h -= h % .5
      ###
      x += .5 
      y += .5 
      if not options.lineWidth > 0
        w += .5
        h += .5
      
      @context.fillStyle = options.fillStyle
      @context.strokeStyle = options.strokeStyle
      @context.lineWidth = options.lineWidth
      if options.dash
        @context.setLineDash(options.dash)
      else
        @context.setLineDash([])

      @context.beginPath()
      @context.moveTo(x,y)
      @context.lineTo(x+w, y)
      @context.lineTo(x+w, y+h)
      @context.lineTo(x, y+h)
      @context.lineTo(x, y)
      @context.closePath()

      @context.fill()
      if options.lineWidth > 0
        @context.stroke()

    draw_line: (x=0,y=0,x2=100,y2=100, options={fillStyle:"transparent", strokeStyle:"rgb(113, 183, 248)", lineWidth:1, dash:0, scale:true, rulerw:16, use_scroll:true})->
      if options.scale isnt false
        x = parseInt(x * window.doc_zoom)
        y = parseInt(y * window.doc_zoom)
        x2 = parseInt(x2 * window.doc_zoom)
        y2 = parseInt(y2 * window.doc_zoom)

      x += .5 
      y += .5
      x2 += .5 
      y2 += .5
      if options.use_scroll
        x += - _m.scroll_x 
        y += - _m.scroll_y 
        x2 += - _m.scroll_x 
        y2 += - _m.scroll_y
      if options.rulerw
        x += _m.ruler_w
        y += _m.ruler_w
        x2 += _m.ruler_w
        y2 += _m.ruler_w


      

      @context.fillStyle = options.fillStyle
      @context.strokeStyle = options.strokeStyle
      @context.lineWidth = options.lineWidth
      if options.dash
        @context.setLineDash(options.dash)
      else
        @context.setLineDash([])

      @context.beginPath()
      @context.moveTo(x,y)
      @context.lineTo(x2, y2)
      @context.closePath()

      @context.fill()
      @context.stroke()

    draw_text: (string, x,y, options={fillStyle:0,font:0,scale:true, rulerw:16, use_scroll: true})->
      if options.scale isnt false
        x *= window.doc_zoom
        y *= window.doc_zoom

      x += .5 
      y -= .5
      if options.use_scroll
        x += - _m.scroll_x 
        y += - _m.scroll_y 
      if options.rulerw
        x += _m.ruler_w 
        y += _m.ruler_w

      if options.fillStyle
        @context.fillStyle = options.fillStyle
      if options.font
        @context.font = options.font
      @context.fillText(string, x, y)

    clear_box: (x=0,y=0,w=100,h=100, options={scale:true})->
      if options.scale
        x *= window.doc_zoom
        y *= window.doc_zoom
        w *= window.doc_zoom
        h *= window.doc_zoom

      x = parseInt(x - _m.scroll_x + 16)
      y = parseInt(y - _m.scroll_y + 16)
      x += .5 
      y += .5 
      #w += .5
      #h += .5
      @context.clearRect(x, y, w, h)

    clear_canvas: ()->
      @context.clearRect(0, 0, @w, @h)

    get_elements_inside_box: ()->
      set = []
      set_changed = 0
      recurse = (element)->
        for child in $(element).children()
          b_o = $(child).offset()
          bw = $(child).outerWidth()
          bh = $(child).outerHeight()
          box =
            left: b_o.left
            top: b_o.top
            right: b_o.left + bw
            bottom: b_o.top + bh
          if window.util.rect_contains( box, _t.tracking.box_select) or _t.select.box_select_previous and child in _t.select.box_select_previous
            if not $(child).data('selection_box')
              set_changed = 1
              _t.select.selected_elements.push child
              _t.tracking.assign_selection_box child
          else
            if $(child).data('selection_box')
              if child in _t.select.selected_elements
                set_changed = 1
                _t.select.selected_elements.remove child
                _t.tracking.free_selection_box $(child).data('selection_box')

          if window.alt_key or $(child).data('outliner_expansion') is 1
            recurse child

      recurse $$('body')
      if set_changed
        $(window).trigger('selectionchange')
      _t.select.refresh_selection_handles()

    draw_things: ()->
      
        
      #window.update_document_dimensions()
      z = window.doc_zoom
      options =
        strokeStyle: 'orange'
        fillStyle: 'transparent'
        lineWidth: 2
        rulerw: true
        use_scroll: true
        scale: true
      if @box_select
        bs = @box_select
        @draw_box bs.left, bs.top,  bs.right - bs.left, bs.bottom - bs.top, options
        @get_elements_inside_box()    

      if _t.select
        if _t.select.selected_elements.length > 0
          if true #not _t.s.widget_active
            sxv = 0
            syv = 0
            if _m.scroll_x_visible
              sxv = 16
            if _m.scroll_y_visible
              syv = 16
            @context.fillStyle = "rgba(0,0,0,.0)"
            @context.rect(16,16,_m.outer_w-syv,_m.outer_h-sxv)
            @context.fill()

            for sb in @sb_active
              sb.clear_canvas_area()
            for sb in @sb_active
              sb.draw_to_canvas()

      if @show_select_mouseover or @show_outliner_mouseover
        if @show_select_mouseover
          t = @show_select_mouseover
        if @show_outliner_mouseover
          t = @show_outliner_mouseover
        options.strokeStyle = 'rgb(113, 183, 248)'
        options.lineWidth = 3
        options.fillStyle = "transparent"
        options.dash = [8,8]

        to = $(t).offset()
        tw = $(t).outerWidth()
        th = $(t).outerHeight()
        @draw_box( to.left, to.top, tw, th, options )
        options.dash = []


      @draw_arrange()

      #clear selection draws from the ruler area
      @context.clearRect(0,0,16,_m.outer_h)
      @context.clearRect(0,0,_m.outer_w,16)

      if _t.select
        if _t.select.selected_elements.length > 0
          
          sa = _t.select.selection_box

          options.strokeStyle = 'rgb(113, 183, 248)'
          options.lineWidth = 1
          @draw_line sa[0][0], sa[0][1],  sa[0][0], -(16/z), options
          @draw_line sa[1][0], sa[0][1],  sa[1][0], -(16/z), options
          @draw_line sa[0][0], sa[0][1],  -(16/z), sa[0][1], options
          @draw_line sa[0][0], sa[1][1],  -(16/z), sa[1][1], options

          options.strokeStyle = 'rgb(113, 183, 248)'
          options.fillStyle = 'rgb(113, 183, 248)'
          #options.font = "10px Arial"
          @draw_text(sa[0][0]+'px', sa[0][0]+(4/z), ((12+_m.scroll_y)/z), options)
          @draw_text(sa[1][0]+'px', sa[1][0]+(4/z), ((12+_m.scroll_y)/z), options)
          @draw_text(sa[0][1]+'px',  ((4+_m.scroll_x)/z), sa[0][1]+((12)/z), options)
          @draw_text(sa[1][1]+'px',  ((4+_m.scroll_x)/z), sa[1][1]+((12)/z), options)


        


          
      #for guide in _t.rulers.guides
      #  guide.draw()
      if _t.select
        if _t.select.selected_elements.length > 0
          sbox = _t.select.selection_box

          options.fillStyle = 'white'
          options.strokeStyle = 'black'
          options.lineWidth = 1
          w = 10/window.doc_zoom
          oo = 5/window.doc_zoom
          @draw_box( sbox[0][0]-oo, sbox[0][1]-oo, w, w, options )
          @draw_box( sbox[1][0]-oo, sbox[0][1]-oo, w, w, options )
          @draw_box( sbox[0][0]-oo, sbox[1][1]-oo, w, w, options )
          @draw_box( sbox[1][0]-oo, sbox[1][1]-oo, w, w, options )
          #sides
          hw = (sbox[1][0] - sbox[0][0])/2
          hh = (sbox[1][1] - sbox[0][1])/2
          @draw_box( sbox[0][0]+hw-oo, sbox[0][1]-oo, w, w, options )
          @draw_box( sbox[0][0]+hw-oo, sbox[1][1]-oo, w, w, options )
          @draw_box( sbox[0][0]-oo, sbox[0][1]+hh-oo, w, w, options )
          @draw_box( sbox[1][0]-oo, sbox[0][1]+hh-oo, w, w, options )

      #if not _t.rulers.hidden
      @draw_rulers()
        
      options = null

      #@draw_debug_move()


      
    draw_debug_move: ()->
      data = @debug_move
      el = data.element
      ###
      debug.element = element
      debug[rule+'_value'] = value
      debug.parent_offset = parent_offset
      debug[rule+'_base'] = base
      debug.cached_position = cached_position
      debug['final_'+rule+'_value'] = value
      ###
      if data.left_value
        @draw_info_point data.left_value, el.offset().top+(el.height()/2), 'preserve_left', 'red'
      if data.top_value
        @draw_info_point el.offset().left+(el.width()/2), data.top_value, 'preserve_top', 'red'
      if data.parent_offset
        @draw_info_point data.parent_offset.left, data.parent_offset.top, 'parent_offset', 'green'
      
      if data.final_left_value
        @draw_info_point data.final_left_value, el.offset().top+(el.height()/2), 'final_left', 'orange'
      if data.final_top_value
        @draw_info_point el.offset().left+(el.width()/2), data.final_top_value, 'final_top', 'orange'

    draw_info_point: (x, y, text, color)->
      options =
        strokeStyle: color
        fillStyle: 'transparent'
        lineWidth: 1
        rulerw: true
        use_scroll: true
        scale: true
      @draw_box( x, y, 3, 3, options )

      options.fillStyle = 'white'
      options.lineWidth = 0
      @draw_box( x+4, y-8, text.length*5, 12, options )

      

      
      

      options.fillStyle = color
      @draw_text(text, x+6, y+2, options)


        

    draw_rulers: ()->
      #draw rulers
      for run in [0,1]
        options =
          fillStyle: 'white'
          strokeStyle: 'gray'
          lineWidth: 1
          rulerw: 16

        z = window.doc_zoom
        if run is 0
          view = _m.outer_w
          scroll = _m.scroll_x
          doc_d = _m.doc_w
        else
          view = _m.outer_h
          scroll = _m.scroll_y
          doc_d = _m.doc_h

        #scaled pixels on ruler
        lcount = parseInt(((view)/(z)))
        lstart = parseInt( scroll/z )
        lcent = 0
        #if _t.rulers.centered and run is 0
        #  lcent = parseInt(doc_d/2)
        
        if run is 0
          lcent #+= _t.rulers.get_lock_offset(0)
        else
          lcent #+= _t.rulers.get_lock_offset(1)

        density = view / lcount
        t_thresh = parseInt(  (1/density)  *100)
        b = 0
        for t in [10000, 5000, 2500 ,1000,200, 100, 50, 20, 10, 5]
          mod = t_thresh % t
          if not b and mod < t_thresh
            t_thresh -= mod
            b = 1
          if b
            break
        if mod < 5
          mod = 5
        if mod is 20
          mod = 10

        options =
          fillStyle: 'black'
          strokeStyle: 'gray'
          lineWidth: 1
          rulerw: 0
          scale:false

        do_line = (x1, y1, x2, y2, options)->
          if run is 0
            _t.tracking.draw_line(x1, y1-1, x2, y2-1, options)
          else
            _t.tracking.draw_line(y1, x1, y2, x2, options)
        do_text = (st, x, y, options)->
          if run is 0
            _t.tracking.draw_text(st, x, y, options)
          else
            for h in [0..st.length-1]
              _t.tracking.draw_text(st[h], 0, x+6+(h*8), options)

        for i in [0..(lcount)] 
          iz = parseInt( i*z )
          oi = i+lstart - lcent
          marker1 = 8
          marker2 = 10
          marker3 = 8
          if oi % t_thresh is 0
            @context.fillStyle = 'gray'
            do_text((oi)+'', iz+18, 8, options)
            marker1 = 0
            marker2 = 0
            marker3 = 0

          if oi % 100 is 0 and density > .05
            do_line( iz+16, marker3, iz+16, 16, options )

          if oi % 10 is 0 and density > .5
            @context.fillStyle = 'gray'
            do_line( iz+16, marker1, iz+16, 16, options )
          if oi % 5 is 0 and density > 2.0
            @context.fillStyle = 'silver'
            do_line( iz+16, marker2, iz+16, 16, options )
          if density > 4
            @context.fillStyle = 'silver'
            do_line( iz+16, 10, iz+16, 16, options )
      options = null
      marker1 = null
      marker2 = null
      marker3 = null
      iz = null
      oi = null
      do_line = null
      do_text = null
      lcount = null

    draw_arrange: ()->
      #if window.current_tool_name isnt 'newdiv' and not _t.outliner.move_triggered and not _t.clipboard.inserting
      return

      z = window.doc_zoom
      options =
          fillStyle: 'transparent'
          strokeStyle: 'red'
          lineWidth: 3
          rulerw: 16
          scale:true
          use_scroll: true
      before = @arrange_before
      after = @arrange_after
      left = @arrange_left
      right = @arrange_right
      inside = @arrange_inside
      if before
        options.lineWidth = 3
        gradient = @make_arrange_gradient before, 'top', 'rgb(93,168,226)'
        options.strokeStyle = gradient
        @draw_box( before.left, before.top, before.width, before.height, options  )
        options.strokeStyle = 'white'
        options.lineWidth = 1
        @draw_box( before.left, before.top, before.width, before.height, options  )

      if after
        options.lineWidth = 3
        gradient = @make_arrange_gradient after, 'bottom', 'rgb(93,168,226)'
        options.strokeStyle = gradient
        @draw_box( after.left, after.top, after.width, after.height, options )
        options.strokeStyle = 'white'
        options.lineWidth = 1
        @draw_box( after.left, after.top, after.width, after.height, options )

      if left
        options.lineWidth = 3
        gradient = @make_arrange_gradient left, 'left', 'rgb(93,168,226)'
        options.strokeStyle = gradient
        @draw_box( left.left, left.top, left.width, left.height, options )
        options.strokeStyle = 'white'
        options.lineWidth = 1
        @draw_box( left.left, left.top, left.width, left.height, options )
      
      if right
        options.lineWidth = 3
        gradient = @make_arrange_gradient right, 'right', 'rgb(93,168,226)'
        options.strokeStyle = gradient
        @draw_box( right.left, right.top, right.width, right.height, options )
        options.strokeStyle = 'white'
        options.lineWidth = 1
        @draw_box( right.left, right.top, right.width, right.height, options )


      if inside
        options.lineWidth = 3
        options.strokeStyle = 'pink'
        @draw_box( inside.left, inside.top, inside.width, inside.height, options )
        options.strokeStyle = 'white'
        options.lineWidth = 1
        @draw_box( inside.left, inside.top, inside.width, inside.height, options )

      if @arrange_cursor
        options.strokeStyle = '#71b7f8'
        options.lineWidth = 1
        @draw_line( @arrange_cursor[0], @arrange_cursor[1]-(10/z), @arrange_cursor[0], @arrange_cursor[1]+(10/z), options )
        @draw_line( @arrange_cursor[0]-(10/z), @arrange_cursor[1], @arrange_cursor[0]+(10/z), @arrange_cursor[1], options )

      for row in _t.arrange.float_rows
        if row.float is 'left'
          options.strokeStyle = 'red'
        else
          options.strokeStyle = 'green'
        options.lineWidth = 1
        options.fillStyle = 'transparent'
        @draw_box( row.x, row.y, row.w, row.h, options )
      options = null
      
    make_arrange_gradient: (obox, pos, color)->
      z = window.doc_zoom
      trans = 'rgba('+color.split('(')[1].split(')')[0]+', 0.0)'
      #console.log trans
      box = []
      s = 0
      for prop in [obox.left, obox.top, obox.right, obox.bottom]
        box.push prop*z

      gradient = 'green'
      if pos is 'right'
        gradient = @context.createLinearGradient( box[0]-_m.scroll_x, box[1]-_m.scroll_y, box[2]-_m.scroll_x, box[1]-_m.scroll_y )
        gradient.addColorStop(0, trans)
        gradient.addColorStop(1, color)
      if pos is 'left'
        gradient = @context.createLinearGradient( box[0]-_m.scroll_x, box[1]-_m.scroll_y, box[2]-_m.scroll_x, box[1]-_m.scroll_y )
        gradient.addColorStop(1, trans)
        gradient.addColorStop(0, color)
      if pos is 'top'
        gradient = @context.createLinearGradient( box[0]-_m.scroll_x, box[1]-_m.scroll_y, box[0]-_m.scroll_x, box[3]-_m.scroll_y )
        gradient.addColorStop(0, trans)
        gradient.addColorStop(1, color)
      if pos is 'bottom'
        gradient = @context.createLinearGradient( box[0]-_m.scroll_x, box[1]-_m.scroll_y, box[0]-_m.scroll_x, box[3]-_m.scroll_y )
        gradient.addColorStop(1, trans)
        gradient.addColorStop(0, color)
      return gradient

    animate: ->
      window.requestAnimFrame _t.tracking.animate
      #_t.tracking.clear_canvas()
      _t.tracking.draw_things()

      



  _t.tracking.init()



  



