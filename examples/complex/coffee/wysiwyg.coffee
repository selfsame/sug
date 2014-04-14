
$(window.ifrm).ready ->

	window.tools['wysiwyg'] =
		manipulating: 0
		edit_mode: 1 # 0-single element, 1-managed document
		advanced_paste: false

		init: ()->
			if rangy
				if not rangy.initialized
					rangy.init()

		build_tool_options: ()->
			$('._tool_options').children().detach()

		focus_on: (element)->
			@init()
			#we don't want to edit created content, at least not with a special command
			@edit_mode = 1
			if element[0].tagName not in ['DIV']
				edited_parent = 0
				recurse = (el)->
					if el.data('user_edited')
						edited_parent = el
						recurse el.parent()
					else if el[0].tagName is 'BODY' or el[0].id is 'document'
							return
					else
						recurse el.parent()
				recurse element
				if edited_parent
					console.log 'found a suitable parent to edit'
					element = edited_parent

			if element[0].tagName not in ['DIV']
				@edit_mode = 0
			else
				@edit_mode = 1

			@build_tool_options()

			@before_editing_html = element.html()
			insert = $('<p>editing!</p>')

			if element.html() is ''
				element.html insert
			@manipulating = element
			editable = element[0]
			#window.enable_select()
			#window.tools.select.update_selection_set []


			element.data('edit_mode', true)
			element.data('user_edited', true)

			element.attr('contenteditable','true')
			element.addClass( '_fp_editable')
			element.css('cursor', 'text')
			element.focus()

			$(window).trigger 'contenteditstart'

			
	

			editable.onpaste = (e)->
				e.preventDefault()
				if window.tools.wysiwyg.manipulating is 0 or this isnt window.tools.wysiwyg.manipulating[0]
					return false
				console.log 'editable paste event'

				s = rangy.getSelection(window.ifrm)
				if s.getRangeAt and s.rangeCount
					range = s.getRangeAt 0

					if window.clipboardData and window.clipboardData.getData # IE
						pastedText = window.clipboardData.getData("Text")
					else if e.clipboardData and e.clipboardData.getData
						if e.clipboardData.getData("text/html")
							pastedText = e.clipboardData.getData("text/html")

							if window.tools.wysiwyg.edit_mode is 1
								if window.tools.wysiwyg.advanced_paste is false
									#parse the copied html to remove inline styles
									pastedText = window.tools.wysiwyg.clean_html_clipboard pastedText, 1
								else
									#advanced mode : is this really neccesarry>?
									pastedText = window.tools.wysiwyg.clean_html_clipboard pastedText, 0

								ancest = window.tools.wysiwyg.range_block_ancestor(range)
								edge = window.tools.wysiwyg.is_range_at_edge_of_block_ancestor range
								breakdown = window.tools.wysiwyg.get_advanced_paste_breakdown pastedText
								start_ancestor = window.tools.wysiwyg.nodes_block_ancestor range.startContainer
								end_ancestor = window.tools.wysiwyg.nodes_block_ancestor range.endContainer

								#console.log "breakdown: ", breakdown
								#if the paste is all inline/text nodes, don't split block elements
								split = 1
								if breakdown[0].length > 0 and breakdown[1].length is 0
									split = 0

								console.log "***", pastedText
								#convert insertion elements if needed
								if start_ancestor.tagName in ['LI']
									temp = $('<div></div>')
									for element in pastedText
										if split and element.nodeType is 3
											temp.append( $('<li></li>').append element )
										else if element.nodeType is 3
											temp.append( element )
										else
											temp.append element
											window.tools.wysiwyg.convert_element element, 'li'
									pastedText = temp.first().contents()


								results = window.tools.wysiwyg.range_insert range, pastedText, split, ancest
								range = results.range

								
							else
								#editing a text node element, give it plain text
								pastedText = e.clipboardData.getData("text/plain")
								hijacked = document.createTextNode pastedText
								range.insertNode hijacked

						else if e.clipboardData.getData("text/plain")
							pastedText = e.clipboardData.getData("text/plain")
							hijacked = document.createTextNode pastedText
							range.insertNode hijacked
						else
							pastedText = ''
					#window.tools.history.save_state('paste')
				return false

			element.on 'keyup mouseup keydown', (e)->
				if window.tools.wysiwyg.edit_mode is 0
					if not @.lastChild or @.lastChild.nodeName isnt 'BR'
						$(this).append $('<br>')
				else
					window.tools.wysiwyg.display_selected_element_type()
					#sel = rangy.getSelection()
					#if sel.rangeCount <= 0
					#	return
					#range = sel.getRangeAt(0)
					#ancest = range.commonAncestorContainer
					#if ancest.tagName in ["DIV"]
					#	if range.collapsed
					#		n = $('<p><br></p>')
					#		range.insertNode n[0]
					#		window.tools.wysiwyg.set_cursor n[0]

			element.on 'keyup', (e)->
				if e.keyCode is 8
					anc = $(window.tools.wysiwyg.range_block_ancestor())
					console.log anc, anc[0]
					for desc in anc.find('*')
						if desc.tagName is 'SPAN'
							if not $(desc).hasClass '_u'
								$(desc).contents().unwrap()

			element.on 'keydown', (e) ->
				#console.log e.keyCode
				sel = rangy.getSelection(window.ifrm)
				a = sel.anchorNode
				selection_node = sel.anchorNode.parentElement
				range = sel.getRangeAt(0)

				if e.keyCode is 13
					if window.tools.wysiwyg.edit_mode is 0
						results = window.tools.wysiwyg.range_insert range, $('<br>')
						range = results.range
						e.preventDefault()
					else

						start_ancestor = window.tools.wysiwyg.nodes_block_ancestor range.startContainer
						end_ancestor = window.tools.wysiwyg.nodes_block_ancestor range.endContainer


						insert_tag = start_ancestor.tagName.toLowerCase()
						if insert_tag not in ['p','li']
							insert_tag = 'p'

						ancest = window.tools.wysiwyg.range_block_ancestor(range)
						edge = window.tools.wysiwyg.is_range_at_edge_of_block_ancestor range

						if edge[0] is edge[1] is 0	
							insert = $('')
							cursor = 'beforeend'
						else if edge[0] is edge[1] is 1	
							insert = $('<'+insert_tag+'><br></'+insert_tag+'><'+insert_tag+'><br></'+insert_tag+'>')
							cursor = 'beforelastinsert'
						else
							insert = $('<'+insert_tag+'><br></'+insert_tag+'>')	
							cursor = 'beforeinsert'	

						#if we're adding a second empty line to a list, break the list.
						console.log "------>",  start_ancestor, end_ancestor, $(start_ancestor).html()
						if start_ancestor.tagName is "LI" and start_ancestor is end_ancestor and $(start_ancestor).html() in ['<br>', "", " "]
							insert = $('<p><br></p>')
							cursor = 'beforeinsert'	
							range.setStartBefore start_ancestor
							range.setEndAfter end_ancestor
							if ancest.tagName in ["UL", "OL"]
								ancest = ancest.parentElement

						results = window.tools.wysiwyg.range_insert range, insert, 1, ancest, cursor
						range = results.range
						e.preventDefault()
						#window.tools.history.save_state('return')
				else if range.collapsed
					window.tools.wysiwyg.remove_brs $(range.commonAncestorContainer)



			document.execCommand('styleWithCSS', false, null)
			text_buttons = $('#text_effects ._fp_center')
			# text_buttons.html ''
			# spacer = '<div style="display:inline-block;width:30px;">&nbsp;</div>'
			# text_buttons.append '<div class="text_button" id="text_bold" style="font-weight:bold;">B</div>'
			# text_buttons.append '<div class="text_button" id="text_italic" style="font-style:italic;">i</div>'
			# text_buttons.append spacer
			# text_buttons.append '<div class="text_button imgbut align" id="text_align_left" ><img src="./img/alignleft.png"></div>'
			# text_buttons.append '<div class="text_button imgbut align" id="text_align_right" ><img src="./img/alignright.png"></div>'
			# text_buttons.append '<div class="text_button imgbut align" id="text_align_center" ><img src="./img/aligncenter.png"></div>'
			# text_buttons.append '<div class="text_button imgbut align" id="text_align_justify" ><img src="./img/justify.png"></div>'
			# text_buttons.append spacer
			# text_buttons.append '<div class="text_button imgbut" id="text_indent" ><img src="./img/indent.png"></div>'
			# text_buttons.append '<div class="text_button imgbut" id="text_unindent" ><img src="./img/outdent.png"></div>'
			# text_buttons.append spacer

			# text_buttons.append '<select id="text_type" class="text_button text_select">
			# 							<option value="p">p</option>
			# 							<option value="h1">h1</option>
			# 							<option value="h2">h2</option>
			# 							<option value="h3">h3</option>
			# 							<option value="h4">h4</option>
			# 							<option value="h5">h5</option>
			# 							<option value="h6">h6</option>
			# 							<option value="ul">ul</option>
			# 							<option value="ol">ol</option>
			# 						</select>'
			# text_buttons.append '<div class="text_button" id="text_fore_color" style="width:35px;"><div class="text_edit_color"></div><div class="text_edit_background"></div></div>'
			# text_buttons.append '<div class="text_button" id="text_link">link</div>'
			# text_buttons.append '<div class="text_button" id="text_advanced">adv.paste</div>'
			# text_buttons.append '<div class="text_button" id="text_cancel">cancel</div>'
			# text_buttons.append '<div class="text_button" id="text_done">done</div>'

			# $('#text_effects').show()
			# o_o = $('#outer').offset()
			# r_o = $('#rulers').offset()
			# $('#text_effects').offset( {left:r_o.left, top: r_o.top } )
			# $('#text_effects').width $('#outer').width()+15

			# $('#text_effects').animate {height: 28 }, 300
			# $('#outer').animate {top:o_o.top+30 }, 300
			# $('#rulers').animate {top:r_o.top+30 }, 300


			save_selection = ()->
				s = rangy.getSelection(window.ifrm)
				r = s.getRangeAt(0)
				window.tools.wysiwyg.saved_range = r
				#console.log "%% saving selection: ", s.getRangeAt(0).inspect()

			load_selection = ()->
				if window.tools.wysiwyg.manipulating
					window.tools.wysiwyg.manipulating.focus()
					s = rangy.getSelection(window.ifrm)
					s.removeAllRanges()
					s.addRange window.tools.wysiwyg.saved_range
					#console.log "## loading selection: ", s.getRangeAt(0).inspect()

			if @advanced_paste
				$('#text_advanced').addClass '_text_button_active'

			text_buttons.each ->
				$(this).click (e)->
					load_selection()

			$('#text_advanced').on 'mousedown', (e)->	
				if window.tools.wysiwyg.advanced_paste
					$(this).removeClass '_text_button_active'
					window.tools.wysiwyg.advanced_paste = 0
				else
					$(this).addClass '_text_button_active'
					window.tools.wysiwyg.advanced_paste = 1
				save_selection()

			$('#text_bold').on 'mousedown', (e)->	
				document.execCommand('bold', false, null)
				save_selection()
				window.tools.wysiwyg.display_selected_element_type()

			$('#text_italic').on 'mousedown', (e)->
				document.execCommand('italic', false, null)
				save_selection()
				window.tools.wysiwyg.display_selected_element_type()

			$('#text_align_left').on 'mousedown', (e)->
				document.execCommand('justifyLeft', false, null)
				save_selection()
				window.tools.wysiwyg.display_selected_element_type()
			$('#text_align_right').on 'mousedown', (e)->
				document.execCommand('justifyRight', false, null)
				save_selection()
				window.tools.wysiwyg.display_selected_element_type()
			$('#text_align_center').on 'mousedown', (e)->
				document.execCommand('justifyCenter', false, null)
				save_selection()
				window.tools.wysiwyg.display_selected_element_type()
			$('#text_align_justify').on 'mousedown', (e)->
				document.execCommand('justifyFull', false, null)
				save_selection()
				window.tools.wysiwyg.display_selected_element_type()


			$('#text_indent').on 'mousedown', (e)->
				window.tools.wysiwyg.indent( 1 )
				#save_selection()
			$('#text_unindent').on 'mousedown', (e)->
				window.tools.wysiwyg.indent( -1 )

			$('#text_type').on 'mousedown', (e)->
				save_selection()
			$('#text_type').on 'change', (e)->
				console.log "converting elements"
				load_selection()
				v = $(this).val()
				if v in ['ol']
					window.tools.wysiwyg.convert_selection_to_list 'ol'
				else if v in ['ul']
					window.tools.wysiwyg.convert_selection_to_list 'ul'
				else
					window.tools.wysiwyg.convert_block_elements v



			$('#text_link').click (e)->
				s = rangy.getSelection(window.ifrm)
				r = s.getRangeAt(0)
				c = r.cloneContents()
				t = c.textContent
				window.tools.wysiwyg.saved_range = r
				#console.log 'text contents: ', t
				actual_dialogue = window.tools['menu'].dialogue()
				new_dialogue = actual_dialogue.children().first()
				new_dialogue.append $('<p>create a link:</p><br>
					<p>link url: </p><input id="_newlink_href" value="http://"><br>
					<p>link text:</p><input id="_newlink_text" value="'+t+'"><br>
					<p>target:</p><br>
					<button id="_done">done</button><button id="cancel">cancel</button>')
				button = new_dialogue.children('#_done')
				url = $('#_newlink_href')
				txt = $('#_newlink_text')
				url.focus()
				button.click (e)->
					url = $('#_newlink_href')
					txt = $('#_newlink_text')
					actual_dialogue.detach()
					#console.log 'creating link: ', url.val()
					window.tools.wysiwyg.manipulating.focus()
					s = rangy.getSelection(window.ifrm)
					s.removeAllRanges()
					s.addRange window.tools.wysiwyg.saved_range
					document.execCommand('createLink', false, url.val())
				cancel = new_dialogue.children('#cancel')
				cancel.click (e)->
					actual_dialogue.detach()
					window.tools.wysiwyg.manipulating.focus()
				$('body').append actual_dialogue

			$('#text_fore_color').mousedown (e)->
				s = rangy.getSelection(window.ifrm)
				r = s.getRangeAt(0)
				#window.tools.wysiwyg.saved_range = r
				#console.log 'mousedown saved range: ', r

			$('#text_fore_color').click (e)->
				s = rangy.getSelection(window.ifrm)
				r = s.getRangeAt(0)
				window.tools.wysiwyg.saved_range = r
				#console.log 'initial saved range: ', r
				content_color_callback = (color)->
					console.log 'COLOR CALLBACK', window.tools.wysiwyg.saved_range
					window.rrr = window.tools.wysiwyg.saved_range
					if window.tools.wysiwyg.saved_range
						s = rangy.getSelection(window.ifrm)
						s.addRange window.tools.wysiwyg.saved_range
					document.execCommand('foreColor', false, color)
				window.tools.color_picker.show_picker( $(this), 1, 'rgb(0,0,0)', content_color_callback )

			$('#text_cancel').click (e)->
				#revert changes and go to edit mode
				window.tools.wysiwyg.manipulating.html window.tools.wysiwyg.before_editing_html		
				window.tools.wysiwyg.remove_focus()
				window.tools.select.activate()

			$('#text_done').click (e)->
				#go to edit mode
				window.tools.wysiwyg.remove_focus()
				window.tools.select.activate()



		remove_focus: ()->
			if @manipulating
				element = @manipulating
				@manipulating = 0
				element.data('edit_mode', false)
				element.css('cursor', 'inherit')
				element.removeClass( '_fp_editable')
				element.removeAttr('contenteditable')
				element.unbind 'paste mouseup keyup keydown'

				#create room above the ruler for editing controls

				# o_o = $('#outer').offset()
				# r_o = $('#rulers').offset()
				# $('#outer').animate {top:o_o.top-30 }, 300
				# $('#text_effects').animate {height: 0 }, 300
				# $('#rulers').animate {top:r_o.top-30 }, 300, ->
				# 	$('#text_effects').hide()

				#window.util.enable_all()

				recurse = (element)->
					for child in element.children()
						$(child).data('content_created', true)

						if $(child)[0].tagName is 'A'
							$(child).unbind('click')
							$(child).click (e)->
								e.preventDefault()
						if $(child)[0].tagName is 'IMG'
							$(child).bind 'dragstart', (event)->
								event.preventDefault() 
						recurse $(child)
				recurse element


		range_delete_merge: (range, insert=0)->
			# if a range starts and ends in similar but different block nodes,
			# delete the range, insert something, and merge the nodes
			if not range.canSurroundContents()
				start = range.startContainer
				end = range.endContainer
				startblock = @nodes_block_ancestor( start )
				endblock = @nodes_block_ancestor( end )

				saved_start = range.startContainer
				saved_offset = range.startOffset
				range.deleteContents()
				if insert
					$(startblock).append insert
				if startblock.tagName is endblock.tagName	
					$(startblock).append $(endblock).contents()
					$(endblock).detach()

				range.setStart saved_start, saved_offset
				range.collapse()
				return range


		remove_empty_first_elements: (frag)->
			console.log "remove_empty_first_elements()"

			recurse_up = (el)->
				if $(el.parentElement).contents().length is 1
					console.log "recursing up"
					recurse_up el.parentElement
				else
					console.log "deleting element:::: ", el
					$(el).detach()

			recurse = (frag)->
				if frag.nodeType is 3
					n = null
				else if $(frag).contents().length is 0
					console.log "EMPTY: ", frag
					recurse_up frag
				else
					recurse $(frag).contents()[0]
			
			recurse frag
			console.log frag
			return frag

		range_insert: (range, jelements, split=0, common_block_ancestor=0, cursor="afterinsert", merge=0)->
			#inserts elements into range, can also split the surrounding elements before insertion
			#cursor placement is --  afterstart, beforeinsert, afterinsert, beforeend 

			console.log "range_insert(", range, jelements, split, common_block_ancestor, cursor, merge, ")"
			#check if we're pasting into a special element (a list to start)
			start = range.startContainer
			startblock = @nodes_block_ancestor( start )
			#console.log "STARTBLOCK: ", startblock
			

			console.log "jelements", jelements

			marker = $('<div></div>')

			if split and $(common_block_ancestor).length > 0
				console.log "splitting......."
				common_block_ancestor = $(common_block_ancestor)[0]
				r1 = rangy.createRange()
				r2 = rangy.createRange()
				r1.setStart common_block_ancestor, 0
				r1.setEnd range.startContainer, range.startOffset
				r2.setStart range.endContainer, range.endOffset
				r2.setEndAfter common_block_ancestor.lastChild
				range.extractContents()
				r2f = r2.extractContents()
				console.log "LAST HALF:", r2f
				r2f = @remove_empty_first_elements r2f
				d = $(common_block_ancestor).children().last()[0]
				split_before = d ? d : 0
				if split_before and $(split_before).text().length is 0
					$(split_before).detach()
					split_before = 0
				$(common_block_ancestor).append marker

			else 
				console.log "merging......"
				m = @range_delete_merge(range, marker[0])
				if m
					range = m
				else
					console.log "can't merge block nodes"
					range.extractContents()
					range.insertNode marker[0]
				
			console.log '-->', jelements
			
			element_before = marker.prev()

			for element in jelements
				console.log '--' ,element
				marker.before element

			if split and common_block_ancestor
				r2.setEndAfter marker[0]
				r2.collapse()
				r2.insertNode r2f
				d = marker.next()[0]
				split_after = d ? d : 0
				if split_after and $(split_after).text().length is 0
					$(split_after).detach()
					split_after = 0

			marker.detach()

			s = rangy.getSelection(window.ifrm)
			s.removeAllRanges()
			range = rangy.createRange()

			# afterstart, beforeinsert, afterinsert, beforeend 
			if cursor is 'beforelastinsert'
				range.setStartBefore jelements.last()[0]
				range.setEndBefore jelements.last()[0]
			else if cursor is 'endoflastinsert'
				if jelements.last().contents().length > 0
					range.setStartAfter jelements.last().contents().last()[0]
					range.setEndAfter jelements.last().contents().last()[0]
				else
					range.setStartBefore jelements.last()[0]
					range.setEndBefore jelements.last()[0]
			else if cursor in ['afterinsert', 'beforeend']
				if element
					range.setStartAfter element
					range.setEndAfter element
				else if split_after
					range.setStartBefore split_after
					range.setEndBefore split_after
				else
					console.log "!noplace to put cursor! --", cursor

			else if cursor in ['beforeinsert','afterstart']
				if jelements[0]
					range.setStartBefore jelements[0]
					range.setEndBefore jelements[0]
				else if split_before
					range.setStartAfter split_before
					range.setEndAfter split_before
				else
					console.log "!noplace to put cursor! --", cursor


			range.collapse()
			s.addRange(range)
			
			results =
				range: range
				split_before:split_before
				split_after:split_after

		range_block_ancestor: (r=0)->
			if not r
				s = rangy.getSelection(window.ifrm)
				r = s.getRangeAt(0)
			ancest = r.commonAncestorContainer
			console.log "range.commonAncestorContainer: ", $(ancest).html()
			ancest = @nodes_block_ancestor ancest
			if @nodes_block_ancestor( r.startContainer ) is @nodes_block_ancestor( r.endContainer ) is ancest
				console.log "start and end are both in same block"
				if ancest isnt @manipulating[0]
					ancest = ancest.parentElement
			console.log "range_block_ancestor: ", ancest
			return ancest

		nodes_block_ancestor: (node)->
			console.log "nodes_block_ancestor"
			#return the first block ancestor of a node
			safety = 0
			while true
				safety += 1
				if safety > 300
					console.log "WARNING: INFINITE LOOP ATTEMPT IN window.text.nodes_block_ancestor()"
					break
				if node.nodeType is 3
					node = node.parentElement
				else if node.tagName is 'LI'
					break
				else if node.tagName in window.block_level_elements
					break
				else if node is @manipulating[0]
					break
				else
					node = node.parentElement
			return node

		convert_selection_to_list: (ltype="ul")->
			console.log "convert_selection_to_list"
			s = rangy.getSelection(window.ifrm)
			r = s.getRangeAt(0)

			start = r.startContainer
			end = r.endContainer
			sa = @nodes_block_ancestor start
			ea = @nodes_block_ancestor end

			r.setStartBefore sa
			r.setEndAfter ea
			ancest = @range_block_ancestor r

			contents = r.extractContents()
			console.log contents
			list = $('<'+ltype+'></'+ltype+'>')

			for node in $(contents).children()
				console.log '-- ', node
				if node.tagName in ['UL','OL']
					list.append $(node).contents()
				else
					list.append node

			for child in list.children()
				if child.tagName isnt "LI"
					@convert_element $(child), 'li'

			#use our insert function to split and insert

			@range_insert(r, list, 1, ancest, "afterinsert")

			#r.insertNode list[0]

		convert_block_elements: (type="p")->
			#note: this can probably be merged with convert_selection_to_list
			console.log "convert_block_elements"
			s = rangy.getSelection(window.ifrm)
			r = s.getRangeAt(0)


			start = r.startContainer
			end = r.endContainer
			sa = @nodes_block_ancestor start
			ea = @nodes_block_ancestor end

			console.log sa, ea
			
			r.setStartBefore sa
			r.setEndAfter ea
			contents = r.extractContents()

			converted = $('<div></div>')
			for node in $(contents).children()
				converted.append node

			recurse = (node)->
				if node.tagName in ['BLOCKQUOTE', 'UL','OL']
					nothing = null
				else if node.tagName in window.block_level_elements
					window.tools.wysiwyg.convert_element $(node), type
				for child in $(node).children()
					recurse child

			for node in converted.children()
				recurse node

			marker = $('<div></div>')
			r.insertNode marker[0]
			firstnode = 0
			for node in converted.children()
				if not firstnode
					firstnode = node
				marker.before node
			marker.detach()
			r = @new_range()
			r.setStartBefore firstnode
			r.setEndAfter $(node).contents().last()[0]
			window.tools.wysiwyg.saved_range = r

		indent: (direction=1)->
			console.log "indent: ", direction
			s = rangy.getSelection(window.ifrm)
			r = s.getRangeAt(0)
			start = r.startContainer
			end = r.endContainer
			sa = @nodes_block_ancestor start
			ea = @nodes_block_ancestor end

			console.log sa, ea
			
			r.setStartBefore sa
			r.setEndAfter ea
			
			if direction is 1
				indent = $('<blockquote></blockquote>')

				#some logic for what exactly is being indented
				#if it's a subset of a list, we want to add another list instead of a blockquote

				if sa.tagName is "LI"
					if sa.parentElement is ea.parentElement
						indent = $('<'+sa.parentElement.tagName+'></'+sa.parentElement.tagName+'>')
				else if ea.tagName is "LI"
					r.setEndAfter ea.parentElement

				contents = r.extractContents()
				console.log contents
				
				for node in $(contents).children()
					indent.append node

				r.insertNode indent[0]

				r.setStartBefore indent[0]
				r.setEndAfter indent.contents().last()[0]
				window.tools.wysiwyg.saved_range = r
			else


			# OUTDENT:
			# if the selection is the entire contents of a blockquote (or list inside another list ) ->
			# replace with just the contents.
			# else if it's a subset, split the indention ancestor with the contents.
			#
			# Or..
			#
			# split the selection as normal.  Iterate through the cut part taking out every blockquote if it's not doubled up.
				contents = r.extractContents()
				common_block = @range_block_ancestor r
				console.log contents
				temp = $('<div></div>')

				for node in $(contents).contents()
					console.log '--', node
					temp.append node

				console.log "OUTDENT: "
				console.log temp

				recurse = (jset)->
					for node in jset.children()
						console.log '%--', node
						if node.tagName is 'BLOCKQUOTE'
							console.log "removing blockquote"
							$(node).replaceWith $(node).contents()
						else
							recurse $(node)

				recurse temp

				#@range_insert(r, $(contents).contents(), 1, common_block, "afterinsert")
				@range_insert(r, temp.contents(), 1, common_block, "afterinsert")



		new_range: ()->
			s = rangy.getSelection(window.ifrm)
			s.removeAllRanges()
			r = rangy.createRange()
			s.addRange r
			return r

		is_range_at_edge_of_block_ancestor: (r=0)->
			console.log "is_range_at_edge_of_block_ancestor"
			if not r
				s = rangy.getSelection(window.ifrm)
				r = s.getRangeAt(0)
			start = r.startContainer
			end = r.endContainer
			sa = @nodes_block_ancestor start
			ea = @nodes_block_ancestor end
			results = [0,0]

			current = start
			is_edge = 1
			safety = 0
			while true
				safety += 1
				if safety > 100
					break
				if current.nodeType is 3
					if r.startOffset is 0
						if current is $(current.parentElement).contents()[0]
							current = current.parentElement
						else
							is_edge = 0
							break
					else
						is_edge = 0
						break
				else if current is sa or current is @manipulating[0]
					break
				else if current is $(current.parentElement).contents()[0]
					current = current.parentElement
				else
					is_edge = 0

			current = end
			is_end_edge = 1
			safety = 0
			while true
				safety += 1
				if safety > 100
					break
				if current.nodeType is 3
					if r.endOffset is current.length
						if current is $(current.parentElement).contents().last()[0]
							current = current.parentElement
						else
							is_end_edge = 0
							break
					else
						is_end_edge = 0
						break
				else if current is ea or current is @manipulating[0]
					break
				else if current is $(current.parentElement).contents().last()[0]
					current = current.parentElement
				else
					is_end_edge = 0

			results = [is_edge, is_end_edge]
			console.log results
			return results

			r_e = r.endContainer
			console.log r, r.endContainer.nextSibling
			console.log 'nextSibling is ', r_e.nextSibling
			if r_e.nextSibling is null or r_e.nextSibling.nodeType is 3 and r_e.nextSibling.length is 0
				
				console.log 'nodeType is ', r_e.nodeType
				if r_e.nodeType is 3
					console.log r.endOffset, r_e.length
					if r.endOffset >= r_e.length
						return true

		clean_html_clipboard: (data, simplify=1, convert=0)->
			banned = "<body>|</body>|<html>|</html>|<!--StartFragment-->|<!--EndFragment-->|\n|\r|\r\n"
			re = new RegExp(banned, 'gi')
			data = data.replace(re, "")
			elements = $('<div></div>')
			elements.html data

			if simplify
				elements.find('*').each ()->
					$(this).removeAttr('style')
					$(this).removeAttr('id')
					$(this).removeAttr('class')
					if $(this)[0].tagName is 'SPAN'
						$(this).contents().unwrap()
				@clean_edit_area( elements )
			else
				elements.find('*').each ()->
					$(this).addClass '_u'

			if convert
				for child in elements.children()
					if child.tagName in ['P']
						@convert_element $(child), convert

			return $(elements).contents()

		get_advanced_paste_breakdown: (elements)->
			before = []
			middle = []
			after = []
			for child in elements
				if child.nodeType isnt 3 and child.tagName in window.block_level_elements 
					if after.length > 0
						middle = middle.concat after
						after = []
					middle.push child
				else
					if middle.length <= 0
						before.push child
					else
						after.push child
			return [before, middle, after]	

		clean_edit_area: (area)->
			@recurse_fix_divs area
			@area_has_dirty_nodes = 1
			#if we reconstruct some of the structure, set dirty nodes so we can rerun
			while @area_has_dirty_nodes is 1
				@area_has_dirty_nodes = 0
				@recurse_shift_left area, 0

		recurse_fix_divs: (element)->
			for child in $(element).children()
				if child.tagName is 'DIV'
					@recurse_fix_divs @convert_element(child, 'p')
				else
					@recurse_fix_divs child

		recurse_shift_left: (element, recurse_level)->
			recurse_level += 1
			for child in $(element).children()
				if recurse_level > 1
					if child.tagName in ['P','H1','H2','H3','H4','H5','H6', 'OL', 'UL','DIV'] and element.tagName not in ['LI']
						@shift_check child
						@area_has_dirty_nodes = 1
						break
					else
						@recurse_shift_left child, recurse_level
				else
					@recurse_shift_left child, recurse_level	

		shift_check: (target)->
			parent = $(target).parent()[0]
			node_count = 0
			node_pos = 0
			is_first = 0
			is_last = 0
			for node in $(parent).contents()
				node_count += 1
				if node is target
					node_pos = node_count
			if node_count is 1 #the target is the only child
				@area_has_dirty_nodes = 1	
				$(parent).replaceWith( $(target) )
			else if node_pos is 1
				@area_has_dirty_nodes = 1
				$(target).detach()
				$(parent).before( $(target) )
			else if node_pos is node_count
				@area_has_dirty_nodes = 1
				$(target).detach()
				$(parent).after( $(target) )
			else
				@area_has_dirty_nodes = 1
				$(target).detach()
				pc = $(parent).clone().empty()
				for i in [node_pos-1..node_count-2]
					pc.append $($(parent).contents()[node_pos-1]).remove()
				$(parent).after( $(target) )
				$(target).after( pc )




		convert_element: (element, newtag)->
			#note: this should do a better job of copying attributes over
			temp = $('<'+newtag+'></'+newtag+'>')
			if element.id isnt ''
				temp.attr('id', element.id)
			if element.className isnt ''
				temp.attr('class', element.className)
			temp.attr('style', $(element).attr('style'))
			temp.html $(element).html()

			for key of $(element).data()
				temp.data(key, $(element).data()[key] )

			$(element).replaceWith( temp )
			return temp

		remove_brs: (element)->
			if element[0].nodeType is 3
				element = element.parent()
			console.log "remove <br>", element[0]
			if element.contents().length > 1
				element.children('br').each ()->
					$(this).detach()

		display_selected_element_type: ()->
			#this should detect mixed elements and set the type select to a null value
			s = rangy.getSelection(window.ifrm)
			r = s.getRangeAt(0)
			startel = r.startContainer
			if startel.nodeType is 3
				startel = startel.parentElement
			if startel.tagName in ['LI']
				startel = $(startel).parent()[0]
			type = startel.tagName.toLowerCase()
			$('#text_type').val type


			align = $(startel).css('text-align')
			$('.text_button.align').removeClass '_text_button_active'
			$('#text_align_'+align).addClass '_text_button_active'
			if $(startel).css('font-style') is 'italic'
				$('#text_italic').addClass '_text_button_active'
			else
				$('#text_italic').removeClass '_text_button_active'

			if $(startel).css('font-weight') is 'bold'
				$('#text_bold').addClass '_text_button_active'
			else
				$('#text_bold').removeClass '_text_button_active'

		delete_range_backspace: ()->
			#this doesn't work, probably a ton of logic to do this
			s = rangy.getSelection(window.ifrm)
			r = s.getRangeAt(0)
			if r.collapsed
				if r.startOffset > 0
					r.setStart r.startContainer, r.startOffset - 1
					r.deleteContents()
				else if r.startContainer.previousSibling
					r.setStart r.startContainer.previousSibling, r.startContainer.previousSibling.length - 1
					r.deleteContents()
				else 
					r.setStart r.startContainer.parentElement.lastChild, r.startContainer.parentElement.lastChild.length - 1
			else
				r.deleteContents()

		


			


		




	



