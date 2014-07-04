Crafty.c "Tile_Select",
  x:0
  y:0
  w:window.game_width
  h:window.game_height
  z:1000
  sheet:'t1'
  init: ->
    @tiles = []
    window.indicator = Crafty.e("2D, Canvas, tile_select").attr(
          x: 30+window.selected_tile[0]*window.game_tile_size+window.selected_tile[0]
          y: 30+window.selected_tile[1]*window.game_tile_size+window.selected_tile[1]
          w: 32
          h: 32
          z: 99000)
    for i in [0..15]
      for j in [0..10]
        g = Crafty.e("2D, Canvas," + @sheet + ", Mouse").attr(
          x: i*(window.game_tile_size+1) + 30
          y: j*(window.game_tile_size+1) + 30
          w:window.game_tile_size
          h:window.game_tile_size
          z: 10001
        ).sprite(i,j,1,1)
        g.bind "MouseDown", (e) ->
          window.selected_tile =  @index
          window.indicator.x = @x
          window.indicator.y = @y

          if @index[1] > 6
            window.selected_solid = 0
            $('#l-solid').fadeTo(20, .3)
          else
            window.selected_solid = 1
            $('#l-solid').fadeTo(20, 1)
        g.index = [i,j]
        @tiles.push g

    $('#l-solid').click ->
      if window.selected_solid is 1
        window.selected_solid = 0
        $('#l-solid').fadeTo(20, .3)
      else
        window.selected_solid = 1
        $('#l-solid').fadeTo(20, 1)

    this

Crafty.c "Editor",
  w:window.game_width
  h:window.game_height
  init: ->
    window.editor = @
    @x = window.game_width
    @y = window.game_height

    window.selected_tile = [0,0]
    window.selected_solid = 1
    window.selected_z = 50

    @drawing = 0
    @choosing = 0
    @needs_save = 0
    @placed_at = [-500,-500]

    
    

    $('#overlay').html "
      <img id='l-edit' src='./img/edit.jpg' style='cursor:pointer;' /><img id='l-save' src='./img/save.jpg' style='cursor:pointer;' /> 
      <img id='l-solid' src='./img/solid.jpg' style='cursor:pointer;' />

      <img id='l-tile' src='./img/tile.jpg' style='cursor:pointer;' />
    "
    
    $('#l-edit').click ->
      window.editor.handle_edit_toggle()
    $('#l-tile').click ->
      window.editor.handle_tile_select()
    $('#l-save').click ->
      if window.paused is 1
        parameters = 
        "map": JSON.stringify(window.map)
        "x" : window.zone[0]
        "y" : window.zone[1]
        $.post(
          './save_level.php'
          parameters
          (data, statusText) ->

          )
        @needs_save = 0
        $('#l-save').fadeTo(100,.3)


    @bind "EnterFrame", (e) ->
      


    @bind "MouseDown", (e) ->
      if window.paused is 1
        @drawing = 1
        @place_tile e.clientX, e.clientY
      
    @bind "MouseUp", (e) ->
      @drawing = 0
      @placed_at = [-500,-500]

    @bind "MouseMove", (e) ->
      if window.paused is 1
        @place_tile e.clientX, e.clientY

    @bind "KeyDown", (e) ->
      if window.paused is 1
        if e.key is Crafty.keys["T"]

          @handle_tile_select()


      if e.key is Crafty.keys["K"]
        @handle_edit_toggle()


    this

  handle_edit_toggle: ->  
    if window.paused is 0
      window.paused = 1
      $('#l-edit').fadeTo(100, .3)
      $('#l-solid, #l-tile').fadeIn(100)
      if @needs_save is 0
        $('#l-save').fadeTo(100,.3)
      else
        $('#l-save').fadeTo(100,1)
    else
      if @choosing is 1
          @handle_tile_select()

      window.paused = 0
      $('#l-edit').fadeTo(100, 1)
      $('#l-save, #l-solid, #l-tile').fadeOut(100)
      


  handle_tile_select: ->  
    if window.paused is 1
      if @choosing is 0
        @choosing = 1
        $('#l-tile').fadeTo(100, .3)
        @sheet = Crafty.e("2D, Canvas, Color, Tile_Select").color("#969696").attr(
          x:0
          y:0
          w:window.game_width
          h:window.game_height
          z:1000
        )
      else
        $('#l-tile').fadeTo(100,1)
        for t in @sheet.tiles
          t.destroy()
        window.indicator.destroy()
        @sheet.destroy()
        @choosing = 0




  place_tile: (eX, eY) ->
    eX = eX - $('#cr-stage').offset().left
    eY = eY - $('#cr-stage').offset().top
    xx = eX - eX % window.game_tile_size
    yy = eY - eY % window.game_tile_size

    gx = xx/window.game_tile_size
    gy = yy/window.game_tile_size


    if @drawing is 1 and gy < 16 and gy > 0 and gx < 20
      if @placed_at[0] != xx or @placed_at[1] != yy

        @placed_at = [xx,yy]

        if window.emap_solid[gx][gy] isnt 0
          window.emap_solid[gx][gy].destroy()


        #data structure for our tiles
        window.map[gx][gy-1] = [window.selected_tile[0],window.selected_tile[1],window.selected_solid, window.selected_z]

        



        if window.selected_solid
          ccc = ',Solid'
        else
          ccc = ''



        window.emap_solid[gx][gy] = Crafty.e("2D, Canvas, t1, Collision"+ccc).attr(
          x: eX - eX % window.game_tile_size
          y: eY - eY % window.game_tile_size
          z: 10
        ).collision().sprite(window.selected_tile[0],window.selected_tile[1],1,1)
        if @needs_save is 0
          @needs_save = 1
          $('#l-save').fadeTo(100,1)
