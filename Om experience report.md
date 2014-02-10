Om experience report

Nothing but rave reviews.

I've been recreating the basic UI from an app I've been developing with js/jQuery to get a feel for what I can do with it.  As I've made these layouts before, I'm confident that Om can speed up my workflow by an order of magnitude.  Those little tabsets on the tool windows were honestly not my best idea, but they took a whole day to put in.  With Om I adding a vector of view keywords to my toolbox data point, a conditional dom/div with the views mapped to the tabs in the generic toolbox, and a transact onClick to change the view. 


The first thing I stumbled on was component communication, basically setting up something like the sortable example where something needs to drag as well as inform it's immediate neighbors to adjust as well.  Core.async is very good for this, but a lot of boilerplate to set up and became confusing to me when I needed more than one route. 

I wrote a sugar layer to automate it out of site, https://github.com/selfsame/sug  It wraps om calls in order to see if there are event handlers declared, which will create (or link to) a keyword/chan conduit.  Components clone the chans and pub/sub to children which keeps the flow constrained to the heirarchy. Components can fire event maps to one or more keyword chan names, and can specify firing up or down the system.

  It's very amatuer and I only spent a few hours reading about core.async and macros before bashing it out, but it does make event firing very simple and tells me that we'll have a lot of fun inventing syntax to work with Om.


I get the best results from Om when I have a well formed tree of data to drill down into, but that's not always the thing I want to model with it. My Current stumbing block is how to integrate the branching structure of the UI I want to make with the branched structures of the data it works with.  UI tends to have a nice hierarchy, perfect for Om to display.  But if you can only drill down the tree, you have to make a choice on whether the data is located in specific areas of your interface or if the UI is an option you send to appropriate places in your data. Some ideas I'm throwing around:

Favor the data.  You can build complex UI heirarchies without drilling down the cursor, in effect you're making your own fugly cursor by chaining state or opts, which lets you build your UI with the whole cursor intact, which can then be split up into views once you are ready for some data.  I think this approach involves more attention to the render update flow, as UI infastructure is usually where you want to be speedy and I believe any change to the app state would trigger the render.  

Favor the UI if you gain more from giving it first class structure. The data can be brought in and manually monitored, or passed down the UI with clever state updates.  It even seems you can pass cursors down from the top in om, but I'm not sure if that's indended.

Structure them in tandem.  The filesystem UI layout entry contains your filesystem data.  Your menu bar contains the definitive boolean values for the entire app. Don't do this.

Sugar over the system for update dependencies.  I played around with this and it seemed promising.  Somewhere downstream a component would explicitly ask the sugar layer for a cursor path, which it will use to build a child component.  The sugar layer records the path, and does diff checks on it's data value, which triggers a broadcast with the path namespace.  The new component listens for those updates, and can reinstate the traditional cascade of data because it left the parents path.  


